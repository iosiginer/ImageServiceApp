package thundermore.imageserviceapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int checkReadPremssion = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (checkReadPremssion != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }
        setContentView(R.layout.activity_main);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startService(View view) {
        Button btn = (Button) findViewById(R.id.buttonStart);
        btn.setEnabled(false);
        Button btn2 = (Button) findViewById(R.id.buttonStop);
        btn2.setEnabled(true);
        Intent intent = new Intent(this, ImageServiceService.class);
        startService(intent);
    }

    public void stopService(View view) {
        Button btn = (Button) findViewById(R.id.buttonStart);
        btn.setEnabled(true);
        Button btn2 = (Button) findViewById(R.id.buttonStop);
        btn2.setEnabled(false);
        Intent intent = new Intent(this, ImageServiceService.class);
        stopService(intent);
    }
}
