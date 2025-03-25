package com.example.adapostapp;

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
import com.example.adapostapp.utils.UserUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class AnimalProfileActivity extends AppCompatActivity {

    private ImageView animalImage;
    private ImageButton backButton, favoriteButton;
    private TextView animalName, arrivalDate, animalBreed, animalYears, animalMonth, animalColor,
            animalSex, animalDescription, statusApplication;
    private FirebaseAuth auth;
    private LinearLayout animalStatusLayout;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private String animalId;
    private Button adoptButton;
    private Animal animal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_profile);

        // Inițializarea componentelor UI
        animalYears = findViewById(R.id.animalYears);
        animalMonth = findViewById(R.id.animalMonths);
        animalBreed = findViewById(R.id.animalBreed);
        animalColor = findViewById(R.id.animalColor);
        animalDescription = findViewById(R.id.animalDescription);
        animalImage = findViewById(R.id.animalImage);
        animalName = findViewById(R.id.animalName);
        animalSex = findViewById(R.id.animalSex);
        arrivalDate = findViewById(R.id.arrivalDate);
        backButton = findViewById(R.id.backButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        animalStatusLayout = findViewById(R.id.animalStatusLayout);
        adoptButton = findViewById(R.id.adoptButton);
        statusApplication = findViewById(R.id.statusApplication);

        // Inițializare Firestore și FirebaseAuth
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Recuperarea obiectului Animal din Intent
        animalId = getIntent().getStringExtra("animal");

        if (animalId != null) {
            // Popularea profilului animalului cu datele recuperate
            getAnimalDetails(animalId, user);
        } else {
            // Dacă nu a fost găsit niciun animal în Intent, afișează un mesaj sau o eroare
            animalName.setText("Animal necunoscut");
        }

        // Adaugă logica pentru butonul "Back"
        backButton.setOnClickListener(v -> onBackPressed());
    }

    @Override
    protected void onStart() {
        super.onStart();
        isApplicated(animalId);
    }

    private void checkUserRole(FirebaseUser user) {
        Log.d("AnimalProfileActivity", "checkUserRole - Verificare rol pentru utilizatorul " + user.getUid() + " " + user.getDisplayName());
        UserUtils.checkUserRole(user, new UserUtils.UserRoleCallback() {
            @Override
            public void onRoleRetrieved(String role) {
                if ("admin".equals(role)) {
                    favoriteButton.setImageResource(R.drawable.ic_edit);
                    favoriteButton.setOnClickListener(v -> {
                        Intent intent = new Intent(AnimalProfileActivity.this, EditAnimalActivity.class);
                        intent.putExtra("animal", animalId);
                        startActivity(intent);
                    });
                    adoptButton.setVisibility(View.VISIBLE);
                    adoptButton.setText("Vezi cereri");
                    adoptButton.setOnClickListener(v -> {
                        Intent intent = new Intent(AnimalProfileActivity.this, AdoptionApplicationActivity.class);
                        intent.putExtra("animal", animalId);
                        startActivity(intent);
                    });
                } else {
                    isApplicated(animalId);

                    isFavorite(animalId);
                    favoriteButton.setOnClickListener(v -> {
                        if ("favorite".equals(favoriteButton.getTag())) {
                            removeFromFavorite(animalId);
                        } else {
                            addToFavorite(animalId);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("Firestore", "Eroare la preluarea rolului", e);
            }
        });
    }


    private void isApplicated(String animalId) {
            db.collection("AdoptionApplications")
                    .whereEqualTo("animalId", animalId)
                    .whereEqualTo("userId", user.getUid())
                    .get().addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Log.d("Firebase", "Cererea de adopție a fost găsită.");
                            // Procesare succes: documente găsite
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                AdoptionApplication adoptionApplication = document.toObject(AdoptionApplication.class);
                                statusApplication.setText("Cerera ta este: " + adoptionApplication.getStatus());
                                statusApplication.setVisibility(View.VISIBLE);
                                adoptButton.setVisibility(View.GONE);
                            }
                        } else {
                            // Nu au fost găsite documente
                            Log.d("Firebase", "Nu au fost găsite cereri de adopție pentru acest animal și utilizator.");
                            adoptButton.setVisibility(View.VISIBLE);
                            adoptButton.setOnClickListener(v -> {
                                Intent intent = new Intent(AnimalProfileActivity.this, AdoptionActivity.class);
                                intent.putExtra("animal", animalId);
                                startActivity(intent);
                            });
                        }
                    }).addOnFailureListener(e -> {
                        Log.e("Firebase", "Eroare la verificarea cererii", e);
                    });
    }


    private void isFavorite(String animalId) {
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("User", user.getUid());
                    if (documentSnapshot.exists()) {
                        List<?> favoriteAnimalsRaw = (List<?>) documentSnapshot.get("favorites");

                        if (favoriteAnimalsRaw != null) {
                            // Convertim lista de DocumentReference în lista de String-uri (ID-uri)
                            List<String> favoriteAnimals = new ArrayList<>();
                            for (Object obj : favoriteAnimalsRaw) {
                                if (obj instanceof DocumentReference) {
                                    favoriteAnimals.add(((DocumentReference) obj).getId());
                                }
                            }
                            Log.d("Favorite", favoriteAnimals + "");
                            if (favoriteAnimals.contains(animalId)) {
                                Log.d("Firebase", "Animalul este favorit!");
                                favoriteButton.setTag("favorite");
                                favoriteButton.setImageResource(R.drawable.ic_favorite_red);
                            } else {
                                Log.d("Firebase", "Animalul NU este favorit!");
                                favoriteButton.setTag("not_favorite");
                                favoriteButton.setImageResource(R.drawable.ic_favorite);
                            }
                        }
                    }
                }).addOnFailureListener(e -> Log.w("Firebase", "Eroare la verificarea favorite.", e));
    }


    private void statusAnimal(String status) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.animal_status_container, animalStatusLayout, false);
        TextView statusTextView = itemView.findViewById(R.id.statusAnimal);
        statusTextView.setText(status);
        animalStatusLayout.addView(itemView);

    }

    private void getAnimalDetails(String animalId, FirebaseUser user) {
        db.collection("Animals")
                .document(animalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        animal = documentSnapshot.toObject(Animal.class);
                        if (animal != null) {
                            if(animal.isAdopted()){
                                statusApplication.setText("Animal adoptat");
                            }else{
                                if (user == null){
                                    adoptButton.setVisibility(View.VISIBLE);
                                    adoptButton.setOnClickListener(v -> {
                                        startActivity(new Intent(AnimalProfileActivity.this, ProfileActivity.class));
                                    });
                                }else{
                                    checkUserRole(user);
                                }
                            }
                            populateAnimalProfile(animal);
                        }
                    } else {
                        Log.e("Firestore", "Animalul nu există în Firestore.");
                        Toast.makeText(AnimalProfileActivity.this, "Animalul nu a fost găsit.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la obținerea detaliilor animalului: ", e);
                    Toast.makeText(AnimalProfileActivity.this, "Eroare la obținerea detaliilor animalului.", Toast.LENGTH_SHORT).show();
                });
    }


    // Metodă care populază profilul animalului pe UI
    private void populateAnimalProfile(Animal animal) {
        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalYears.setText(animal.getYears() + (animal.getYears() == 1 ? " an" : " ani"));
        animalMonth.setText(animal.getMonths() + (animal.getMonths() == 1 ? " luna" : " luni"));
        animalColor.setText(animal.getColor());
        animalDescription.setText(animal.getDescription());
        animalSex.setText(animal.getGen());

        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
        arrivalDate.setText(dateFormat.format(animal.getArrivalDate().toDate()));

        if (animal.isVaccinated()) {
            statusAnimal("Vaccinat");
        }
        if (animal.isSterilized()) {
            statusAnimal("Sterilizat");
        }

        // Încarcă imaginea animalului folosind Glide
        Glide.with(this).load(animal.getPhoto()).into(animalImage);

        if (animal.isAdopted()){
            adoptButton.setVisibility(View.GONE);
        }

    }

    // Adăugăm animalul la favorite
    private void addToFavorite(String documentId) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid(); // Obține UID-ul utilizatorului

        // Actualizează array-ul "favorites" al utilizatorului
        db.collection("users").document(uid)
                .update("favorites", FieldValue.arrayUnion(db.collection("Animals").document(documentId))) // Adaugă la array fără duplicare
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Referințele au fost adăugate cu succes în 'favorites'!");
                    Toast.makeText(AnimalProfileActivity.this, "Animal adăugat în favorite!", Toast.LENGTH_SHORT).show();
                    favoriteButton.setImageResource(R.drawable.ic_favorite_red);
//                    animal.setFavorite(true);
                    favoriteButton.setTag("favorite");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la actualizarea array-ului 'favorites': ", e);
                });
    }

    // Eliminăm animalul din favorite
    private void removeFromFavorite(String documentId) {
        FirebaseUser currentUser = auth.getCurrentUser();

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
                    Toast.makeText(AnimalProfileActivity.this, "Animal sters din favorite!", Toast.LENGTH_SHORT).show();
                    favoriteButton.setImageResource(R.drawable.ic_favorite);
//                    animal.setFavorite(false);
                    favoriteButton.setTag("not_favorite");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la actualizarea array-ului 'favorites': ", e);
                });
    }

}
