
package com.sonyericsson.extras.liveware.extension.keybd;

import java.io.DataOutputStream;
import java.io.IOException;

import java.util.ArrayList;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor;


import com.sonyericsson.extras.liveware.extension.util.Dbg;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorManager;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import android.os.AsyncTask;

import android.os.Handler;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * The sample control for SmartWatch handles the control on the accessory.
 * This class exists in one instance for every supported host application that
 * we have registered to
 */
class SampleControlSmartWatch extends ControlExtension {

	private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;


	private Handler mHandler;

	private boolean mIsShowingAnimation = false;

	private boolean mIsVisible = false;

	//private Animation mAnimation = null;

	private final int width;

	private final int height;

	//private Bitmap mCurrentImage = null;

	private static final int NUMBER_TILE_TEXT_SIZE = 14;

	private TextPaint mNumberTextPaint;

	private ArrayList<TilePosition> mTilePositions;

	private ArrayList<GameTile> mGameTiles;

	public Process process;
	public DataOutputStream out;

	long pressTime;

	private AccessorySensor mSensor = null;

	private boolean accelOn = false;

	float[] estimates = new float[3];

	float[] cumuEsti = new float[3];

	float[] vestimates = new float[3];

	float[] alpha = new float[3];

	float[] beta = new float[3];

	ArrayList<float[]> estiTrend;

	float[] crossed = new float[3];

	//-1 - Below the cross line. 1 - above
	int[] direction = new int[3];

	long swipeTime;

	long startTime;

	boolean first = false;

	long prevTime;

	int released = 0;

	//for y and z values and the time
	ArrayList<float[]>  prevValues = new ArrayList<float[]>();

	ArrayList<Long> prevTimes = new ArrayList<Long>();

	//-1 left, 1 - right
	int jerkStart = 0;

	//for y and z values and the time
	ArrayList<float[]>  fbprevValues = new ArrayList<float[]>();

	ArrayList<Long> fbprevTimes = new ArrayList<Long>();

	//-1 left, 1 - right
	int fbjerkStart = 0;

	boolean back = false;

	String str;

	int globalI = 0;
	int globalX;
	int inputLimitCount = 0;

	boolean newnavigate = false;

	int canstop = 1;

	boolean tRunning = false;

	Thread t = new Thread();

	public TCPClient mTcpClient;

	public int flag = 0;

	connectTask tsk;

	ArrayList<Double> distances = new ArrayList<Double>();

	boolean mRun = true;
	
	String dbl;
	
	double[][] pts = new double[3][4];
	double[] finger = new double[3];
	double[] fobject = new double[3];
	double[] aa = new double[4];
	double dist=0, sqr=0, di=0;
	
	//char[] ls = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	char[] ls = {'0','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
	
	/**
	 * Create sample control.
	 *
	 * @param hostAppPackageName Package name of host application.
	 * @param context The context.
	 * @param handler The handler to use
	 */
	SampleControlSmartWatch(final String hostAppPackageName, final Context context,
			Handler handler) {
		super(context, hostAppPackageName);
		if (handler == null) {
			throw new IllegalArgumentException("handler == null");
		}

		mHandler = handler;
		mNumberTextPaint = new TextPaint();
		mNumberTextPaint.setTextSize(NUMBER_TILE_TEXT_SIZE);
		mNumberTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mNumberTextPaint.setColor(Color.WHITE);
		mNumberTextPaint.setAntiAlias(true);


		width = getSupportedControlWidth(context);
		height = getSupportedControlHeight(context);

		AccessorySensorManager manager = new AccessorySensorManager(context, hostAppPackageName);
		mSensor = manager.getSensor(Sensor.SENSOR_TYPE_ACCELEROMETER);

		try
		{
			process = Runtime.getRuntime().exec("su");
			out = new DataOutputStream(process.getOutputStream());

		}
		catch(Exception e)
		{
			Dbg.d("XYZY: process ");
			Log.d("Singleton", "XYZY: gfdhf");
		}

	}

	/**
	 * Get supported control width.
	 *
	 * @param context The context.
	 * @return the width.
	 */
	public static int getSupportedControlWidth(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_width);
	}

	/**
	 * Get supported control height.
	 *
	 * @param context The context.
	 * @return the height.
	 */
	public static int getSupportedControlHeight(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_height);
	}

	@Override
	public void onDestroy() {

		Log.d(SampleExtensionService.LOG_TAG, "SampleControlSmartWatch onDestroy");
		//stopAnimation();
		if  (mHandler!=null)
		{
			mHandler = null;
		}

		tsk.cancel(true);
		mRun = false;

		// Stop sensor
		if (mSensor != null) {
			mSensor.unregisterListener();
			mSensor = null;
		}

	};

	@Override
	public void onStart() {
		// Nothing to do. Animation is handled in onResume.
		/*try
		{
			process = Runtime.getRuntime().exec("su");
			out = new DataOutputStream(process.getOutputStream());

		}
		catch(Exception e)
		{
			Dbg.d("XYZY: process ");
			Log.d("Singleton", "XYZY: gfdhf");
		}*/

		// connect to the server
		//new connectTask().execute("");
		/*tsk = new connectTask();
		tsk.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);*/
	}

	/*private void getFingerStream() {
		// TODO Auto-generated method stub

	}*/

	public class connectTask extends AsyncTask<String, Double,TCPClient> {

		@Override
		protected TCPClient doInBackground(String... message) {

			//we create a TCPClient object and
			mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
				@Override
				//here the messageReceived method is implemented
				public void messageReceived(Double[] allData) {

					/*String[] strpts = new String[19];

					int cnt = 0;
					for(int i=0;i<pts.length;i++)
					{
						for(int j=0;j<pts[0].length;j++)
						{
							strpts[cnt] = Double.toString(pts[i][j]);
							cnt++;
						}
					}
					for(int i=0;i<finger.length;i++)
					{
						strpts[cnt] = Double.toString(finger[i]);
						cnt++;
					}*/
					//this method calls the onProgressUpdate
					publishProgress(allData);
					//Dbg.d("KEYBD: process ");
					//Log.d("KEYBD", "1MSG: "+message);
				}
			});
			mTcpClient.run();
			
			//return null;
			
			try{

				byte[] sendbytes = new byte[8];

				sendbytes[0] = (byte)3;
				for (int i = 1; i < 8; ++i)
				{
					sendbytes[i] = (byte)0;
				}
				mTcpClient.sendBytes(sendbytes);

				int type, packet, count;
				byte[] buffer=new byte[4];
				byte[] buffer1= new byte[8];
				Double[] allData = new Double[20];
				//String dat = "";
				long currTime;
				int dcnt;
				double dd;
				//DATA
				while(mRun)				
				{				
					//Log.d("Keybd", "Run");
					//Debug.dumpHprofData(absPath);
					//sendBytes(sendbytes);

					//buffer = new byte[4];
					//in = new DataInputStream(socket.getInputStream());
					mTcpClient.in.read(buffer);
					currTime = System.currentTimeMillis();

					packet = mTcpClient.fromByteArray(buffer);
					//Log.d("Keybd", "P: "+packet);
					mTcpClient.in.read(buffer);
					type = mTcpClient.fromByteArray(buffer);
					//in.read(aa);
					//System.out.println("Packet: "+packet);
					//System.out.println("Type: "+type);

					if (type != 1) {
						//System.out.println("Type: "+type);
						//System.out.println("Type Error ");
					}

					if (packet != 2){

						//System.out.println("Packet Error ");
					}

					mTcpClient.in.read(buffer);
					count = mTcpClient.fromByteArray(buffer);
					//System.out.println("Count: "+count);
					 
					//double[][] pts = new double[4][4];
					//double[] finger = new double[3];
					//Double[] allData = new Double[20];
					dcnt = 0;

					for (int i = 0; i < count; ++i)
					{
						//if(i>=9)
						//{

					
						//int cIndex = 0;
						//buffer1 = new byte[8];            
						mTcpClient.in.read(buffer1);
						dd = mTcpClient.fromByteArraytoDouble(buffer1);
						//String channel = new String(b, "UTF-8");

						if (i>=9)
						{
							//System.out.print(" "+dd);	
							//dat = dat+dd+" ";
							//watch marker values
							if(i>=9 && i<21)
							{
								//int q = (i-9)/4;
								//pts[q][(i-9)%4] = dd;
								allData[dcnt++] = Double.valueOf(dd);
								if (i==9)
								{
									//dbl = Double.toString(dd);
									//Log.d("Keybd", dbl);
								}
							}//finger marker values
							else if(i>=29 && i<32)
							{
								//finger[(i-33)] = dd; 
								allData[dcnt++] = Double.valueOf(dd);
							}//finger object values
							else if(i>=42 && i<45)
							{
								allData[dcnt++] = Double.valueOf(dd);
							}

						}

						//}				
					}
										
					//allData[dcnt] = (double)currTime;
					allData[dcnt]= Double.valueOf((double)currTime);
					//Log.d("TCP", System.currentTimeMillis()+" "+dat);

					mTcpClient.mMessageListener.messageReceived(allData);
				}

				sendbytes = new byte[8];

				sendbytes[0] = (byte)4;
				for (int i = 1; i < 8; ++i)
				{
					sendbytes[i] = (byte)0;
				}
				mTcpClient.sendBytes(sendbytes);

				try
				{
					mTcpClient.in.close();
					mTcpClient.socket.close();
				}
				catch (IOException e)
				{
					Log.e("Keybd", "T: Error", e);
				}


				//Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
				//Dbg.d("KEYBD1: "+serverMessage);

			} catch (Exception e) {

				Log.e("Keybd", "S: Error", e);
				//System.out.println("Error "+ e.getStackTrace());

			} finally {


			}	
			return null;
		}

		/** Gets the parameters of the plan ax+by+cz+d=0 in an array.
		 * @param pts
		 * @return
		 */
		double[] planeEquation(double[][] pts)
		{
			//Making all the points at the same initial z-level
			//Assuming the situation where mid marker on watch is marker 1, left marker is marker 2 and right marker is marker 3. 
			
			//pts[1][2] = pts[1][2]-12.36326;
			//pts[2][2] = pts[2][2]-20.61238;
			
			pts[1][2] = pts[1][2]-11.7;
			pts[2][2] = pts[2][2]-1.4;
			
			aa[0] = (pts[1][1] - pts[0][1])*(pts[2][2] - pts[0][2]) - (pts[2][1] - pts[0][1])*(pts[1][2] - pts[0][2]);
			aa[1] = (pts[1][2] - pts[0][2])*(pts[2][0] - pts[0][0]) - (pts[2][2] - pts[0][2])*(pts[1][0] - pts[0][0]);
			aa[2] = (pts[1][0] - pts[0][0])*(pts[2][1] - pts[0][1]) - (pts[2][0] - pts[0][0])*(pts[1][1] - pts[0][1]);
			aa[3] = -(aa[0]*pts[0][0]+aa[1]*pts[1][1]+aa[2]*pts[2][2]);
			
			//Calculating the watch area location
			//pts[1][1]
			//pts[2][0]
			
			return aa;
		}

		/**
		 * Gets the distance of a point from a plane
		 * @param plane
		 * @param point
		 * @return
		 */
		double distanceFromPlane(double[] plane, double[] point)
		{
			
			sqr = plane[0]*plane[0]+plane[1]*plane[1]+plane[2]*plane[2]; 
			//dist = Math.abs((plane[0]*point[0]+plane[1]*point[1]+plane[2]*point[2]+plane[0])/(Math.sqrt(sqr)));
			dist = (plane[0]*point[0]+plane[1]*point[1]+plane[2]*point[2]+plane[3])/(Math.sqrt(sqr));
			return dist;
		}


		@Override
		protected void onCancelled() {
			if (mTcpClient != null)
			{
				try {
					mTcpClient.stopClient();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					Log.e("Keybd", "ERROR: "+e.getMessage());
				}
				//mTcpClient = null;
			}
		}

		@Override
		protected void onProgressUpdate(Double... values) {
			super.onProgressUpdate(values);


			//in the arrayList we add the messaged received from server
			//arrayList.add(values[0]);
			// notify the adapter that the data set has changed. This means that new message received
			// from server was added to the list
			// mAdapter.notifyDataSetChanged();
			
			//if (flag>=1)
			//{
				flag++;
				if(released ==1)
				{
					released = 0;
					flag=0;
				}
				
				int cnt = 0;
				//String dat = "";
				for(int i=0;i<pts.length;i++)
				{
					for(int j=0;j<pts[0].length;j++)
					{
						pts[i][j] = values[cnt];
						cnt++;
						//dat = dat+" "+values[cnt];
					}
				}
				for(int i=0;i<finger.length;i++)
				{
					finger[i] = values[cnt];
					//dat = dat+" "+values[cnt];
					cnt++;
				}
				for(int i=0;i<fobject.length;i++)
				{
					fobject[i] = values[cnt];
					//dat = dat+" "+values[cnt];
					cnt++;
				}
				
				di = distanceFromPlane(planeEquation(pts), finger);
				//distances.add(d);
				//Log.d("Keybd", "PTS: "+dat);

				Log.d("Keybd", "DIS: "+values[cnt]+" "+di+" "+distanceFromPlane(planeEquation(pts), fobject));
				Log.d("TCP", pts[2][0]+" "+pts[1][1]+" "+finger[0]+" "+finger[1]);
				
			//}
			//Dbg.d("KEYBD: process ");
			//Log.d("KEYBD", "MSG: "+values[0]);
		}
	}

	@Override
	public void onStop() {
		// Nothing to do. Animation is handled in onPause.

	}

	@Override
	public void onResume() {
		mIsVisible = true;

		Log.d(SampleExtensionService.LOG_TAG, "Starting animation");

		//Part of vicon
		tsk = new connectTask();
		tsk.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		mRun = true;

		startNewGame();

		//0.15322891 is the smallest unit of measurement
		estimates[0] = (float)0.15322891;
		estimates[1] = (float)0.15322891;
		estimates[2] = (float)9.65322891;

		vestimates[0] = 0;
		vestimates[1] = 0;
		vestimates[2] = 0;        

		newnavigate = false;
		canstop = 1;
		tRunning=false;
		str = "";
		globalI = 0;

		alpha[0] = (float)0.05;
		alpha[1] = (float)0.05;
		alpha[2] = (float)0.05;

		beta[0] = (float)0.001;
		beta[1] = (float)0.001;
		beta[2] = (float)0.001;

		//direction[0] = 0;
		//direction[1] = 0;
		//direction[2] = 0;
		globalX = 750;
		inputLimitCount=0;

		first = false;

		estiTrend = new ArrayList<float[]>();


		prevValues = new ArrayList<float[]>();		
		jerkStart = 0;
		prevTimes = new ArrayList<Long>();


		fbprevValues = new ArrayList<float[]>();		
		fbjerkStart = 0;
		fbprevTimes = new ArrayList<Long>();


		prevTime = -1;
		setScreenState(Control.Intents.SCREEN_STATE_ON);
		// Animation not showing. Show animation.
		mIsShowingAnimation = true;
		// mAnimation = new Animation();
		//  mAnimation.run();


	}


	@Override
	public void onPause() {
		Log.d(SampleExtensionService.LOG_TAG, "Stopping animation");
		mIsVisible = false;
		tsk.cancel(true);
		mRun = false;
		if (mIsShowingAnimation) {
			stopAnimation();

		}
		mHandler = null;

		//	String s="sendevent /dev/input/event2 3 57 4294967295\n"+"sendevent /dev/input/event2 0 0 0\n";

		/*try{
			out.writeBytes(s);
			out.flush();

		}
		catch(IOException e){
			Log.d("Accel", "EXC: Pau");
		}*/
		setScreenState(Control.Intents.SCREEN_STATE_AUTO);
		// Stop sensor
		if (mSensor != null) {
			mSensor.unregisterListener();
		}
	}


	private void startNewGame() {
		//drawLoadingScreen();

		// Create game positions
		initTilePositions(new TilePosition(1, new Rect(0, 32, 31, 63)), new TilePosition(2,
				new Rect(32, 32, 63, 63)), new TilePosition(3, new Rect(64, 32, 95, 63)),  new TilePosition(4, new Rect(96, 32, 127, 63)),				
				new TilePosition(5, new Rect(0, 64, 31, 95)), new TilePosition(6,
						new Rect(32, 64, 63, 95)), new TilePosition(7, new Rect(64, 64, 95, 95)),  new TilePosition(8, new Rect(96, 64, 127, 95)),
						new TilePosition(9, new Rect(0, 96, 31, 127)), new TilePosition(10,
								new Rect(32, 96, 63, 127)), new TilePosition(11, new Rect(64, 96, 95, 127)),  new TilePosition(12, new Rect(96, 96, 127, 127)),
								new TilePosition(13, new Rect(96, 0, 127, 31)));


		getNumberImage();


		// Create game tiles
		initTiles();

		// Draw initial game Bitmap
		getCurrentImage(true);

		// Init game state
		// mNumberOfMoves = 0;
		// mGameState = GameState.PLAYING;
		// Dbg.d("game started with empty tile index " + mEmptyTileIndex);
	}


	/**
	 * Init the 9 tile position objects.
	 *
	 * @param tilePositions The tile positions
	 */
	private void initTilePositions(TilePosition... tilePositions) {
		mTilePositions = new ArrayList<TilePosition>(13);
		for (TilePosition tilePosition : tilePositions) {
			mTilePositions.add(tilePosition);
		}
	}

	/**
	 * Get bitmap with number tiles drawn.
	 *
	 * @return The bitmap
	 */
	private Bitmap getNumberImage() {
		Bitmap bitmap = Bitmap.createBitmap(width, height, BITMAP_CONFIG);
		// Set the density to default to avoid scaling.
		bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);

		Paint tilePaint = new Paint();
		tilePaint.setColor(Color.GRAY);
		for (TilePosition tilePosition : mTilePositions) {
			//if (tilePosition.position != 25) {
			canvas.drawRect(tilePosition.frame, tilePaint);
			canvas.drawText(tilePosition.position,
					tilePosition.frame.left + 5, tilePosition.frame.top + 18, mNumberTextPaint);
			//}
		}

		return bitmap;
	}

	/**
	 * Init the 9 tiles with index and bitmap, based on game type.
	 */
	private void initTiles() {
		mGameTiles = new ArrayList<GameTile>(13);
		// Force size to 9
		for (int i = 0; i < 13; i++) {
			mGameTiles.add(new GameTile());
		}

		int i = 1;
		for (TilePosition tp : mTilePositions) {
			GameTile gt = new GameTile();
			if (i != 14) {
				gt.correctPosition = i;

				char[] ls = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
				String r = "";
				while(true) {
					r =String.valueOf(ls[(i * 2)-1])+String.valueOf(ls[i * 2]) + r;
					if(i < 26) {
						break;
					}
					i /= 26;
				}

				gt.text = r;
				Log.d("Keybd", "Keybd: "+r);
			}

			gt.tilePosition = tp;
			//  if (mGameType == GameType.NUMBERS) {
			setNumberTile(gt);
			//  } else {
			//      setImageTile(mCurrentImage, gt);
			//  }
			mGameTiles.set(i-1, gt);
			i++;
		}
	}

	/**
	 * Create number based bitmap for tile
	 *
	 * @param gt The tile
	 */
	private void setNumberTile(GameTile gt) {
		gt.bitmap = Bitmap.createBitmap(32, 32, BITMAP_CONFIG);
		// Set the density to default to avoid scaling.
		gt.bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

		Canvas canvas = new Canvas(gt.bitmap);
		//if (gt.text != null) {
		canvas.drawColor(Color.GRAY);
		canvas.drawText(gt.text, 5, 18, mNumberTextPaint);
		// } else {
		// Empty tile
		//    canvas.drawColor(Color.WHITE);
		//    mEmptyTileIndex = gt.tilePosition.position;
		//  }
	}

	/**
	 * Draw all tiles into bitmap and show it.
	 *
	 * @param show True if bitmap shown be shown, false otherwise
	 * @return The complete bitmap of the current game
	 */
	private Bitmap getCurrentImage(boolean show) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, BITMAP_CONFIG);
		bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		Canvas canvas = new Canvas(bitmap);
		// Set background
		canvas.drawColor(Color.BLACK);
		// Draw tiles
		for (GameTile gt : mGameTiles) {
			canvas.drawBitmap(gt.bitmap, gt.tilePosition.frame.left, gt.tilePosition.frame.top,
					null);
		}
		if (show) {
			showBitmap(bitmap);
		}

		return bitmap;
	}

	/**
	 * Stop showing animation on control.
	 */
	public void stopAnimation() {
		// Stop animation on accessory
		// if (mAnimation != null) {
		//  mAnimation.stop();
		//  mHandler.removeCallbacks(mAnimation);
		//   mAnimation = null;
		//  }
		mIsShowingAnimation = false;

		// If the control is visible then stop it
		if (mIsVisible) {
			stopRequest();
		}
	}

	@Override
	public void onTouch(final ControlTouchEvent event) {
		//Log.d(SampleExtensionService.LOG_TAG, "onTouch() " + event.getAction());

		if (event.getAction() == Control.Intents.KEY_ACTION_PRESS) {
			pressTime = event.getTimeStamp();
			Log.d("Keybd", "Keybd Press: "+pressTime+" "+event.getX()+" ,"+event.getY());
			flag=1;

		}
		else if (event.getAction() == Control.Intents.TOUCH_ACTION_RELEASE) {
			int tileReleaseIndex = getTileIndex(event);
			long rlsTime = event.getTimeStamp();
			Log.d("Keybd", "Touch Time: "+rlsTime);
			if(tileReleaseIndex == -1)
				return;
			
			String r = "";
			while(true) {
				r = ls[(tileReleaseIndex*2)-1] + r;
				if(tileReleaseIndex < 14) {
					break;
				}
				tileReleaseIndex /= 26;
			} 
			Log.d("Keybd", "Keybd: "+event.getX()+" ,"+event.getY()+"\n");

			int val=1;
			if ((event.getTimeStamp()-pressTime)>200)
				val = 0;

			int keyindex = ((tileReleaseIndex*2)-val)+28; 
			try
			{
				out.writeBytes("input keyevent "+ keyindex+"\n");
				//out.writeBytes("input tap 500 600\n");


				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}
			if (flag>1)
			{
				flag = 0;
			}
			else
			{
				released = 1;
			}
		}
	}

	@Override
	public void onSwipe(int direction) {

		switch (direction) {
		case Control.Intents.SWIPE_DIRECTION_LEFT:
			try
			{
				out.writeBytes("input keyevent 67\n");
				//out.writeBytes("input tap 500 600\n");


				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}
		case Control.Intents.SWIPE_DIRECTION_RIGHT:

			try
			{
				out.writeBytes("input keyevent 62\n");
				//out.writeBytes("input tap 500 600\n");


				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}
			break;
		case Control.Intents.SWIPE_DIRECTION_UP:
			if (accelOn == false)
			{
				accelOn = true;
				Log.d("Accel", "ACEL: on");
				//startVibrator(40, 0, 1);

			}

			else
			{
				accelOn = false;
				Log.d("Accel", "ACEL: off");
				//startVibrator(30, 30, 2);
			}
			/*try
			{
				out.writeBytes("input keyevent 62\n");
				//out.writeBytes("input tap 500 600\n");


				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}*/
			break;
		default:
			break;
		}

	}

	/**
	 * Start repeating vibrator
	 *
	 * @param onDuration On duration in milliseconds.
	 * @param offDuration Off duration in milliseconds.
	 * @param repeats The number of repeats of the on/off pattern. Use
	 *            {@link Control.Intents#REPEAT_UNTIL_STOP_INTENT} to repeat
	 *            until explicitly stopped.
	 */
	public void startVibrator(int onDuration, int offDuration, int repeats) {
		if (Dbg.DEBUG) {
			Dbg.v("startVibrator: onDuration: " + onDuration + ", offDuration: " + offDuration
					+ ", repeats: " + repeats);
		}
		Intent intent = new Intent(Control.Intents.CONTROL_VIBRATE_INTENT);
		intent.putExtra(Control.Intents.EXTRA_ON_DURATION, onDuration);
		intent.putExtra(Control.Intents.EXTRA_OFF_DURATION, offDuration);
		intent.putExtra(Control.Intents.EXTRA_REPEATS, repeats);
		sendToHostApp(intent);
	}

	/**
	 * Get tile index for the coordinates in the event.
	 *
	 * @param event The touch event
	 * @return The tile index
	 */
	private int getTileIndex(ControlTouchEvent event) {
		int x = event.getX();
		int y = event.getY();

		//Finger correction
		//if (x > 5)
		//x = x-5;
		//if (y>2)
		//y = y-2;
		int rowIndex = x / 32;
		int columnIndex = y / 32;
		if (columnIndex==0)
		{
			if (rowIndex==3)
				return 13;
			else
				return -1;
		}
		else
			return 1+rowIndex + (columnIndex-1) * 4;
	}
	/* *//**
	 * The animation class shows an animation on the accessory. The animation
	 * runs until mHandler.removeCallbacks has been called.
	 *//*
    private class Animation implements Runnable {
        private int mIndex = 1;

        private final Bitmap mBackground;

        private boolean mIsStopped = false;

	  *//**
	  * Create animation.
	  *//*
        Animation() {
            mIndex = 1;

            // Extract the last part of the host application package name.
            String packageName = mHostAppPackageName
                    .substring(mHostAppPackageName.lastIndexOf(".") + 1);

            // Create background bitmap for animation.
            mBackground = Bitmap.createBitmap(width, height, BITMAP_CONFIG);
            // Set default density to avoid scaling.
            mBackground.setDensity(DisplayMetrics.DENSITY_DEFAULT);

            LinearLayout root = new LinearLayout(mContext);
            root.setLayoutParams(new LayoutParams(width, height));

            LinearLayout sampleLayout = (LinearLayout)LinearLayout.inflate(mContext,
                    R.layout.sample_control, root);
            ((TextView)sampleLayout.findViewById(R.id.sample_control_text)).setText(packageName);
            sampleLayout.measure(width, height);
            sampleLayout.layout(0, 0, sampleLayout.getMeasuredWidth(),
                    sampleLayout.getMeasuredHeight());

            Canvas canvas = new Canvas(mBackground);
            sampleLayout.draw(canvas);

            showBitmap(mBackground);
        }

	   *//**
	   * Stop the animation.
	   *//*
        public void stop() {
            mIsStopped = true;
        }

        public void run() {
            int resourceId;
            switch (mIndex) {
                case 1:
                    resourceId = R.drawable.generic_anim_1_icn;
                    break;
                case 2:
                    resourceId = R.drawable.generic_anim_2_icn;
                    break;
                case 3:
                    resourceId = R.drawable.generic_anim_3_icn;
                    break;
                case 4:
                    resourceId = R.drawable.generic_anim_2_icn;
                    break;
                default:
                    Log.e(SampleExtensionService.LOG_TAG, "mIndex out of bounds: " + mIndex);
                    resourceId = R.drawable.generic_anim_1_icn;
                    break;
            }
            mIndex++;
            if (mIndex > 4) {
                mIndex = 1;
            }

            if (!mIsStopped) {
                updateAnimation(resourceId);
            }
            if (mHandler != null && !mIsStopped) {
                mHandler.postDelayed(this, ANIMATION_DELTA_MS);
            }
        }

	    *//**
	    * Update the animation on the accessory. Only updates the part of the
	    * screen which contains the animation.
	    *
	    * @param resourceId The new resource to show.
	    *//*
        private void updateAnimation(int resourceId) {
            Bitmap animation = BitmapFactory.decodeResource(mContext.getResources(), resourceId,
                    mBitmapOptions);

            // Create a bitmap for the part of the screen that needs updating.
            Bitmap bitmap = Bitmap.createBitmap(animation.getWidth(), animation.getHeight(),
                    BITMAP_CONFIG);
            bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            Rect src = new Rect(ANIMATION_X_POS, ANIMATION_Y_POS, ANIMATION_X_POS
                    + animation.getWidth(), ANIMATION_Y_POS + animation.getHeight());
            Rect dst = new Rect(0, 0, animation.getWidth(), animation.getHeight());

            // Add first the background and then the animation.
            canvas.drawBitmap(mBackground, src, dst, paint);
            canvas.drawBitmap(animation, 0, 0, paint);

            showBitmap(bitmap, ANIMATION_X_POS, ANIMATION_Y_POS);
        }
    };*/

}
