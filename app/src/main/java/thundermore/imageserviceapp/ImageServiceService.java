package thundermore.imageserviceapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 *
 */
public class ImageServiceService extends Service
{
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Service starting...", Toast.LENGTH_LONG).show();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
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

    private void startTransfer() {
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, "Ima shel barak...", Toast.LENGTH_LONG).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
