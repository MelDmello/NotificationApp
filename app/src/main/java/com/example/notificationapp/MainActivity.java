package com.example.notificationapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import com.example.notificationapp.MessagingService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    public static final String CHANNEL_ID = "notification_channel";
    public static final int NOTIFICATION_ID = 1;
    private EditText textBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textBox = findViewById(R.id.textBox);

        //createNotificationChannel(); // Moved to MessagingService

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        handleIntentResponse(getIntent());

        //Start Foreground Service
        startMessagingService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntentResponse(intent);
    }

    private void handleIntentResponse(Intent intent) {
        if (intent != null && intent.hasExtra("response")) {
            String response = intent.getStringExtra("response");
            if ("Yes".equals(response)) {
                textBox.setText("Yes");
            }
        }
    }

    private void startMessagingService() {
        Intent serviceIntent = new Intent(this, MessagingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }


    public void startNotification(View view) {
        Intent scheduleIntent = new Intent(this, MessagingService.class);
        scheduleIntent.setAction(MessagingService.ACTION_SCHEDULE_NOTIFICATION);
        startService(scheduleIntent);
    }

    public void cancelScheduledNotification(View view) {
        Intent serviceIntent = new Intent(this, MessagingService.class);
        serviceIntent.setAction(MessagingService.ACTION_CANCEL_NOTIFICATION);
        startService(serviceIntent);
    }
}
