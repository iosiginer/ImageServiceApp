package thundermore.imageserviceapp;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.util.Output;
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 *
 */
public class ImageServiceService extends Service
{

    private Socket connectionSocket;

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

                    connectionSocket = new Socket(serverAddress, 8001);
                } catch (Exception e) {
                    Log.e("TCP", "S: Error: ", e);
                }
            }
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
        //Registering the wifi receiver
        this.registerReceiver(receiver, filter);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service stopping...", Toast.LENGTH_LONG).show();
        disconnect();
        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * The function responsible to transfer pictures to the service
     */
    public void startTransfer() {
        Toast.makeText(this, "Wi-Fi connected!", Toast.LENGTH_LONG).show();
        Thread thread = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {

                //get the image files
                File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                if (dcim != null) {

                    File[] images = dcim.listFiles();

                    if (null != images) {
                        List<File> allPhotos = collectPhotosFromFolder(images[0]);
                        sendAllImages(allPhotos);
                        Log.d("LOG", "There are " + Integer.toString(images.length) + "pics");
                    } else {
                        Log.d("LOG", "Could not find DCIM folder");
                    }
                }
            }
        });
        thread.start();
    }

    /**
     * The function gets the images and sending them to the service
     * @param allImages the images from the camera
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendAllImages(List<File> allImages) {
        int progressCounter = 0;
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("default",
                "Channel name",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Channel description");
        notificationManager.createNotificationChannel(channel);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
        builder.setContentTitle("Picture Transfer").setContentText("Transfer in progress")
                .setPriority(NotificationCompat.PRIORITY_LOW);
        builder.setSmallIcon(R.drawable.ic_launcher_background);
        final int notify_id = 1;
        for(File image : allImages) {
            progressCounter++;
            try {
                sendImage(image);
            } catch (IOException e) {
                e.printStackTrace();
            }
            builder.setProgress(allImages.size(), progressCounter, false);
            notificationManager.notify(notify_id, builder.build());

        }
        builder.setProgress(0,0, false);
        builder.setContentText("Download Complete...");
        notificationManager.notify(notify_id, builder.build());

    }

    /**
     * The function getting image and send it to the service
     * @param image - solo image
     * @throws IOException - throwing exception when there is problem with the connection/sending
     */
    private void sendImage(File image) throws IOException {
        DataOutputStream dataStream=new DataOutputStream(connectionSocket.getOutputStream());
        OutputStream outStream = connectionSocket.getOutputStream();
        byte[] name = image.getName().getBytes();
        int lengthName = name.length;
        dataStream.writeInt(lengthName);
        outStream.write(name);
        FileInputStream fis = new FileInputStream(image);
        Bitmap bm = BitmapFactory.decodeStream(fis);
        byte[] imageBytes = getBytesFromBitmap(bm);
        int lengthImage = imageBytes.length;
        dataStream.writeInt(lengthImage);
        outStream.write(imageBytes);
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }

    /**
     * The function searching recursively for all photos from a folder and its sub folders
     * @param file a folder or a file
     * @return
     */
    private List<File> collectPhotosFromFolder(File file) {
        List<File> photos = new ArrayList<>();
        if (!file.isDirectory()) {
            photos.add(file);
            return photos;
        }
        File[] listFiles = file.listFiles();
        if (listFiles != null) {
            for (File fileInFolder : listFiles) {
                photos.addAll(collectPhotosFromFolder(fileInFolder));
            }
        }
        return photos;
    }

    /**
     * The function disconnect the server
     */
    private void disconnect() {
        try {
            connectionSocket.close();
        } catch (IOException e) {
            Log.e("CONNECTION", "S: Error: ", e);
        }
    }
}
