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
    private static final String CHANNEL_ID_VOLUNTEER = "VolunteerNotifications";
    private static final String CHANNEL_ID_VOLUNTEER_REQUEST = "VolunteerRequestNotifications"; // Separat pentru cereri noi
    private static int notificationIdCounter = 0;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Notificare primită: " + remoteMessage.getData());

        // Preia titlul și corpul din notificare sau date
        String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : remoteMessage.getData().get("title");
        String body = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : remoteMessage.getData().get("body");
        String clickAction = remoteMessage.getData().get("click_action");
        String chatId = remoteMessage.getData().get("chatId");
        String otherUserId = remoteMessage.getData().get("otherUserId");
        String applicationId = remoteMessage.getData().get("applicationId");
        String requestId = remoteMessage.getData().get("requestId");

        if (title == null || body == null) {
            Log.w(TAG, "Titlul sau corpul notificării este null. Ignorăm notificarea.");
            return;
        }

        // Determină canalul de notificare în funcție de tipul notificării
        String channelId;
        String channelName;
        switch (clickAction) {
            case "CHAT_ACTIVITY":
                channelId = CHANNEL_ID_CHAT;
                channelName = "Chat Notifications";
                break;
            case "APPROVAL_ACTIVITY":
            case "REJECTION_ACTIVITY":
                channelId = CHANNEL_ID_ADOPTION;
                channelName = "Adoption Updates";
                break;
            case "NEW_APPLICATION_ACTIVITY":
                channelId = CHANNEL_ID_ADMIN;
                channelName = "Admin Notifications";
                break;
            case "VOLUNTEER_APPROVAL_ACTIVITY":
            case "VOLUNTEER_REJECTION_ACTIVITY":
                channelId = CHANNEL_ID_VOLUNTEER;
                channelName = "Volunteer Updates";
                break;
            case "NEW_VOLUNTEER_REQUEST_ACTIVITY":
                channelId = CHANNEL_ID_VOLUNTEER_REQUEST;
                channelName = "Volunteer Request Notifications";
                break;
            default:
                channelId = CHANNEL_ID_VOLUNTEER;
                channelName = "Volunteer Notifications";
                Log.w(TAG, "click_action necunoscut: " + clickAction + ". Folosim canalul implicit.");
                break;
        }

        // Configurează canalul de notificare (pentru Android O și mai nou)
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Construiește notificarea
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Configurează intent-ul în funcție de click_action
        if (clickAction != null) {
            Intent intent;
            switch (clickAction) {
                case "CHAT_ACTIVITY":
                    if (chatId == null || otherUserId == null) {
                        Log.w(TAG, "chatId sau otherUserId lipsesc pentru CHAT_ACTIVITY.");
                        intent = new Intent(this, MainActivity.class);
                    } else {
                        intent = new Intent(this, ChatActivity.class); // Deschide direct ChatActivity
                        intent.putExtra("chatId", chatId);
                        intent.putExtra("otherUserId", otherUserId);
                    }
                    break;
                case "APPROVAL_ACTIVITY":
                case "REJECTION_ACTIVITY":
                    intent = new Intent(this, AdoptionApplicationDetailsActivity.class); // Presupunem o activitate pentru adopții
                    if (applicationId != null) {
                        intent.putExtra("applicationId", applicationId);
                    }
                    break;
                case "NEW_APPLICATION_ACTIVITY":
                    intent = new Intent(this, AdoptionApplicationDetailsActivity.class); // Lista cererilor pentru admini
                    if (applicationId != null) {
                        intent.putExtra("applicationId", applicationId);
                    }
                    break;
                case "VOLUNTEER_APPROVAL_ACTIVITY":
                case "VOLUNTEER_REJECTION_ACTIVITY":
                    intent = new Intent(this, ApplicationsListActivity.class); // Lista cererilor de voluntariat
                    if (applicationId != null) {
                        intent.putExtra("volunteerApplication", applicationId);
                    }
                    break;
                case "NEW_VOLUNTEER_REQUEST_ACTIVITY":
                    intent = new Intent(this, ApplicationsListActivity.class); // Deschide lista cererilor noi
                    if (requestId != null) {
                        intent.putExtra("volunteerApplication", requestId);
                    }
                    break;
                default:
                    Log.w(TAG, "click_action necunoscut: " + clickAction + ". Redirecționăm către MainActivity.");
                    intent = new Intent(this, MainActivity.class);
                    break;
            }

            // Adaugă flag-uri pentru a gestiona stiva de activități
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Creează PendingIntent
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    notificationIdCounter, // Folosește un ID unic pentru fiecare notificare
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.setContentIntent(pendingIntent);

            // Loghează detalii pentru debugging
            Log.d(TAG, "Intent creat: " + intent + ", Extras: " + intent.getExtras());
        }

        // Afișează notificarea
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