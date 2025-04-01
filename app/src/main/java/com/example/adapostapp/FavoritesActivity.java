package com.example.adapostapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

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
    private GridLayout gridLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView noneFavoriteTextView;
    private ScrollView scrollView;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        gridLayout = findViewById(R.id.GridLayout);
        progressBar = findViewById(R.id.progressBar);
        noneFavoriteTextView = findViewById(R.id.textViewEmpty);
        scrollView = findViewById(R.id.ScrollView);

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        bottomNavigationView.setSelectedItemId(R.id.navigation_favorites);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(FavoritesActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(FavoritesActivity.this, ChatListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_animals) {
                startActivity(new Intent(FavoritesActivity.this, ListAnimalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(FavoritesActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user == null){
            Toast.makeText(this, "Autentifica-te pentru a vedea favoritele!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        gridLayout.removeAllViews();
        fetchAnimals();
    }


    @SuppressLint("SetTextI18n")
    private void fetchAnimals() {
        gridLayout.removeAllViews();
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            noneFavoriteTextView.setVisibility(View.VISIBLE); // Afișează mesajul dacă nu este autentificat
            progressBar.setVisibility(View.GONE);
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists() || !documentSnapshot.contains("favorites")) {
                        Log.d("Firestore", "Utilizatorul nu are câmpul 'favorites'.");
                        noneFavoriteTextView.setVisibility(View.VISIBLE); // Afișează mesajul dacă nu sunt favorite
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    List<DocumentReference> favorites = (List<DocumentReference>) documentSnapshot.get("favorites");

                    if (favorites == null || favorites.isEmpty()) {
                        Log.d("Firestore", "Lista de favorite este goală.");
                        scrollView.setVisibility(View.GONE);
                        noneFavoriteTextView.setVisibility(View.VISIBLE); // Afișează mesajul dacă lista este goală
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    List<String> favoriteAnimalIds = new ArrayList<>();
                    for (DocumentReference ref : favorites) {
                        favoriteAnimalIds.add(ref.getId());
                    }

                    Query query = db.collection("Animals");
                    query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                        boolean hasFavorites = false;
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Animal animal = document.toObject(Animal.class);
                            animal.setId(document.getId());

                            if (favoriteAnimalIds.contains(animal.getId())) {
                                addAnimalCardToUI(animal);
                                hasFavorites = true;
                            }
                        }

                        // Dacă nu au fost favorite, arată mesajul
                        if (!hasFavorites) {
                            noneFavoriteTextView.setVisibility(View.VISIBLE);
                        }

                        progressBar.setVisibility(View.GONE);
                    }).addOnFailureListener(e -> {
                        Log.w("Firebase", "Error getting documents.", e);
                        Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la preluarea listei de favorite: ", e);
                    noneFavoriteTextView.setVisibility(View.VISIBLE); // Afișează mesajul în caz de eroare
                    progressBar.setVisibility(View.GONE);
                });
    }




    private void addAnimalCardToUI(Animal animal) {
        // Crează cardul pentru animal
        View itemView = LayoutInflater.from(this).inflate(R.layout.card_item, gridLayout, false);

        // Obține referințele la elementele din card
        ImageView animalPhoto = itemView.findViewById(R.id.imageItemImageView);
        ImageView imageGen = itemView.findViewById(R.id.imageGen);
        TextView animalName = itemView.findViewById(R.id.textViewName);
        TextView animalBreed = itemView.findViewById(R.id.textViewBreed);
        TextView animalAge = itemView.findViewById(R.id.textViewAge);
        ImageButton imageButtonFavoriteDel = itemView.findViewById(R.id.imageButtonFavorite);

        // Populează datele animalului
        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalAge.setText(animal.getYears() + (animal.getYears() == 1 ? " an" : " ani"));
        imageGen.setImageResource(animal.getGen().equals("Mascul") ? R.drawable.ic_male : R.drawable.ic_female);
        imageButtonFavoriteDel.setImageResource(R.drawable.ic_x);

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
        }

        // Butonul de eliminare din favorite
        imageButtonFavoriteDel.setOnClickListener(v -> {
            showDeleteConfirmationDialog(animal.getId(), itemView);
        });

        // Setează lățimea cardului pentru a ocupa jumătate din ecran
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED); // Lasă rândul să fie dinamic
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED); // Lasă coloana să fie dinamică
        layoutParams.setMargins(0, 0, 32, 32);  // Adaugă margini între carduri
        itemView.setLayoutParams(layoutParams);

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(FavoritesActivity.this, AnimalProfileActivity.class);
            intent.putExtra("animal", animal.getId());
            intent.putExtra("favorite", "favorite");
            startActivity(intent);
        });

        // Adaugă cardul la GridLayout
        gridLayout.addView(itemView);
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
                    Toast.makeText(this, "Animal sters din favorite!", Toast.LENGTH_SHORT).show();
                    fetchAnimals();
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
                })
                .setNegativeButton("Anulează", (dialog, which) -> {
                    // Închide dialogul fără nicio acțiune
                    dialog.dismiss();
                })
                .show();
    }
}