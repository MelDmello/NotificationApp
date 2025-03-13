package com.example.notificationapp;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.RemoteInput;

import androidx.core.app.NotificationCompat;
import com.example.notificationapp.MessagingService;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_NO = "com.example.notificationapp.ACTION_NO";

    // In NotificationReceiver.java
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra(RemoteInput.EXTRA_RESULTS_DATA)) {
            // ... (Your existing remote input handling code - this is correct) ...
        }

        if (ACTION_NO.equals(intent.getAction())) {
            // Stop the repeating notifications
            cancelNotifications(context);
        } else {
            // Show the notification EVERY time the alarm goes off!
            MessagingService messagingService = new MessagingService();
            messagingService.showNotification(context);
        }
    }


    // Method to cancel all notifications and stop future ones
    private void cancelNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MainActivity.NOTIFICATION_ID);

        // Cancel the alarm (scheduled notifications)
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }
}
