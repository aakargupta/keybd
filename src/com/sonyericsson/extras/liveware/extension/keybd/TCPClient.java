package com.sonyericsson.extras.liveware.extension.keybd;

import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import com.sonyericsson.extras.liveware.extension.util.Dbg;


public class TCPClient {

	private String serverMessage;
	public static final String SERVERIP = "172.21.17.76"; //your computer IP address
	public static final int SERVERPORT = 800;
	public OnMessageReceived mMessageListener = null;
	private boolean mRun = false;
	
	public Socket socket;
	
	
	public PrintWriter out;
	public DataInputStream in;
	public DataOutputStream dos;
	/**
	 *  Constructor of the class. OnMessagedReceived listens for the messages received from server
	 */
	public TCPClient(OnMessageReceived listener) {
		mMessageListener = listener;
	}

	/**
	 * Sends the message entered by client to the server
	 * @param message text entered by client
	 */
	public void sendMessage(String message){
		if (out != null && !out.checkError()) {
			out.println(message);
			out.flush();
		}
	}

	public void sendBytes(byte[] myByteArray) throws IOException {
		sendBytes(myByteArray, 0, myByteArray.length);
	}

	public void sendBytes(byte[] myByteArray, int start, int len) throws IOException {
		if (len < 0)
			throw new IllegalArgumentException("Negative length not allowed");
		if (start < 0 || start >= myByteArray.length)
			throw new IndexOutOfBoundsException("Out of bounds: " + start);
		// Other checks if needed.

		// May be better to save the streams in the support class;
		// just like the socket variable.


		//dos.writeInt(len);
		if (len > 0) {
			dos.write(myByteArray, start, len);
		}
	}

	public void stopClient() throws IOException{
		
		/*byte[] sendbytes = new byte[8];

		sendbytes[0] = (byte)4;
		for (int i = 1; i < 8; ++i)
		{
			sendbytes[i] = (byte)0;
		}
		sendBytes(sendbytes);*/
		
		mRun = false;
	}

	int fromByteArray(byte[] bytes) {		
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	double fromByteArraytoDouble(byte[] bytes) {
		
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
	}

	void getChannelInfo() throws IOException
	{
		byte[] buffer = new byte[4];
		in.read(buffer);
		int packet = fromByteArray(buffer);

		in.read(buffer);
		int type = fromByteArray(buffer);

		//System.out.println("Packet: "+packet);
		//System.out.println("Type: "+type);

		if (type != 1) {
			//System.out.println("Type: "+type);
			System.out.println("Type Error ");
		}

		if (packet != 2){

			System.out.println("Packet Error ");
		}

		in.read(buffer);
		int count = fromByteArray(buffer);
		System.out.println("Count: "+count);

		int IndiciesToSkip = 1;
		for (int i = 0; i < count; ++i)
		{
			int s;
			byte[] b; 
			//int cIndex = 0;

			int iRx = in.read(buffer);
			s = fromByteArray(buffer);

			b = new byte[s];
			iRx = in.read(b);

			char[] c = new char[s];
			/*System.Text.Decoder d = System.Text.Encoding.UTF8.GetDecoder();
		    int charLen = d.GetChars(b, 0, s, c, 0);
		    System.String szData = new System.String(c);
		    txtDataRx.Text += "\r\n" + szData;
            channels[i] = szData;*/

			String channel = new String(b, "UTF-8");
			Log.d("Keybd", "Channel: "+channel);
			/*if (i > 0 && 0 == IndiciesToSkip)
            {
                //ViconChannel vc = new ViconChannel(szData);
               // IndiciesToSkip = vc.ChannelEntries;
               // viconArray.Add(vc);
            }*/
			IndiciesToSkip--;

		}

	}

	
	public void run() {

		mRun = true;

		try {
			//here you must put your computer's IP address.
			InetAddress serverAddr = InetAddress.getByName(SERVERIP);

			//System.out.println("TCP Client C: Connecting...");

			//create a socket to make the connection with the server
			socket = new Socket(serverAddr, SERVERPORT);            
			
			
			try {

				//send the message to the server
				// out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

				OutputStream out = socket.getOutputStream(); 
				dos = new DataOutputStream(out);
				// dos.writeLong(3);
				// dos.writeLong(0);
				int type, packet, count;
				byte[] buffer;
				byte[] sendbytes = new byte[8];

				sendbytes[0] = (byte)1;
				for (int i = 1; i < 8; ++i)
				{
					sendbytes[i] = (byte)0;
				}
				sendBytes(sendbytes);

				buffer = new byte[4];
				in = new DataInputStream(socket.getInputStream());
				in.read(buffer);
				packet = fromByteArray(buffer);

				in.read(buffer);
				type = fromByteArray(buffer);
				//in.read(aa);
				//System.out.println("Packet: "+packet);
				//System.out.println("Type: "+type);

				if (type != 1) {
					//System.out.println("Type: "+type);
					// System.out.println("Type Error ");
				}

				if (packet != 2){

					// System.out.println("Packet Error ");
				}

				in.read(buffer);
				count = fromByteArray(buffer);
				//System.out.println("Count: "+count);


				String[] channels = new String[count];
				//viconArray = new ArrayList();
				int IndiciesToSkip = 1;
				for (int i = 0; i < count; ++i)
				{
					int s;
					byte[] b; 
					//int cIndex = 0;

					int iRx = in.read(buffer);
					s = fromByteArray(buffer);
					//Log.d("Keybd", "Val: "+s);
					b = new byte[s];
					iRx = in.read(b);
					
					char[] c = new char[s];


					String channel = new String(b, "UTF-8");
					//System.out.println("Channel: "+channel);
					Log.d("Keybd", "Channel: "+channel);
					IndiciesToSkip--;
				}

				/*sendbytes = new byte[8];

				sendbytes[0] = (byte)3;
				for (int i = 1; i < 8; ++i)
				{
					sendbytes[i] = (byte)0;
				}
				sendBytes(sendbytes);
				//DATA
				while(mRun)
				{

					
					//sendBytes(sendbytes);
					
					buffer = new byte[4];
					//in = new DataInputStream(socket.getInputStream());
					in.read(buffer);
					long currTime = System.currentTimeMillis();
					
					packet = fromByteArray(buffer);

					in.read(buffer);
					type = fromByteArray(buffer);
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

					in.read(buffer);
					count = fromByteArray(buffer);
					//System.out.println("Count: "+count);
					String dat = ""; 
					//double[][] pts = new double[4][4];
					//double[] finger = new double[3];
					Double[] allData = new Double[20];
					int dcnt = 0;
					
					for (int i = 0; i < count; ++i)
					{
						//if(i>=9)
						//{

						int s;
						byte[] b; 
						//int cIndex = 0;
						buffer = new byte[8];            
						int iRx = in.read(buffer);
						double dd = fromByteArraytoDouble(buffer);
						//String channel = new String(b, "UTF-8");
						
						if (i>=9)
						{
							//System.out.print(" "+dd);	
							dat = dat+dd+" "; 
							if(i>=9 && i<25)
							{
								//int q = (i-9)/4;
								//pts[q][(i-9)%4] = dd;
								allData[dcnt++] = dd;
								
							}
							else if(i>=33 && i<36)
							{
								//finger[(i-33)] = dd; 
								allData[dcnt++] = dd;
							}

						}

						//}				
					}
					allData[dcnt] = (double)currTime;
					Log.d("TCP", System.currentTimeMillis()+" "+dat);
					
					
					//mMessageListener.messageReceived(allData);
				}
				
				in.close();
				socket.close();


				//Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
				//Dbg.d("KEYBD1: "+serverMessage);*/

			} catch (Exception e) {

				Log.e("Keybd", "TCP S: Error", e);
				//System.out.println("Error "+ e.getStackTrace());

			} finally {
				//the socket must be closed. It is not possible to reconnect to this socket
				// after it is closed, which means a new socket instance has to be created.
				//socket.close();
			}

		} catch (Exception e) {

			Log.e("Keybd", "C: Error", e);
			//System.out.println("Error "+e.getStackTrace());
		}

	}
	
	
	
	//Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
		//class at on asynckTask doInBackground
		public interface OnMessageReceived {
			//public void messageReceived(String message);
			public void messageReceived(Double[] allData);
		}
	
	/*
	public void run() {

		mRun = true;

		try {
			//here you must put your computer's IP address.
			InetAddress serverAddr = InetAddress.getByName(SERVERIP);

			//System.out.println("TCP Client C: Connecting...");

			//create a socket to make the connection with the server
			Socket socket = new Socket(serverAddr, SERVERPORT);            
			
			try {

				//send the message to the server
				// out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

				OutputStream out = socket.getOutputStream(); 
				dos = new DataOutputStream(out);
				// dos.writeLong(3);
				// dos.writeLong(0);
				int type, packet, count;
				byte[] buffer;
				byte[] sendbytes = new byte[8];

				sendbytes[0] = (byte)1;
				for (int i = 1; i < 8; ++i)
				{
					sendbytes[i] = (byte)0;
				}
				sendBytes(sendbytes);

				buffer = new byte[4];
				in = new DataInputStream(socket.getInputStream());
				in.read(buffer);
				packet = fromByteArray(buffer);

				in.read(buffer);
				type = fromByteArray(buffer);
				//in.read(aa);
				//System.out.println("Packet: "+packet);
				//System.out.println("Type: "+type);

				if (type != 1) {
					//System.out.println("Type: "+type);
					// System.out.println("Type Error ");
				}

				if (packet != 2){

					// System.out.println("Packet Error ");
				}

				in.read(buffer);
				count = fromByteArray(buffer);
				//System.out.println("Count: "+count);


				String[] channels = new String[count];
				//viconArray = new ArrayList();
				int IndiciesToSkip = 1;
				for (int i = 0; i < count; ++i)
				{
					int s;
					byte[] b; 
					//int cIndex = 0;

					int iRx = in.read(buffer);
					s = fromByteArray(buffer);

					b = new byte[s];
					iRx = in.read(b);

					char[] c = new char[s];


					String channel = new String(b, "UTF-8");
					//System.out.println("Channel: "+channel);
					Log.d("Keybd", "Channel: "+channel);
					IndiciesToSkip--;
				}

				sendbytes = new byte[8];

				sendbytes[0] = (byte)3;
				for (int i = 1; i < 8; ++i)
				{
					sendbytes[i] = (byte)0;
				}
				sendBytes(sendbytes);
				//DATA
				while(mRun)
				{

					
					//sendBytes(sendbytes);
					
					buffer = new byte[4];
					//in = new DataInputStream(socket.getInputStream());
					in.read(buffer);
					long currTime = System.currentTimeMillis();
					
					packet = fromByteArray(buffer);

					in.read(buffer);
					type = fromByteArray(buffer);
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

					in.read(buffer);
					count = fromByteArray(buffer);
					//System.out.println("Count: "+count);
					String dat = ""; 
					//double[][] pts = new double[4][4];
					//double[] finger = new double[3];
					Double[] allData = new Double[20];
					int dcnt = 0;
					
					for (int i = 0; i < count; ++i)
					{
						//if(i>=9)
						//{

						int s;
						byte[] b; 
						//int cIndex = 0;
						buffer = new byte[8];            
						int iRx = in.read(buffer);
						double dd = fromByteArraytoDouble(buffer);
						//String channel = new String(b, "UTF-8");
						
						if (i>=9)
						{
							//System.out.print(" "+dd);	
							dat = dat+dd+" "; 
							if(i>=9 && i<25)
							{
								//int q = (i-9)/4;
								//pts[q][(i-9)%4] = dd;
								allData[dcnt++] = dd;
								
							}
							else if(i>=33 && i<36)
							{
								//finger[(i-33)] = dd; 
								allData[dcnt++] = dd;
							}

						}

						//}				
					}
					allData[dcnt] = (double)currTime;
					Log.d("TCP", System.currentTimeMillis()+" "+dat);
					
					
					//mMessageListener.messageReceived(allData);
				}
				
				in.close();
				socket.close();


				//Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
				//Dbg.d("KEYBD1: "+serverMessage);

			} catch (Exception e) {

				Log.e("TCP", "S: Error", e);
				//System.out.println("Error "+ e.getStackTrace());

			} finally {
				//the socket must be closed. It is not possible to reconnect to this socket
				// after it is closed, which means a new socket instance has to be created.
				socket.close();
			}

		} catch (Exception e) {

			Log.e("TCP", "C: Error", e);
			//System.out.println("Error "+e.getStackTrace());
		}

	}
*/
	/*
	public void run() {

		mRun = true;

		try {
			//here you must put your computer's IP address.
			InetAddress serverAddr = InetAddress.getByName(SERVERIP);

			Log.e("TCP Client", "C: Connecting...");

			//create a socket to make the connection with the server
			Socket socket = new Socket(serverAddr, SERVERPORT);


			try {

				//send the message to the server
				//out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

				OutputStream out = socket.getOutputStream(); 
				dos = new DataOutputStream(out);

				byte[] sendbytes = new byte[8];

				//If doing channel info, change 3 to 1.
				sendbytes[0] = (byte)3;
				for (int i = 1; i < 8; ++i)
				{
					sendbytes[i] = (byte)0;
				}
				sendBytes(sendbytes);            	

				Log.e("TCP Client", "C: Sent.");

				in = new DataInputStream(socket.getInputStream());

				//getChannelInfo()

				//Log.e("RESPONSE FROM SERVER1i", "S: Received Messagei: '" + in.read()+ "'");
				//Dbg.d("KEYBD2i: ");
				//in this while the client listens for the messages sent by the server
				while (mRun) {

					byte[] buffer = new byte[4];
					in = new DataInputStream(socket.getInputStream());
					in.read(buffer);
					int packet = fromByteArray(buffer);

					in.read(buffer);
					int type = fromByteArray(buffer);
					//in.read(aa);
					//System.out.print("Packet: "+packet);
					//System.out.print("Type: "+type);

					if (type != 1) {
						//System.out.println("Type: "+type);
						System.out.println("Type Error ");
					}

					if (packet != 2){

						System.out.println("Packet Error ");
					}

					//String[] channels = new String[count];
					//viconArray = new ArrayList();
					//int IndiciesToSkip = 1;

					in.read(buffer);
					int count = fromByteArray(buffer);
					System.out.print("Count: "+count+", ");

					for (int i = 0; i < count; ++i)
					{
						int s;
						byte[] b; 
						//int cIndex = 0;
						buffer = new byte[8];            
						int iRx = in.read(buffer);
						double dd = fromByteArraytoDouble(buffer);	               

						//String channel = new String(b, "UTF-8");
						Log.d("Keybd", " "+dd);

					}
					long currTime = System.currentTimeMillis();
				while(System.currentTimeMillis()<currTime+2000)
				{
				}


					if (serverMessage != null && mMessageListener != null) {
						//call the method messageReceived from MyActivity class
						mMessageListener.messageReceived(serverMessage);
					}
					serverMessage = null;

				}

				//Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
				//Dbg.d("KEYBD1: "+serverMessage);

			} catch (Exception e) {

				Log.e("TCP", "S: Error", e);


			} finally {
				//the socket must be closed. It is not possible to reconnect to this socket
				// after it is closed, which means a new socket instance has to be created.
				socket.close();
			}

		} catch (Exception e) {

			Log.e("TCP", "C: Error", e);

		}

	}
	 */
	
}