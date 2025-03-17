package com.example.adapostapp;

import android.content.Intent;
import android.net.Uri;
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

import androidx.annotation.Nullable;
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
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ListAnimalActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private ImageButton buttonBackToMain;
    private GridLayout gridLayout;
    private LinearLayout linearLayout;
    private FirebaseFirestore db;
    private TextView noneFavoriteTextView;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_animal);

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        gridLayout = findViewById(R.id.GridLayout);
        linearLayout = findViewById(R.id.linearLayout);
        noneFavoriteTextView = findViewById(R.id.noneFavoriteTextView);
        progressBar = findViewById(R.id.progressBar);

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        bottomNavigationView.setSelectedItemId(R.id.navigation_animals);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(ListAnimalActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(ListAnimalActivity.this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(ListAnimalActivity.this, MessagesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_animals) {
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(ListAnimalActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gridLayout.removeAllViews();
        linearLayout.removeAllViews();
        readAnimalsFromDB();
    }

    private void readAnimalsFromDB() {
        progressBar.setVisibility(View.VISIBLE);

        // Query pentru animale
        db.collection("Animals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("Firebase", "Nu au fost găsite animale.");
                        noneFavoriteTextView.setVisibility(View.VISIBLE);
                    } else {
                        // Dacă utilizatorul este logat, verificăm rolul
                        if (user != null) {
                            db.collection("users")
                                    .document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        String role = documentSnapshot.exists() ? documentSnapshot.getString("role") : "user";
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            Animal animal = document.toObject(Animal.class);
                                            if ("admin".equals(role)) {
                                                addCardToUI(animal);
                                            } else {
                                                addAnimalCardToUI(animal);
                                            }
                                        }
                                        progressBar.setVisibility(View.GONE);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w("Firebase", "Eroare la obținerea rolului utilizatorului.", e);
                                        progressBar.setVisibility(View.GONE);
                                    });
                        } else {
                            // Dacă nu este logat, afișăm animalele ca utilizator obișnuit
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Animal animal = document.toObject(Animal.class);
                                addAnimalCardToUI(animal);
                            }
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("Firebase", "Error getting documents.", e);
                    Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }



    private void addAnimalCardToUI(Animal animal) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.card_item, gridLayout, false);
        itemView.findViewById(R.id.imageButtonFavorite).setVisibility(View.GONE);
        ImageView imageGen = itemView.findViewById(R.id.imageGen);
        imageGen.setImageResource(animal.getGen().equals("Mascul") ? R.drawable.ic_male : R.drawable.ic_female);

        // Obține referințele la elementele din card
        ImageView animalPhoto = itemView.findViewById(R.id.imageItemImageView);
        TextView animalName = itemView.findViewById(R.id.textViewName);
        TextView animalBreed = itemView.findViewById(R.id.textViewBreed);
        TextView animalAge = itemView.findViewById(R.id.textViewAge);

        // Populează datele animalului
        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalAge.setText(animal.getAge() + (animal.getAge() == 1 ? " an" : " ani"));

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
        }

        // Setează lățimea cardului pentru a ocupa jumătate din ecran
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED); // Lasă rândul să fie dinamic
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED); // Lasă coloana să fie dinamică
        layoutParams.setMargins(0, 0, 32, 32);  // Adaugă margini între carduri
        itemView.setLayoutParams(layoutParams);

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimalProfileActivity.class);
            intent.putExtra("animal", animal.getId());
//            intent.putExtra("favorite", "favorite");
            startActivity(intent);
        });

        // Adaugă cardul la GridLayout
        gridLayout.addView(itemView);
    }

    private void addCardToUI(Animal animal) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.card_item_edit, linearLayout, false);

        // Obține referințele la elementele din card
        ImageView animalPhoto = itemView.findViewById(R.id.imageItemImageView);
        TextView animalName = itemView.findViewById(R.id.textViewName);
        TextView animalBreed = itemView.findViewById(R.id.textViewBreed);
        TextView animalAge = itemView.findViewById(R.id.textViewAge);
        ImageButton imageEdit = itemView.findViewById(R.id.imageEdit);
        ImageButton imageButtonDelete = itemView.findViewById(R.id.imageButtonDelete);

        // Populează datele animalului
        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalAge.setText(animal.getAge() + (animal.getAge() == 1 ? " an" : " ani"));

        if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
        }
        imageEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditAnimalActivity.class);
            intent.putExtra("animal", animal.getId());
            startActivity(intent);
        });
        imageButtonDelete.setOnClickListener(v -> {showDeleteConfirmationDialog(animal.getId());});

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimalProfileActivity.class);
            intent.putExtra("animal", animal.getId());
            intent.putExtra("favorite", "favorite");
            startActivity(intent);
        });

        // Adaugă cardul la GridLayout
        linearLayout.addView(itemView);
    }

    private void showDeleteConfirmationDialog(String Id) {
        // Creează un dialog de confirmare
        new AlertDialog.Builder(this)
                .setTitle("Ștergere animal din baza de date")
                .setMessage("Ești sigur că vrei să ștergi acest animal din baza de date?")
                .setPositiveButton("Da", (dialog, which) -> {
                    deleteAnimalAndImage(Id);
                })
                .setNegativeButton("Anulează", (dialog, which) -> {
                    // Închide dialogul fără nicio acțiune
                    dialog.dismiss();
                })
                .show();
    }

    private void deleteAnimalAndImage(String animalId) {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Obținem documentul animalului din Firestore pentru a lua URL-ul imaginii
        db.collection("Animals")
                .document(animalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("photo");
                        Log.d("Firebase", "URL-ul imaginii: " + imageUrl); // Afișează URL-ul imaginii în Logcat")
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // Extragem numele fișierului din URL
                            Uri uri = Uri.parse(imageUrl);
                            String fileName = uri.getLastPathSegment(); // Obținem doar numele fișierului

                            // Creăm referința către imagine în Firebase Storage
                            StorageReference imageRef = storage.getReference().child(fileName);

                            // Ștergem imaginea
                            imageRef.delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firebase", "Imaginea a fost ștearsă cu succes.");
                                        removeAnimalFromFavorites(animalId); // După ștergerea imaginii, eliminăm animalul din Firestore și favorite
                                    })
                                    .addOnFailureListener(e -> Log.w("Firebase", "Eroare la ștergerea imaginii.", e));
                        } else {
                            removeAnimalFromFavorites(animalId); // Dacă nu există imagine, ștergem doar animalul
                        }
                    } else {
                        Log.d("Firebase", "Animalul nu există în Firestore.");
                    }
                })
                .addOnFailureListener(e -> Log.w("Firebase", "Eroare la obținerea datelor animalului.", e));
    }

    private void removeAnimalFromFavorites(String animalId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference animalRef = db.collection("Animals").document(animalId);

        db.collection("users")
                .whereArrayContains("favorites", animalRef)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DocumentReference userRef = document.getReference();
                        batch.update(userRef, "favorites", FieldValue.arrayRemove(animalRef));
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firebase", "Animalul a fost eliminat din favorite.");
                                deleteAnimalFromFirestore(animalId); // După ce eliminăm din favorite, ștergem documentul
                            })
                            .addOnFailureListener(e -> Log.w("Firebase", "Eroare la actualizarea listei de favorite.", e));
                })
                .addOnFailureListener(e -> Log.w("Firebase", "Eroare la căutarea utilizatorilor.", e));
    }

    private void deleteAnimalFromFirestore(String animalId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Animals").document(animalId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    readAnimalsFromDB();
                    Log.d("Firebase", "Animalul a fost șters din Firestore.");
                })
                .addOnFailureListener(e -> Log.w("Firebase", "Eroare la ștergerea animalului.", e));
    }

}
