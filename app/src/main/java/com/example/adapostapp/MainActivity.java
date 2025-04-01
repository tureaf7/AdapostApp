package com.example.adapostapp;

import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.adapostapp.utils.UserUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout horizontalLinearLayout;
    private ImageButton notificationButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBar;
    private TextView adoptInfoText, donatText, voluntarText, noneAnimalTextView;
    private FirebaseUser user;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "AdapostAppPrefs";
    private static final String KEY_HAS_UNREAD_MESSAGES = "hasUnreadMessages";
    private ListenerRegistration unreadMessagesListener;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inițializări UI
        horizontalLinearLayout = findViewById(R.id.horizontalLinearLayout);
        notificationButton = findViewById(R.id.notificationButton);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        progressBar = findViewById(R.id.progressBar);
        adoptInfoText = findViewById(R.id.adoptInfoText);
        donatText = findViewById(R.id.donatText);
        voluntarText = findViewById(R.id.voluntarText);
        noneAnimalTextView = findViewById(R.id.noneAnimalstextView);

        // Inițializări Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // Inițializează SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        notificationButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(MainActivity.this, ChatListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_animals) {
                startActivity(new Intent(MainActivity.this, ListAnimalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // Creează canalul de notificări
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default_channel",
                    "Default Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        updateMessagesIcon(sharedPreferences.getBoolean(KEY_HAS_UNREAD_MESSAGES, false));
        loadChats();
        handleNotificationIntent(getIntent());
        checkNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        horizontalLinearLayout.removeAllViews();
        fetchAnimals();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "Intent-ul este null.");
            return;
        }

        Log.d(TAG, "Procesăm intent-ul: " + intent.toString());
        Log.d(TAG, "Acțiune: " + intent.getAction());
        if (intent.getExtras() != null) {
            Log.d(TAG, "Extra-uri: " + intent.getExtras().toString());
        } else {
            Log.d(TAG, "Nu există extra-uri.");
        }

        // Verificăm dacă intent-ul provine dintr-o notificare
        boolean fromNotification = intent.getBooleanExtra("from_notification", false);
        String clickAction = intent.getStringExtra("click_action");
        String chatId = intent.getStringExtra("chatId");
        String otherUserId = intent.getStringExtra("otherUserId");

        if (fromNotification || clickAction != null) {
            Log.d(TAG, "Intent de notificare detectat: click_action=" + clickAction + ", chatId=" + chatId + ", otherUserId=" + otherUserId);

            if ("CHAT_ACTIVITY".equals(clickAction) && chatId != null && otherUserId != null) {
                Intent chatIntent = new Intent(this, ChatActivity.class);
                chatIntent.putExtra("chatId", chatId);
                chatIntent.putExtra("otherUserId", otherUserId);
                startActivity(chatIntent);
            } else {
                Log.w(TAG, "Datele notificării sunt incomplete: click_action=" + clickAction + ", chatId=" + chatId + ", otherUserId=" + otherUserId);
            }
        } else {
            Log.d(TAG, "Intent-ul nu provine dintr-o notificare (from_notification=false și click_action lipsă).");
        }
    }

    @SuppressLint("SetTextI18n")
    private void fetchAnimals() {
        horizontalLinearLayout.removeAllViews();
        progressBar.setVisibility(View.VISIBLE); // Afișează indicatorul de încărcare

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            readAnimalsFromDB();
            return;
        }

        String uid = currentUser.getUid();

        // Preia favoritele utilizatorului
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> favoriteAnimalIds = new ArrayList<>();
                    if (documentSnapshot.exists() && documentSnapshot.contains("favorites")) {
                        List<DocumentReference> favorites = (List<DocumentReference>) documentSnapshot.get("favorites");
                        if (favorites != null) {
                            for (DocumentReference ref : favorites) {
                                favoriteAnimalIds.add(ref.getId());
                            }
                        }
                    }
                    readAnimalsFromDB(favoriteAnimalIds);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Eroare la preluarea listei de favorite: ", e));
    }

    private void readAnimalsFromDB() {
        db.collection("Animals").whereEqualTo("adopted", false).limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("Firebase", "Nu au fost găsite animale.");
                        noneAnimalTextView.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Animal animal = document.toObject(Animal.class);
                        addAnimalCardToUI(animal, "");
                    }
                    View itemView = LayoutInflater.from(this).inflate(R.layout.view_more, horizontalLinearLayout, false);
                    ImageButton imageButton = itemView.findViewById(R.id.imageButton);
                    imageButton.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, ListAnimalActivity.class);
                        startActivity(intent);
                    });
                    horizontalLinearLayout.addView(itemView);
                }).addOnFailureListener(e -> {
                    Log.w("Firebase", "Error getting documents.", e);
                    Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
                });
    }

    private void readAnimalsFromDB(List<String> favoriteAnimalIds) {
        db.collection("Animals").whereEqualTo("adopted", false).limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("Firebase", "Nu au fost găsite animale.");
                        noneAnimalTextView.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Animal animal = document.toObject(Animal.class);
                        animal.setId(document.getId());
                        String favoriteTag = favoriteAnimalIds.contains(document.getId()) ? "favorite" : "not_favorite";
                        addAnimalCardToUI(animal, favoriteTag);
                    }
                    View itemView = LayoutInflater.from(this).inflate(R.layout.view_more, horizontalLinearLayout, false);
                    ImageButton imageButton = itemView.findViewById(R.id.imageButton);
                    imageButton.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, ListAnimalActivity.class);
                        startActivity(intent);
                    });
                    horizontalLinearLayout.addView(itemView);
                }).addOnFailureListener(e -> {
                    Log.w("Firebase", "Error getting documents.", e);
                    Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
                });
    }

    private void addAnimalCardToUI(Animal animal, String favoriteTag) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.card_item, horizontalLinearLayout, false);

        ImageView animalPhoto = itemView.findViewById(R.id.imageItemImageView);
        ImageView imageGen = itemView.findViewById(R.id.imageGen);
        TextView animalName = itemView.findViewById(R.id.textViewName);
        TextView animalBreed = itemView.findViewById(R.id.textViewBreed);
        TextView animalAge = itemView.findViewById(R.id.textViewAge);
        ImageButton imageButtonFavorite = itemView.findViewById(R.id.imageButtonFavorite);
        imageButtonFavorite.setVisibility(View.GONE);

        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalAge.setText(animal.getYears() + (animal.getYears() == 1 ? " an" : " ani"));
        imageGen.setImageResource(animal.getGen().equals("Mascul") ? R.drawable.ic_male : R.drawable.ic_female);

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(this)
                    .load(animal.getPhoto())
                    .error(R.drawable.ic_launcher_foreground)
                    .into(animalPhoto);
        }

        checkUserRole(user, imageButtonFavorite);

        imageButtonFavorite.setTag(favoriteTag);
        if ("favorite".equals(imageButtonFavorite.getTag())) {
            imageButtonFavorite.setImageResource(R.drawable.ic_favorite_red);
        } else if ("not_favorite".equals(imageButtonFavorite.getTag())) {
            imageButtonFavorite.setImageResource(R.drawable.ic_favorite);
        } else {
            imageButtonFavorite.setVisibility(View.GONE);
        }

        imageButtonFavorite.setOnClickListener(v -> {
            if ("favorite".equals(imageButtonFavorite.getTag())) {
                removeFromFavorite(imageButtonFavorite, animal.getId());
            } else {
                addToFavorite(imageButtonFavorite, animal.getId());
            }
        });

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AnimalProfileActivity.class);
            intent.putExtra("animal", animal.getId());
            intent.putExtra("favorite", imageButtonFavorite.getTag().toString());
            startActivity(intent);
        });

        horizontalLinearLayout.addView(itemView);
    }

    private void addToFavorite(ImageButton imageButtonFavorite, String documentId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid();
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.update("favorites", FieldValue.arrayUnion(db.collection("Animals").document(documentId)))
                .addOnSuccessListener(aVoid -> {
                    imageButtonFavorite.setImageResource(R.drawable.ic_favorite_red);
                    imageButtonFavorite.setTag("favorite");
                    Log.d("Firestore", "Adăugat la favorite");
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Eroare la adăugarea la favorite", e);
                    Toast.makeText(this, "Eroare", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFromFavorite(ImageButton imageButtonFavorite, String documentId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid();
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.update("favorites", FieldValue.arrayRemove(db.collection("Animals").document(documentId)))
                .addOnSuccessListener(aVoid -> {
                    imageButtonFavorite.setImageResource(R.drawable.ic_favorite);
                    imageButtonFavorite.setTag("not_favorite");
                    Log.d("Firestore", "Eliminat din favorite");
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Eroare la eliminarea din favorite", e);
                    Toast.makeText(this, "Eroare", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUserRole(FirebaseUser user, ImageButton imageButtonFavorite) {
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("role")) {
                        String role = documentSnapshot.getString("role");
                        if ("user".equals(role)) {
                            imageButtonFavorite.setVisibility(VISIBLE);
                        }
                    }
                });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            } else {
                Log.d("Notificări", "Permisiunea deja acordată!");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Notificări", "Permisiune acordată!");
            } else {
                Toast.makeText(this, "Permisiunea pentru notificări a fost refuzată!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadChats() {
        unreadMessagesListener = db.collection("users")
                .document(user.getUid())
                .collection("chats")
                .whereEqualTo("hasUnreadMessages", true)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Eroare la ascultarea mesajelor necitite", error);
                        Toast.makeText(this, "Eroare la preluarea conversațiilor!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean hasUnreadMessages = snapshot != null && !snapshot.isEmpty();
                    Log.d(TAG, hasUnreadMessages ? "Mesaje necitite detectate" : "Nu există mesaje necitite");

                    // Salvează starea în SharedPreferences
                    sharedPreferences.edit()
                            .putBoolean(KEY_HAS_UNREAD_MESSAGES, hasUnreadMessages)
                            .apply();

                    // Actualizează iconița
                    updateMessagesIcon(hasUnreadMessages);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
        }
    }

    private void updateMessagesIcon(boolean hasUnreadMessages) {
        int iconResId = hasUnreadMessages ? R.drawable.ic_message_red : R.drawable.ic_message;
        bottomNavigationView.getMenu().findItem(R.id.navigation_messages).setIcon(iconResId);
    }
}