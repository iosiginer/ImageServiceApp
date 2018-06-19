package thundermore.imageserviceapp;

import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.system.*;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


public class TcpConnection {

    private Socket connectionSocket;
    private OutputStream connectionOutputStream;

    public TcpConnection() {
    }

    public void connect() {

                try {
                    InetAddress serverAddress = InetAddress.getByName("10.0.2.2");

                    connectionSocket = new Socket(serverAddress, 8000);
                    try {
                        //if the socket creation hasn't failed yet
                        connectionOutputStream = connectionSocket.getOutputStream();
                    } catch (Exception e) {
                        Log.e("TCP", "S: Error: ", e);
                    }
                } catch (Exception e) {
                    Log.e("TCP", "S: Error: ", e);
                }
    }

    public void startCommunication(final NotificationManager notificationManager, final NotificationCompat.Builder builder) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                connect();

                //get the image files
                File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                if (null == dcim) return;
                File[] images = dcim.listFiles();

                //create the writer
                PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(connectionOutputStream));


                int imageCount = 0;
                if (null != images) {
                    for (File image : images) {
                        transferImage(image);
                    }
                    Log.d("LOG", "There are " + Integer.toString(images.length) + "pics");
                } else {
                    Log.d("LOG", "Could not find DCIM folder");
                }
            }
        });

        thread.start();

    }

    private void transferImage(File image) {
        return;
    }

    public void closeCommunication() {
        try {
            connectionSocket.close();
        } catch (IOException e) {
            Log.e("TCP", "S: Error: ", e);
        }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }

}
