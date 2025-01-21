package com.example.adapostapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

public class FavoritesActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ImageButton buttonBackToMain;
    private LinearLayout verticalLinearLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        verticalLinearLayout = findViewById(R.id.VerticalLinearLayout);

        buttonBackToMain.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        bottomNavigationView.setSelectedItemId(R.id.navigation_favorites);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(FavoritesActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(FavoritesActivity.this, MessagesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(FavoritesActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fetchAnimals("");
    }


    @SuppressLint("SetTextI18n")
    private void fetchAnimals(String species) {
        verticalLinearLayout.removeAllViews();

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid();

        // Preia lista de favorite ale utilizatorului curent
        db.collection("users").document(uid)
                .get()
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

                    // Preia animalele și compară cu lista de favorite
                    Query query = db.collection("Animals");
                    if (!species.isEmpty()) {
                        query = query.whereEqualTo("species", species);
                    }

                    query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Animal animal = document.toObject(Animal.class);
                            animal.setDocId(document.getId());

                            // Adaugă animalul în UI dacă este favorit
                            if (favoriteAnimalIds.contains(animal.getDocId())) {
                                addAnimalCardToUI(animal);
                            }
                        }
                    }).addOnFailureListener(e -> {
                        Log.w("Firebase", "Error getting documents.", e);
                        Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la preluarea listei de favorite: ", e);
                });
    }


    private void addAnimalCardToUI(Animal animal) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.card_item_favorite, verticalLinearLayout, false);

        // Elementele din layout-ul cardului
        ImageView animalPhoto = itemView.findViewById(R.id.imageItemImageView);
        ImageView imageGen = itemView.findViewById(R.id.imageGen);
        TextView animalName = itemView.findViewById(R.id.textViewName);
        TextView animalBreed = itemView.findViewById(R.id.textViewBreed);
        TextView animalAge = itemView.findViewById(R.id.textViewAge);
        ImageButton imageButtonFavoriteDel = itemView.findViewById(R.id.imageButtonFavoriteDel);

        // Populează datele animalului
        animalName.setText(animal.getName());
        animalBreed.setText("Rasa: " + animal.getBreed());
        animalAge.setText("Vârsta: " + animal.getAge() + (animal.getAge() == 1 ? " an" : " ani"));
        imageGen.setImageResource(animal.getGen().equals("Male") ? R.drawable.ic_male : R.drawable.ic_female);

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
        }

        imageButtonFavoriteDel.setOnClickListener(v -> {
            showDeleteConfirmationDialog(animal.getDocId(), itemView);
//            verticalLinearLayout.removeView(itemView);
        });

        // Adaugă cardul în layout-ul orizontal
        verticalLinearLayout.addView(itemView);
    }

    private void removeFromFavorite(String documentId) {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid(); // Obține UID-ul utilizatorului

        // Actualizează array-ul "favorites" al utilizatorului
        db.collection("users").document(uid)
                .update("favorites", FieldValue.arrayRemove(db.collection("Animals").document(documentId))) // Adaugă la array fără duplicare
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Referințele au fost sterse cu succes în 'favorites'!");
                    Toast.makeText(FavoritesActivity.this, "Animal sters din favorite!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la actualizarea array-ului 'favorites': ", e);
                });
    }

    private void showDeleteConfirmationDialog(String documentId, View itemView) {
        // Creează un dialog de confirmare
        new AlertDialog.Builder(this)
                .setTitle("Ștergere animal din favorite")
                .setMessage("Ești sigur că vrei să ștergi acest animal din favorite?")
                .setPositiveButton("Da", (dialog, which) -> {
                    removeFromFavorite(documentId);
                    verticalLinearLayout.removeView(itemView);
                })
                .setNegativeButton("Anulează", (dialog, which) -> {
                    // Închide dialogul fără nicio acțiune
                    dialog.dismiss();
                })
                .show();
    }
}