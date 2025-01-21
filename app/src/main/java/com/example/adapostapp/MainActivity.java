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
    private Button dogsButton, catsButton, allButton;
    private ImageButton profileButton;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        horizontalLinearLayout = findViewById(R.id.horizontalLinearLayout);
        dogsButton = findViewById(R.id.dogsButton);
        catsButton = findViewById(R.id.catsButton);
        profileButton = findViewById(R.id.profileButton);
        allButton = findViewById(R.id.allButton);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        db = FirebaseFirestore.getInstance();

        dogsButton.setOnClickListener(v -> fetchAnimals("Câine"));
        catsButton.setOnClickListener(v -> fetchAnimals("Pisică"));
        allButton.setOnClickListener(v -> fetchAnimals(""));

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                fetchAnimals("");
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(MainActivity.this, MessagesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // Preia toate animalele la lansare
        fetchAnimals("");


        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(currentUser.getUid())  // Folosește UID-ul utilizatorului pentru a găsi documentul corect
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {

                            String urlImage = documentSnapshot.getString("profileImage");

                            Glide.with(this)
                                    .load(urlImage)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .into(profileButton);
                        }
                    });
        }
    }

    @SuppressLint("SetTextI18n")
    private void fetchAnimals(String species) {
        horizontalLinearLayout.removeAllViews(); // Curăță lista înainte de a adăuga altele noi

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            readAnimalsFromDB(species);
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

                            // Adaugă animalul în UI
                            addAnimalCardToUI(animal, favoriteAnimalIds.contains(animal.getDocId()), true);
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

    private void readAnimalsFromDB(String species){
        Query query = db.collection("Animals");
        if (!species.isEmpty()) {
            query = query.whereEqualTo("species", species);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Animal animal = document.toObject(Animal.class);
                animal.setDocId(document.getId());

                // Adaugă animalul în UI
                addAnimalCardToUI(animal, false, false);
            }
        }).addOnFailureListener(e -> {
            Log.w("Firebase", "Error getting documents.", e);
            Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
        });
    }


    private void addToFavorite(ImageButton imageButtonFavorite, String documentId) {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid(); // Obține UID-ul utilizatorului
//        FirebaseFirestore db = FirebaseFirestore.getInstance();


        // Actualizează array-ul "favorites" al utilizatorului
        db.collection("users").document(uid)
                .update("favorites", FieldValue.arrayUnion(db.collection("Animals").document(documentId))) // Adaugă la array fără duplicare
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Referințele au fost adăugate cu succes în 'favorites'!");
                    Toast.makeText(MainActivity.this, "Animal adăugat în favorite!", Toast.LENGTH_SHORT).show();
                    imageButtonFavorite.setImageResource(R.drawable.ic_favorite_red);
                    imageButtonFavorite.setTag("favorite");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la actualizarea array-ului 'favorites': ", e);
                });
    }

    private void removeFromFavorite(ImageButton imageButtonFavorite, String documentId) {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid(); // Obține UID-ul utilizatorului
//        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Instanță Firestore

        // Actualizează array-ul "favorites" al utilizatorului
        db.collection("users").document(uid)
                .update("favorites", FieldValue.arrayRemove(db.collection("Animals").document(documentId))) // Adaugă la array fără duplicare
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Referințele au fost sterse cu succes în 'favorites'!");
                    Toast.makeText(MainActivity.this, "Animal sters din favorite!", Toast.LENGTH_SHORT).show();
                    imageButtonFavorite.setImageResource(R.drawable.ic_favorite);
                    imageButtonFavorite.setTag("not_favorite");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la actualizarea array-ului 'favorites': ", e);
                });
    }

    private void addAnimalCardToUI(Animal animal, boolean isFavorite, boolean user) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.card_item, horizontalLinearLayout, false);

        // Elementele din layout-ul cardului
        ImageView animalPhoto = itemView.findViewById(R.id.imageItemImageView);
        ImageView imageGen = itemView.findViewById(R.id.imageGen);
        TextView animalName = itemView.findViewById(R.id.textViewName);
        TextView animalBreed = itemView.findViewById(R.id.textViewBreed);
        TextView animalAge = itemView.findViewById(R.id.textViewAge);


        // Populează datele animalului
        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalAge.setText(animal.getAge() + (animal.getAge() == 1 ? " an" : " ani"));
        imageGen.setImageResource(animal.getGen().equals("Male") ? R.drawable.ic_male : R.drawable.ic_female);

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
        }

        if (user) {
            ImageButton imageButtonFavorite = itemView.findViewById(R.id.imageButtonFavorite);
            imageButtonFavorite.setVisibility(VISIBLE);
            // Setează starea butonului de favorite
            if (isFavorite) {
                imageButtonFavorite.setImageResource(R.drawable.ic_favorite_red);
                imageButtonFavorite.setTag("favorite");
            } else {
                imageButtonFavorite.setImageResource(R.drawable.ic_favorite);
                imageButtonFavorite.setTag("not_favorite");
            }


            // Gestionează click-ul pe butonul de favorite
            imageButtonFavorite.setOnClickListener(v -> {
                if ("favorite".equals(imageButtonFavorite.getTag())) {
                    removeFromFavorite(imageButtonFavorite, animal.getDocId());
                } else {
                    addToFavorite(imageButtonFavorite, animal.getDocId());
                }
            });
        }
        itemView.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Nume animal: " + animal.getName(), Toast.LENGTH_SHORT).show();
        });

        // Adaugă cardul în layout-ul orizontal
        horizontalLinearLayout.addView(itemView);
    }


}