package com.example.adapostapp;

import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private LinearLayout horizontalLinearLayout;
    private ImageButton notificationButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView adoptInfoText, donatText, voluntarText, noneAnimalTextView;
    private FirebaseUser user;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final String TAG = "MainActivity";
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verifică dacă utilizatorul este admin și redirecționează
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && isAdmin()) {
            Log.d(TAG, "Utilizatorul este admin, redirecționare către ListAnimalActivity");
            Intent intent = new Intent(this, ListAnimalActivity.class); // Sau o altă activitate pentru admin
            startActivity(intent);
            finish();
            return;
        }

        // Inițializări UI
        horizontalLinearLayout = findViewById(R.id.horizontalLinearLayout);
        notificationButton = findViewById(R.id.notificationButton);
        progressBar = findViewById(R.id.progressBar);
        adoptInfoText = findViewById(R.id.adoptInfoText);
        donatText = findViewById(R.id.donatText);
        voluntarText = findViewById(R.id.voluntarText);
        noneAnimalTextView = findViewById(R.id.noneAnimalstextView);

        setupBottomNavigation(R.id.navigation_home);

        // Inițializări Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        voluntarText.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, VolunteerFormActivity.class)));
        notificationButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NotificationsActivity.class)));

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

        if (user != null) {
            checkNotifications();
        }
        handleNotificationIntent(getIntent());
        checkNotificationPermission();
    }
    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_home;
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
                Log.w(TAG, "Datele notificării sunt incomplete.");
            }
        } else {
            Log.d(TAG, "Intent-ul nu provine dintr-o notificare.");
        }
    }

    @OptIn(markerClass = ExperimentalBadgeUtils.class)
    private void checkNotifications() {
        if (user == null) return;

        notificationListener = db.collection("Notifications")
                .whereEqualTo("viewed", false)
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Eroare la preluarea notificărilor", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Număr de notificări nevizualizate: " + queryDocumentSnapshots.size());
                        BadgeDrawable badge = BadgeDrawable.create(this);
                        badge.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                        badge.setHorizontalOffset(dpToPx(30));
                        badge.setVerticalOffset(dpToPx(30));
                        badge.setBadgeGravity(BadgeDrawable.TOP_END);
                        badge.setBadgeTextColor(getResources().getColor(android.R.color.white));
                        badge.setNumber(queryDocumentSnapshots.size());
                        badge.setVisible(true);
                        BadgeUtils.attachBadgeDrawable(badge, notificationButton);
                    } else {
                        BadgeUtils.detachBadgeDrawable(BadgeDrawable.create(this), notificationButton);
                    }
                });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (user != null && notificationListener == null) {
            checkNotifications();
        }
    }

    @SuppressLint("SetTextI18n")
    private void fetchAnimals() {
        horizontalLinearLayout.removeAllViews();
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d("Firestore", "Utilizatorul nu este autentificat, afișare ca guest.");
            readAnimalsFromDB();
            return;
        }

        String uid = currentUser.getUid();
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
                    addViewMoreButton();
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
                    addViewMoreButton();
                }).addOnFailureListener(e -> {
                    Log.w("Firebase", "Error getting documents.", e);
                    Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
                });
    }

    private void addViewMoreButton() {
        View itemView = LayoutInflater.from(this).inflate(R.layout.view_more, horizontalLinearLayout, false);
        ImageButton imageButton = itemView.findViewById(R.id.imageButton);
        imageButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ListAnimalActivity.class)));
        horizontalLinearLayout.addView(itemView);
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

        if (!isFinishing() && !isDestroyed()) {
            if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
                Glide.with(this)
                        .load(animal.getPhoto())
                        .error(R.drawable.ic_launcher_foreground)
                        .into(animalPhoto);
            }
        } else {
            Log.w(TAG, "Activitatea este distrusă, nu încarcăm imaginea pentru " + animal.getName());
        }

        // Butonul de favorite este vizibil doar pentru utilizatori autentificați cu rol "user"
        if (user != null && "user".equals(getUserRole())) {
            imageButtonFavorite.setVisibility(VISIBLE);
            imageButtonFavorite.setTag(favoriteTag);
            imageButtonFavorite.setImageResource("favorite".equals(favoriteTag) ? R.drawable.ic_favorite_red : R.drawable.ic_favorite);

            imageButtonFavorite.setOnClickListener(v -> {
                if ("favorite".equals(imageButtonFavorite.getTag())) {
                    removeFromFavorite(imageButtonFavorite, animal.getId());
                } else {
                    addToFavorite(imageButtonFavorite, animal.getId());
                }
            });
        }

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AnimalProfileActivity.class);
            intent.putExtra("animal", animal.getId());
            intent.putExtra("favorite", imageButtonFavorite.getTag() != null ? imageButtonFavorite.getTag().toString() : "not_favorite");
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

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
}