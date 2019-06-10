package com.hopelesscoder.motioncontrol;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeMap;

public class MainActivity extends Activity implements PictureCapturingListener {

    private static final int SERVERPORT =  8080;
    //TcpClient mTcpClient;
    TextView textView ;

    private ServerSocket serverSocket;

    Thread serverThread = null;

    private APictureCapturingService pictureService;

    //private byte[] picture = null;

    //private boolean done = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);

        pictureService = PictureCapturingServiceImpl.getInstance(this);

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }


   /* @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable{

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;

            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {


            while (!Thread.currentThread().isInterrupted()) {

                try {

                    final String read = input.readLine();
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    if (read == null ){
                        Thread.currentThread().interrupt();
                        //out.write("Nothing received");
                    }else{

                        //out.write("Ok");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(),read, Toast.LENGTH_LONG).show();
                                textView.setText("Received in input: " + read);
                                startCapturingFromActivity();
                            }
                        });
                        //readMessage = read;
                        SendMessageImpl.sendToTelegram(read);

                        /*while (!done){
                            Thread.sleep(500);
                        }*/
                        //SendMessageImpl.sendToTelegram(read, picture);
                        //done = false;

                        Log.e("testVideoSorv","Received: "+read);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    public void startCapturingFromActivity(){
        pictureService.startCapturing(this, true);
    }

    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            picturesTaken.forEach((pictureUrl, pictureData) -> {
                //convert the byte array 'pictureData' to a bitmap (no need to read the file from the external storage) but in case you
                //You can also use 'pictureUrl' which stores the picture's location on the device
                final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
            });

            //picture = picturesTaken.firstEntry().getValue();
            //done = true;
            SendMessageImpl.sendToTelegram("", picturesTaken.firstEntry().getValue());
            showToast("Done capturing all photos!");
            return;
        }
        showToast("No camera detected!");
    }

    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                //convert byte array 'pictureData' to a bitmap (no need to read the file from the external storage)
                final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                //scale image to avoid POTENTIAL "Bitmap too large to be uploaded into a texture" when displaying into an ImageView
                final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                //do whatever you want with the bitmap or the scaled one...
            });
            showToast("Picture saved to " + pictureUrl);
        }
    }

    private void showToast(final String text) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
    }
}
