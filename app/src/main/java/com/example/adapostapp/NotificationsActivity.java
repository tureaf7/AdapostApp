package com.example.adapostapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends BaseActivity implements NotificationAdapter.OnNotificationClickListener {
    private RecyclerView recyclerViewNotifications;
    private ProgressBar progressBar;
    private TextView textViewEmpty;
    private ImageButton backButton;
    private NotificationAdapter adapter;
    private List<Notification> notifications;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationsListener; // Adăugăm pentru a gestiona listenerul

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        backButton = findViewById(R.id.buttonBackToMain);
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(notifications, this);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(adapter);

        backButton.setOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Încărcăm notificările doar dacă utilizatorul este autentificat
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }

        loadNotifications();
    }

    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_notifications;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verificăm dacă utilizatorul este autentificat
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        }
        setupBottomNavigation(R.id.navigation_notifications);
    }

    private void loadNotifications() {
        String userId = auth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);

        notificationsListener = db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Eroare la încărcarea notificărilor!", Toast.LENGTH_SHORT).show();
                        Log.e("NotificationsActivity", "Eroare la încărcarea notificărilor:", error);
                        return;
                    }
                    if (value != null) {
                        notifications.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Notification notification = doc.toObject(Notification.class);
                            notification.setId(doc.getId()); // Setează ID-ul notificării
                            notifications.add(notification);
                        }
                        textViewEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Oprim listenerul pentru a evita scurgeri de memorie sau cereri neautorizate
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
            Log.d("NotificationsActivity", "Listenerul pentru notificări a fost oprit");
        }
    }

    @Override
    public void onNotificationClick(Notification notification) {
        String type = notification.getType();
        String applicationId = notification.getApplicationId();
        String notificationId = notification.getId();

        // Marchează notificarea ca vizualizată
        if (!notification.isViewed() && notificationId != null) {
            db.collection("Notifications")
                    .document(notificationId)
                    .update("viewed", true)
                    .addOnSuccessListener(aVoid -> {
                        adapter.notifyDataSetChanged();
                        Log.d("NotificationsActivity", "Notificare vizualizată cu succes!");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Eroare la marcarea notificării!", Toast.LENGTH_SHORT).show();
                        Log.e("NotificationsActivity", "Eroare la marcarea notificării:", e);
                    });
        }

        switch (type) {
            case "APPROVAL":
            case "REJECTION":
                Intent intent = new Intent(this, AdoptionApplicationDetailsActivity.class);
                if (applicationId != null) {
                    intent.putExtra("adoptionApplication", applicationId);
                } else {
                    Toast.makeText(this, "Cererea asociată nu a fost găsită!", Toast.LENGTH_SHORT).show();
                }
                startActivity(intent);
                break;

            case "VOLUNTEER_APPROVAL":
            case "VOLUNTEER_REJECTION":
                Intent intent1 = new Intent(this, ApplicationsListActivity.class);
                if (applicationId != null) {
                    intent1.putExtra("volunteerApplication", applicationId);
                } else {
                    Toast.makeText(this, "Cererea asociată nu a fost găsită!", Toast.LENGTH_SHORT).show();
                }
                startActivity(intent1);
                break;

            case "NEW_APPLICATION":
            case "NEW_VOLUNTEER_REQUEST":
                Intent intent2 = new Intent(this, type.equals("NEW_APPLICATION") ? AdoptionApplicationDetailsActivity.class : ApplicationsListActivity.class);
                if (applicationId != null) {
                    intent2.putExtra(type.equals("NEW_APPLICATION") ? "adoptionApplication" : "volunteerApplication", applicationId);
                } else {
                    Toast.makeText(this, "Cererea asociată nu a fost găsită!", Toast.LENGTH_SHORT).show();
                }
                startActivity(intent2);
                break;

            default:
                Toast.makeText(this, "Acțiune necunoscută pentru această notificare!", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}

// Clasa model pentru notificare
class Notification {
    private String id; // ID-ul documentului din Firestore
    private String userId;
    private String title;
    private String body;
    private Date timestamp;
    private String type;
    private String applicationId;
    private boolean viewed;

    public Notification() {}

    public Notification(String userId, String title, String body, Date timestamp, String type, String applicationId) {
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.type = type;
        this.applicationId = applicationId;
        this.viewed = false; // Implicit nevizualizat
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public boolean isViewed() { return viewed; }
    public void setViewed(boolean viewed) { this.viewed = viewed; }
}

// Adaptor pentru RecyclerView
class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;
    private OnNotificationClickListener onNotificationClickListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.onNotificationClickListener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.textViewTitle.setText(notification.getTitle());
        holder.textViewBody.setText(notification.getBody());
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault());
        holder.textViewTimestamp.setText(notification.getTimestamp() != null ? dateFormat.format(notification.getTimestamp()) : "N/A");

        // Schimbă aspectul dacă notificarea este nevizualizată
        holder.imageView.setAlpha(notification.isViewed() ? 0.7f : 1.0f);
        holder.imageView.setVisibility(notification.isViewed() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (onNotificationClickListener != null) {
                onNotificationClickListener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewBody, textViewTimestamp;
        ImageView imageView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewBody = itemView.findViewById(R.id.textViewBody);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}