package com.example.adapostapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.Console;
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
                fetchAnimals(""); // Home - reload the main screen
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

        Query query = db.collection("Animals");

        if (!species.isEmpty()) {
            query = query.whereEqualTo("species", species);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Animal> animals = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Animal animal = document.toObject(Animal.class);
                animals.add(animal);
            }

            for (Animal animal : animals) {
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
                imageGen.setImageResource(animal.getGen().equals("Male") ? R.drawable.ic_male : R.drawable.ic_female);

                if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
                    Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
                }

                imageButtonFavorite.setOnClickListener(v -> {
                    addToFavorite();
                });

                itemView.setOnClickListener(v -> {
                    Toast.makeText(MainActivity.this, "Nume animal: " + animal.getName(), Toast.LENGTH_SHORT).show();
                });

                horizontalLinearLayout.addView(itemView);
            }
        }).addOnFailureListener(e -> {
            Log.w("Firebase", "Error getting documents.", e);
            Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
        });
    }

    private void addToFavorite(){
        Toast.makeText(MainActivity.this, "Adăugat în favorite", Toast.LENGTH_SHORT).show();
    }

}