package com.example.adapostapp;

import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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

        notificationButton.setOnClickListener(v -> {
            // TODO: Navigare către pagina de notificări
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(MainActivity.this, MessagesActivity.class));
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        horizontalLinearLayout.removeAllViews();
        fetchAnimals();
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
        Query query = db.collection("Animals");

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if(queryDocumentSnapshots.isEmpty()){
                Log.d("Firebase", "Nu au fost găsite animale.");
                progressBar.setVisibility(View.GONE);
                noneAnimalTextView.setVisibility(View.VISIBLE);
                return;
            }

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Animal animal = document.toObject(Animal.class);
                addAnimalCardToUI(animal, "");
            }
            progressBar.setVisibility(View.GONE);
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
            progressBar.setVisibility(View.GONE);
        });
    }

    private void readAnimalsFromDB(List<String> favoriteAnimalIds) {
        Query query = db.collection("Animals");

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if(queryDocumentSnapshots.isEmpty()){
                Log.d("Firebase", "Nu au fost găsite animale.");
                progressBar.setVisibility(View.GONE);
                noneAnimalTextView.setVisibility(View.VISIBLE);
                return;
            }

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Animal animal = document.toObject(Animal.class);
                animal.setId(document.getId());
                String favoriteTag = favoriteAnimalIds.contains(document.getId()) ? "favorite" : "not_favorite";
                addAnimalCardToUI(animal, favoriteTag);
            }
            progressBar.setVisibility(View.GONE);
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
            progressBar.setVisibility(View.GONE);
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

        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalAge.setText(animal.getAge() + (animal.getAge() == 1 ? " an" : " ani"));
        imageGen.setImageResource(animal.getGen().equals("Mascul") ? R.drawable.ic_male : R.drawable.ic_female);

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(this)
                    .load(animal.getPhoto())
                    .error(R.drawable.ic_launcher_foreground)
                    .into(animalPhoto);
        }

        imageButtonFavorite.setTag(favoriteTag);
        if ("favorite".equals(imageButtonFavorite.getTag())) {
            imageButtonFavorite.setImageResource(R.drawable.ic_favorite_red);
        }
        else if("not_favorite".equals(imageButtonFavorite.getTag())) {
            imageButtonFavorite.setImageResource(R.drawable.ic_favorite);
        }
        else {
            imageButtonFavorite.setVisibility(View.GONE);
        }

        imageButtonFavorite.setOnClickListener(v -> {
            if ("favorite".equals(imageButtonFavorite.getTag())) {
                removeFromFavorite(imageButtonFavorite, animal.getId(), animal);
            }
            else {
                addToFavorite(imageButtonFavorite, animal.getId(), animal);
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

    private void addToFavorite(ImageButton imageButtonFavorite, String documentId, Animal animal) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users").document(uid)
                .update("favorites", FieldValue.arrayUnion(db.collection("Animals").document(documentId)))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Animal adăugat la favorite!");
                    Toast.makeText(MainActivity.this, "Animal adăugat în favorite!", Toast.LENGTH_SHORT).show();
                    imageButtonFavorite.setImageResource(R.drawable.ic_favorite_red);
                    imageButtonFavorite.setTag("favorite");
//                    animal.setFavorite(true);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Eroare la adăugarea în favorite: ", e));
    }

    private void removeFromFavorite(ImageButton imageButtonFavorite, String documentId, Animal animal) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users").document(uid)
                .update("favorites", FieldValue.arrayRemove(db.collection("Animals").document(documentId)))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Animal eliminat din favorite!");
                    Toast.makeText(MainActivity.this, "Animal eliminat din favorite!", Toast.LENGTH_SHORT).show();
                    imageButtonFavorite.setImageResource(R.drawable.ic_favorite);
                    imageButtonFavorite.setTag("not_favorite");
//                    animal.setFavorite(false);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Eroare la eliminarea din favorite: ", e));
    }

}
