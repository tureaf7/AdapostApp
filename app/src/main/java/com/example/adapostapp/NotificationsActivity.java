package com.example.adapostapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {
    private RecyclerView recyclerViewNotifications;
    private ProgressBar progressBar;
    private TextView textViewEmpty;
    private ImageButton backButton;
    private NotificationAdapter adapter;
    private List<Notification> notifications;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        backButton = findViewById(R.id.buttonBackToMain);
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(notifications, this); // Trece this ca listener
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(adapter);

        backButton.setOnClickListener(v -> onBackPressed());

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(this, ChatListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_animals) {
                startActivity(new Intent(this, ListAnimalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadNotifications();
    }

    private void loadNotifications() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Eroare la încărcarea notificărilor!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    notifications.clear();
                    for (var doc : value) {
                        Notification notification = doc.toObject(Notification.class);
                        notifications.add(notification);
                    }
                    if (notifications.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                    } else {
                        textViewEmpty.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onNotificationClick(Notification notification) {
        String applicationId = notification.getApplicationId();
        if (applicationId == null || applicationId.isEmpty()) {
            Toast.makeText(this, "Cererea asociată nu a fost găsită!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preia animalId din AdoptionApplications
        db.collection("AdoptionApplications")
                .document(applicationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String animalId = documentSnapshot.getString("animalId");
                        if (animalId != null && !animalId.isEmpty()) {
                            // Redirecționează către AnimalProfileActivity cu animalId
                            Intent intent = new Intent(NotificationsActivity.this, AnimalProfileActivity.class);
                            intent.putExtra("animal", animalId);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Animalul asociat nu a fost găsit!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Cererea asociată nu a fost găsită!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la preluarea cererii!", Toast.LENGTH_SHORT).show();
                });
    }
}

// Clasa model pentru notificare
class Notification {
    private String userId;
    private String title;
    private String body;
    private Date timestamp;
    private String type;
    private String applicationId;

    public Notification() {}

    public Notification(String userId, String title, String body, Date timestamp, String type, String applicationId) {
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.type = type;
        this.applicationId = applicationId;
    }

    // Getters și setters
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
}

// Adaptor pentru RecyclerView
class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;
    private OnNotificationClickListener onNotificationClickListener;

    // Interfață pentru gestionarea click-urilor
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
        holder.textViewTimestamp.setText(dateFormat.format(notification.getTimestamp()));

        // Adaugă listener de click pe element
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

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewBody = itemView.findViewById(R.id.textViewBody);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }
    }
}
