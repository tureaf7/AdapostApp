package com.example.adapostapp.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.adapostapp.MainActivity;
import com.example.adapostapp.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Verifică dacă mesajul conține date
        if (remoteMessage.getData().size() > 0) {
            Log.d("FCM", "Mesaj cu date: " + remoteMessage.getData());
        }

        // Verifică dacă mesajul conține o notificare
        if (remoteMessage.getNotification() != null) {
            Log.d("FCM", "Notificare primită: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "Token nou: " + token);
        // Poți trimite acest token la backend pentru a trimite notificări targetate
    }
}
