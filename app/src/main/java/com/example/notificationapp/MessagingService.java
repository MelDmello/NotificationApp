package com.example.notificationapp;

import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;

import com.example.notificationapp.MainActivity;
import com.example.notificationapp.NotificationReceiver;

public class MessagingService extends Service {
    public static final String ACTION_REPLY = "com.example.REPLY";
    public static final String ACTION_MARK_AS_READ = "com.example.MARK_AS_READ";
    public static final String ACTION_SCHEDULE_NOTIFICATION = "com.example.SCHEDULE_NOTIFICATION";
    public static final String ACTION_CANCEL_NOTIFICATION = "com.example.CANCEL_NOTIFICATION";
    private static final int FOREGROUND_SERVICE_ID = 123;  // Unique ID for foreground service
    public MessagingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(); // Initialize notification channel
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_REPLY.equals(action)) {
                handleReplyAction();
            } else if (ACTION_MARK_AS_READ.equals(action)) {
                handleMarkAsReadAction();
            } else if (ACTION_SCHEDULE_NOTIFICATION.equals(action)) {
                scheduleNotification();
            }
            else if (ACTION_CANCEL_NOTIFICATION.equals(action)) {
                cancelScheduledNotification();
            }
        }


        return START_NOT_STICKY; // Service is only needed to schedule the alarm
    }


    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (ACTION_REPLY.equals(action)) {
            handleReplyAction();
        } else if (ACTION_MARK_AS_READ.equals(action)) {
            handleMarkAsReadAction();
        }

        stopSelf();
    }

    private void handleReplyAction() {
        // Automatically set the reply to "Yes"
        String replyMessage = "Yes";
        Log.d("MessagingService", "Auto reply sent: " + replyMessage);

        // Send the "Yes" response back to MainActivity
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.putExtra("response", replyMessage);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainActivityIntent);

        // Cancel the notification
        cancelNotification();
    }

    private void handleMarkAsReadAction() {
        Log.d("MessagingService", "Notification marked as read");
        cancelNotification();
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MainActivity.NOTIFICATION_ID);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    MainActivity.CHANNEL_ID,
                    "Notification Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for notifications");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            channel.setAllowBubbles(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private NotificationCompat.Action createMarkAsReadAction(Context context) {
        Intent noIntent = new Intent(context, MessagingService.class);
        noIntent.setAction(MessagingService.ACTION_MARK_AS_READ);
        PendingIntent noPendingIntent = PendingIntent.getService(
                context,
                1, // Unique ID
                noIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "No",
                noPendingIntent
        )
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build();
    }

    public void showNotification(Context context) {
        Person user = new Person.Builder()
                .setName("Reminder")
                .setKey("reminder_system")
                .setImportant(true)
                .build();

        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(user)
                .setConversationTitle("Reminder!")
                .addMessage("Do you want to continue??", System.currentTimeMillis(), user);

        Log.d("MessagingService", "MessagingStyle: " + messagingStyle.getMessages().size() + " messages");

        // Yes Action (Reply)
        Intent yesIntent = new Intent(context, MessagingService.class);
        yesIntent.setAction(MessagingService.ACTION_REPLY);
        PendingIntent yesPendingIntent = PendingIntent.getService(
                context,
                0,
                yesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        NotificationCompat.Action yesAction =
                new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_send, "Yes", yesPendingIntent)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setContentTitle("Reminder")
                .setContentText("Do you want to continue?")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(messagingStyle)
                .addAction(yesAction)
                .addInvisibleAction(createMarkAsReadAction(context)) // Use helper function
                .setShortcutId("reminder_conv");

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MainActivity.NOTIFICATION_ID, builder.build());

        Log.d("MessagingService", "Notification sent with ID: " + MainActivity.NOTIFICATION_ID);
    }


    private void scheduleNotification() {
        Context context = this;
        Intent intent = new Intent(context, NotificationReceiver.class); // Target the receiver!

        PendingIntent pendingIntent = PendingIntent.getBroadcast( // Use getBroadcast!
                context,
                0, // Request code (important: keep this the same for cancelling)
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        long interval = 30 * 1000;
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);


        //Create notification for foreground service
        Notification notification = createForeGroundNotification();
        startForeground(FOREGROUND_SERVICE_ID, notification);

        stopSelf(); // Stop the service after scheduling
    }

    private Notification createForeGroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

        return new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setContentTitle("Reminder Service Running")
                .setContentText("App is scheduling reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void cancelScheduledNotification() {
        Context context = this;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class); // Target the receiver!
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0, // Must match the request code used when scheduling!
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        alarmManager.cancel(pendingIntent);

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
