package com.example.notificationapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import android.app.AlarmManager;
import androidx.core.app.RemoteInput;

public class MessagingService extends Service {

    public static final String ACTION_REPLY = "com.example.REPLY";
    public static final String ACTION_MARK_AS_READ = "com.example.MARK_AS_READ";
    public static final String ACTION_SCHEDULE_NOTIFICATION = "com.example.SCHEDULE_NOTIFICATION";
    public static final String ACTION_CANCEL_NOTIFICATION = "com.example.CANCEL_NOTIFICATION";
    public static final String ACTION_NO = "com.example.ACTION_NO";
    public static final String REMOTE_INPUT_RESULT_KEY = "reply_key";
    public static final String EXTRA_CONVERSATION_ID_KEY = "conversation_id";
    private static final int FOREGROUND_SERVICE_ID = 123;
    private static int notificationId = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(FOREGROUND_SERVICE_ID, createForegroundNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_REPLY.equals(action)) {
                handleReplyAction(intent);
            } else if (ACTION_MARK_AS_READ.equals(action)) {
                handleMarkAsReadAction();
            } else if (ACTION_SCHEDULE_NOTIFICATION.equals(action)) {
                scheduleNotification();
            } else if (ACTION_CANCEL_NOTIFICATION.equals(action)) {
                cancelScheduledNotification();
            } else if (ACTION_NO.equals(action)) {
                handleNoAction();
            }
        }
        return START_STICKY;
    }

    private void handleReplyAction(Intent intent) {
        Bundle results = RemoteInput.getResultsFromIntent(intent);
        if (results != null) {
            CharSequence message = results.getCharSequence(REMOTE_INPUT_RESULT_KEY);
            if (message != null) {
                String replyMessage = message.toString();
                Log.d("MessagingService", "Received reply: " + replyMessage);
                Intent mainActivityIntent = new Intent(this, MainActivity.class);
                mainActivityIntent.putExtra("response", replyMessage);
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainActivityIntent);
                cancelNotification();
            }
        } else {
            Log.w("MessagingService", "No RemoteInput results found!");
        }
    }

    private void handleMarkAsReadAction() {
        Log.d("MessagingService", "Notification marked as read");
        cancelNotification();
    }

    private void handleNoAction() {
        Log.d("MessagingService", "No action performed: Cancelling notifications");
        cancelScheduledNotification();
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
        Intent markAsReadIntent = new Intent(context, MessagingService.class);
        markAsReadIntent.setAction(MessagingService.ACTION_MARK_AS_READ);
        PendingIntent markAsReadPendingIntent = PendingIntent.getService(
                context,
                1,
                markAsReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Mark as Read",
                markAsReadPendingIntent
        )
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build();
    }

    private RemoteInput createReplyRemoteInput() {
        return new RemoteInput.Builder(REMOTE_INPUT_RESULT_KEY).build();
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

        Intent replyIntent = new Intent(context, MessagingService.class);
        replyIntent.setAction(ACTION_REPLY);
        PendingIntent replyPendingIntent = PendingIntent.getService(
                context,
                0,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        RemoteInput remoteInput = createReplyRemoteInput();

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_send,
                        "Reply",
                        replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .build();

        Intent noIntent = new Intent(context, MessagingService.class);
        noIntent.setAction(MessagingService.ACTION_NO);
        PendingIntent noPendingIntent = PendingIntent.getService(
                context,
                2,
                noIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Action noAction =
                new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "No", noPendingIntent)
                        .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(messagingStyle)
                .addAction(replyAction)
                .addAction(noAction)
                .addAction(createMarkAsReadAction(context))
                .setShortcutId("reminder_conv");

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Use the incremented notificationId
        notificationManager.notify(notificationId, builder.build());
        Log.d("MessagingService", "Notification sent with ID: " + notificationId);

        // Increment the notification ID for the next notification
        notificationId++;
    }

    private void scheduleNotification() {
        Context context = this;
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        long interval = 30 * 1000;
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
    }

    private Notification createForegroundNotification() {
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
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        alarmManager.cancel(pendingIntent);
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}