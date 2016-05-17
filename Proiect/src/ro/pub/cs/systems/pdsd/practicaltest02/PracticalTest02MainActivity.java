package ro.pub.cs.systems.pdsd.practicaltest02;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;



public class PracticalTest02MainActivity extends Activity {

	public EditText portServer;
	public EditText adresaClient;
	public EditText portClient;
	public EditText keyEdit;
	public EditText valueEdit;
	public Button buttonServer;
	public Button getButton;
	public Button putButton;
	public String WEB_SERVICE_ADDRESS = "http://www.timeapi.org/utc/now";
	HashMap<String, HashValue> hashData = new HashMap<String, HashValue>();
	
	public static BufferedReader getReader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

	public static PrintWriter getWriter(Socket socket) throws IOException {
	    return new PrintWriter(socket.getOutputStream(), true);
	}
	public ServerThread serverThread;
	
	public class HashValue{
		String value;
		int date;
		
		public HashValue(String Value, int createDate){
			this.value = Value;
			this.date = createDate;
		}
		public String GetValue(){
			return this.value;
		}
		
		public int GetDate(){
			return this.date;
		}
		
	}
	
	private class CommunicationThread extends Thread {

        private Socket socket;
        private Context ctx;
        
        public CommunicationThread(Context ctx,Socket socket) {
            if (socket != null) {
                this.socket = socket;
                this.ctx = ctx;
                Log.d("Debug", "[SERVER] Created communication thread with: "+socket.getInetAddress());
            }
        }

        @Override
        public void run() {
            if (socket != null) {
                try {
                    BufferedReader bufferedReader = getReader(socket);
                    PrintWriter    printWriter    = getWriter(socket);
                    if (bufferedReader != null && printWriter != null) {
                        //Log.d("Debug", "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type)!");
                        String word = bufferedReader.readLine();

                        HttpClient httpClient = new DefaultHttpClient();
                        HttpPost httpPost = new HttpPost(WEB_SERVICE_ADDRESS);
                        
                        ResponseHandler<String> responseHandler = new BasicResponseHandler();
                        String pageSourceCode = httpClient.execute(httpPost, responseHandler);
                        int minut=0;
                        if (pageSourceCode != null) {
                        			//serverThread.setData(pageSourceCode);
                        			minut = Integer.parseInt(pageSourceCode.substring(16,17));
                                    //printWriter.println(pageSourceCode);
                                    //printWriter.flush();
                               
                        } else {
                            Log.e("Error", "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                        }
                        //Log.d("Debug", "[COMMUNICATION THREAD] GetData ");
                        //String data = serverThread.getData();
                        //Log.d("Debug", "[COMMUNICATION THREAD] I got = "+data);
                        if (word != null && !word.isEmpty() ) {
                        	String delims = "[;]";
                        	String[] tokens = word.split(delims);
                        	
                        	if(tokens.length == 1){
                        		//get
                        		if(hashData.containsKey(tokens[0])){
                        			if(minut - hashData.get(tokens[0]).date <= 1)
                                		printWriter.println(hashData.get(tokens[0]).value);
                                        printWriter.flush();
                        		}
                        		
                        	}else if(tokens.length == 2){
                        		//put
                        		hashData.put(tokens[0], new HashValue(tokens[1], minut));
                        	}


                            //Log.d("Debug", "[COMMUNICATION THREAD] Getting the information from the webservice...");
                            
                        }
                    } else {
                        Log.e("Error", "[COMMUNICATION THREAD] BufferedReader / PrintWriter are null!");
                    }
                    socket.close();
                } catch (IOException ioException) {
                    Log.e("Error", "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                        ioException.printStackTrace();
                } 
            } else {
                Log.e("Error", "[COMMUNICATION THREAD] Socket is null!");
            }
        }
    }
	
	public class ServerThread extends Thread{
		
		
        
        boolean isRunning = true;
        public ServerSocket serverSocket;
        
        public ServerThread (){
        	 Log.e("test","Empty constructor");
        }
        
        public ServerThread(int port) {
            try {
                this.serverSocket = new ServerSocket(port);
                Log.e("test","Am deschis socket pe "+ port);
                isRunning = true;
                Socket socket = serverSocket.accept();
                CommunicationThread communicationThread = new CommunicationThread(getApplicationContext(), socket);
                communicationThread.start();
            } catch (IOException ioException) {
                Log.e("error", "An exception has occurred:" + ioException.getMessage());
                    ioException.printStackTrace();
            }
        }
        
        public ServerSocket getServerSocket(){
            return this.serverSocket;
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Log.d("Debug", "[SERVER] Waiting for a connection...");
                    Socket socket = serverSocket.accept();
                    
                }
            }  catch (IOException ioException) {
                Log.e("error", "An exception has occurred: " + ioException.getMessage());
                    ioException.printStackTrace();
            }
        }
        
        public void stopThread() {
            if (serverSocket != null) {
                interrupt();
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException ioException) {
                    Log.e("Error", "An exception has occurred: " + ioException.getMessage());
                        ioException.printStackTrace();
                }
            }
        }

	}
	
	private class ClientThreadPut extends Thread {

        private Socket socket;
        private String address;
        private int port;
        private String key;
        private String value;
        private String result;
        
        public ClientThreadPut(String clientAddress,int clientPort, String key, String value){
            this.address = clientAddress;
            this.port = clientPort;
            this.key = key;
            this.value = value;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(address, port);
                if (socket == null) {
                    Log.e("Error", "[CLIENT THREAD] Could not create socket!");
                    return;
                }
                BufferedReader bufferedReader = getReader(socket);
                PrintWriter    printWriter    = getWriter(socket);
                if (bufferedReader != null && printWriter != null) {
                    printWriter.println(key + ";" + value);
                } else {
                    Log.e("Error", "[CLIENT THREAD] BufferedReader / PrintWriter are null!");
                }
                socket.close();
            } catch (IOException ioException) {
                Log.e("Error", "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
                    ioException.printStackTrace();
            }
        }
    }
	
	private class ClientThreadGet extends Thread {

        private Socket socket;
        private String address,city,info;
        private int port;
        private String key;
        private String result;
        
        public ClientThreadGet(String clientAddress,int clientPort, String key){
            this.address = clientAddress;
            this.port = clientPort;
            this.key = key;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(address, port);
                if (socket == null) {
                    Log.e("Error", "[CLIENT THREAD] Could not create socket!");
                    return;
                }
                BufferedReader bufferedReader = getReader(socket);
                PrintWriter    printWriter    = getWriter(socket);
                if (bufferedReader != null && printWriter != null) {
                    printWriter.println(key);
                    printWriter.flush();
                    String wordInfo;
                    while ((wordInfo = bufferedReader.readLine()) != null) {
                    	Log.e("Result client", "Result " + wordInfo);
                        final String finalizedWeatherInformation = wordInfo;
                        valueEdit.post(new Runnable() {
                            @Override
                            public void run() {
                            	valueEdit.append(finalizedWeatherInformation + "\n");
                            }
                        });
                    }
                } else {
                    Log.e("Error", "[CLIENT THREAD] BufferedReader / PrintWriter are null!");
                }
                socket.close();
            } catch (IOException ioException) {
                Log.e("Error", "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
                    ioException.printStackTrace();

            }
        }
    }
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_main);
        
        
        portServer = (EditText)findViewById(R.id.ServerPortEditText);
        adresaClient = (EditText) findViewById(R.id.ClientAddressEditText);
        portClient = (EditText) findViewById(R.id.ClientPortEditText);
        keyEdit = (EditText) findViewById(R.id.ClientKeyEditText);
        valueEdit = (EditText) findViewById(R.id.ClientValueEditText);

        buttonServer = (Button) findViewById(R.id.ServerConnect);
        buttonServer.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				String serverPort = portServer.getText().toString();
	            if (serverPort == null || serverPort.isEmpty()) {
	                Toast.makeText(
	                        getApplicationContext(),
	                        "Server port should be filled!",
	                        Toast.LENGTH_SHORT
	                ).show();
	                return;
	            }
	            serverThread = new ServerThread(Integer.parseInt(serverPort));
	            Log.e("test", "portul "+Integer.parseInt(serverPort));
	            if (serverThread.getServerSocket() != null) {
	                serverThread.start();
	            } else {
	                Log.e("error", "[MAIN ACTIVITY] Could not creat server thread!");
	            }
			}
		});
        //buttonServer.setOnClickListener(connectButton);

        getButton = (Button) findViewById(R.id.ClientGetButton);
        //getButton.setOnClickListener(getWord);
        putButton = (Button) findViewById(R.id.ClientPutButton);
        //putButton.setOnClickListener(getWord);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.practical_test02_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
