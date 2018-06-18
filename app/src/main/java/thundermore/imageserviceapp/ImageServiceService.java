package thundermore.imageserviceapp;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 *
 */
public class ImageServiceService extends Service
{

    private Socket connectionSocket;
    private OutputStream connectionOutputStream;

    @Override
    public void onCreate() {
        super.onCreate();

        //show text to user
        Toast.makeText(this, "Service starting...", Toast.LENGTH_LONG).show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddress = InetAddress.getByName("10.0.2.2");

                    connectionSocket = new Socket(serverAddress, 8600);
                    try {
                        //if the socket creation hasn't failed yet
                        connectionOutputStream = connectionSocket.getOutputStream();
                    } catch (Exception e) {
                        Log.e("TCP", "S: Error: ", e);
                    }
                } catch (Exception e) {
                    Log.e("TCP", "S: Error: ", e);
                }            }
        });

        thread.start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                @SuppressLint("ServiceCast") final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel = new NotificationChannel("default", "ImageService", NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.setDescription("Notifications for ImageService");
                notificationManager.createNotificationChannel(notificationChannel);

                final NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                        "default");
                builder.setSmallIcon(R.drawable.ic_launcher_background);
                builder.setContentTitle("Image Transfer status");
                builder.setContentText("In progress");

                if (networkInfo != null) {
                    if ((networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                            && (networkInfo.getState() == NetworkInfo.State.CONNECTED)) {
                        startTransfer();
                    }
                }
            }
        };
        this.registerReceiver(receiver, filter);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service stopping...", Toast.LENGTH_LONG).show();
        disconnect();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startTransfer() {
        final ImageServiceService self = this;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                //get the image files
                File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                if (dcim != null) {

                    File[] images = dcim.listFiles();

                    //create the writer
                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(connectionOutputStream));

                    if (null != images) {
//                    for (File image : images) {
//                        transferImage(image);
//                    }
                        Toast.makeText(self, Integer.toString(images.length), Toast.LENGTH_LONG);
                        Log.d("LOG", "There are " + Integer.toString(images.length) + "pics");
                    } else {
                        Log.d("LOG", "Could not find DCIM folder");
                    }
                }
            }
        });

        thread.start();
    }

    public void connect() {

        try {
            InetAddress serverAddress = InetAddress.getByName("10.0.2.2");

            connectionSocket = new Socket(serverAddress, 8600);
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


    private void disconnect() {
        try {
            connectionSocket.close();
        } catch (IOException e) {
            Log.e("CONNECTION", "S: Error: ", e);
        }
    }
}
