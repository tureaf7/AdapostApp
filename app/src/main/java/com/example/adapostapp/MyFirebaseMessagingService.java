package com.example.adapostapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMessaging";
    private static final String CHANNEL_ID_CHAT = "ChatNotifications";
    private static final String CHANNEL_ID_ADOPTION = "AdoptionNotifications";
    private static final String CHANNEL_ID_ADMIN = "AdminNotifications";
    private static int notificationIdCounter = 0;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Notificare primită: " + remoteMessage.getData());

        String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : remoteMessage.getData().get("title");
        String body = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : remoteMessage.getData().get("body");
        String clickAction = remoteMessage.getData().get("click_action");
        String chatId = remoteMessage.getData().get("chatId");
        String otherUserId = remoteMessage.getData().get("otherUserId");
        String applicationId = remoteMessage.getData().get("applicationId");

        if (title == null || body == null) {
            Log.w(TAG, "Titlul sau corpul notificării este null. Ignorăm notificarea.");
            return;
        }

        String channelId;
        String channelName;
        if ("CHAT_ACTIVITY".equals(clickAction)) {
            channelId = CHANNEL_ID_CHAT;
            channelName = "Chat Notifications";
        } else if ("APPROVAL_ACTIVITY".equals(clickAction) || "REJECTION_ACTIVITY".equals(clickAction)) {
            channelId = CHANNEL_ID_ADOPTION;
            channelName = "Adoption Updates";
        } else if ("NEW_APPLICATION_ACTIVITY".equals(clickAction)) {
            channelId = CHANNEL_ID_ADMIN;
            channelName = "Admin Notifications";
        } else {
            channelId = CHANNEL_ID_ADOPTION;
            channelName = "Adoption Updates";
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true);

        if (clickAction != null) {
            Intent intent;
            if ("CHAT_ACTIVITY".equals(clickAction)) {
                if (chatId == null || otherUserId == null) {
                    Log.w(TAG, "chatId sau otherUserId lipsesc pentru CHAT_ACTIVITY. Redirecționăm către MainActivity.");
                    intent = new Intent(this, MainActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                    intent.setAction("OPEN_CHAT_FROM_NOTIFICATION");
                    intent.putExtra("click_action", clickAction);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("otherUserId", otherUserId);
                    intent.putExtra("from_notification", true);
                }
            } else if ("APPROVAL_ACTIVITY".equals(clickAction) || "REJECTION_ACTIVITY".equals(clickAction)) {
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("click_action", clickAction);
                intent.putExtra("from_notification", true);
            } else if ("NEW_APPLICATION_ACTIVITY".equals(clickAction)) {
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("click_action", clickAction);
                if (applicationId != null) {
                    intent.putExtra("applicationId", applicationId);
                }
                intent.putExtra("from_notification", true);
            } else {
                Log.w(TAG, "click_action necunoscut: " + clickAction + ". Redirecționăm către MainActivity.");
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("from_notification", true);
            }

            // Loghează intent-ul creat
            Log.d(TAG, "Intent creat pentru PendingIntent: " + intent.toString());
            Log.d(TAG, "Acțiune: " + intent.getAction());
            Log.d(TAG, "Extra-uri: " + intent.getExtras());

            // Folosim FLAG_ACTIVITY_NEW_TASK pentru a lansa aplicația dacă este închisă
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.setContentIntent(pendingIntent);
        }

        notificationManager.notify(notificationIdCounter++, builder.build());
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Token FCM actualizat: " + token);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token FCM salvat în Firestore: " + token))
                    .addOnFailureListener(e -> Log.e(TAG, "Eroare la salvarea token-ului FCM", e));
        } else {
            Log.w(TAG, "Utilizatorul nu este autentificat. Token-ul FCM nu a fost salvat.");
        }
    }
}