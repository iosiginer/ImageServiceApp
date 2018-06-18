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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

/**
 *
 */
public class ImageServiceService extends Service
{
    private TcpConnection connection;
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        //show text to user
        Toast.makeText(this, "Service starting...", Toast.LENGTH_LONG).show();

        connection = new TcpConnection();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        final ImageServiceService self = this;
        receiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                @SuppressLint("ServiceCast")
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
                        Toast.makeText(self, "starting comm", Toast.LENGTH_LONG).show();
                        connection.startCommunication(notificationManager, builder);
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
        connection.closeCommunication();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
