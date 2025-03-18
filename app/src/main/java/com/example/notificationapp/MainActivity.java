package com.example.notificationapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Handle initial intent
        handleIntentResponse(getIntent());

        // Start the service
        startMessagingService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // **CRITICAL: Update the intent!**
        handleIntentResponse(intent);
    }

    private void handleIntentResponse(Intent intent) {
        if (intent != null && intent.hasExtra("response")) {
            String response = intent.getStringExtra("response");
            Log.d("MainActivity", "Received response from notification: " + response);
            if ("Yes".equals(response)) {
                textBox.setText("Yes");
            } else {
                textBox.setText(response);
            }
        }
    }

    private void startMessagingService() {
        Intent serviceIntent = new Intent(this, MessagingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            new Handler(Looper.getMainLooper()).post(() -> {
                startForegroundService(serviceIntent);
            });
        } else {
            startService(serviceIntent);
        }
    }

    public void startNotification(View view) {
        Intent scheduleIntent = new Intent(this, MessagingService.class);
        scheduleIntent.setAction(MessagingService.ACTION_SCHEDULE_NOTIFICATION);
        startService(scheduleIntent);
        Log.d("Main Activity", "Starting Notifications");
    }

    public void cancelScheduledNotification(View view) {
        Intent serviceIntent = new Intent(this, MessagingService.class);
        serviceIntent.setAction(MessagingService.ACTION_CANCEL_NOTIFICATION);
        startService(serviceIntent);
        Log.d("Main Activity", "Stopping Notifications");
    }
}
