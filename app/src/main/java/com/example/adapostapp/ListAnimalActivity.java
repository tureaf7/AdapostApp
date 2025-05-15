package com.example.adapostapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ListAnimalActivity extends BaseActivity implements AnimalsAdapter.OnAdminActionListener {
    private ImageButton buttonBackToMain;
    private RecyclerView recyclerViewAnimals;
    private TextView noneFavoriteTextView;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private AnimalsAdapter adapter;
    private FloatingActionButton fabAddAnimal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_animal);

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        recyclerViewAnimals = findViewById(R.id.recyclerViewAnimals);
        noneFavoriteTextView = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);
        fabAddAnimal = findViewById(R.id.fab_add_animal);

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        adapter = new AnimalsAdapter(this, isAdmin(), this);
        recyclerViewAnimals.setAdapter(adapter);
        if (isAdmin()) {
            fabAddAnimal.setVisibility(View.VISIBLE);
            fabAddAnimal.setOnClickListener(v -> startActivity(new Intent(this, AddAnimalActivity.class)));
            recyclerViewAnimals.setLayoutManager(new LinearLayoutManager(this)); // Listă verticală pentru admin
        } else {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            recyclerViewAnimals.setLayoutManager(gridLayoutManager);
            recyclerViewAnimals.setPadding(0, 0, 0, 0); // Padding pentru margini simetrice
        }

        setupBottomNavigation(R.id.navigation_animals);
        readAnimalsFromDB(getUserRole());
    }

    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_animals;
    }

    private void readAnimalsFromDB(String role) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Animals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Object> items = new ArrayList<>();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("Firebase", "Nu au fost găsite animale.");
                        noneFavoriteTextView.setVisibility(View.VISIBLE);
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Animal animal = document.toObject(Animal.class);
                            animal.setId(document.getId());
                            if ("admin".equals(role)) {
                                items.add(animal);
                            } else {
                                if (!animal.isAdopted()) {
                                    items.add(animal);
                                }
                            }
                        }
                        adapter.setItems(items);
                        noneFavoriteTextView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("Firebase", "Eroare la preluarea datelor", e);
                    Toast.makeText(ListAnimalActivity.this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
                    noneFavoriteTextView.setVisibility(View.VISIBLE);
                    adapter.setItems(new ArrayList<>());
                    progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public void onEditClicked(String animalId) {
        Intent intent = new Intent(this, EditAnimalActivity.class);
        intent.putExtra("animal", animalId);
        startActivity(intent);
    }

    @Override
    public void onDeleteClicked(String animalId) {
        showDeleteConfirmationDialog(animalId);
    }

    private void showDeleteConfirmationDialog(String animalId) {
        new AlertDialog.Builder(this)
                .setTitle("Ștergere animal din baza de date")
                .setMessage("Ești sigur că vrei să ștergi acest animal din baza de date?")
                .setPositiveButton("Da", (dialog, which) -> deleteAnimalAndImage(animalId))
                .setNegativeButton("Anulează", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteAnimalAndImage(String animalId) {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        db.collection("Animals")
                .document(animalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("photo");
                        Log.d("Firebase", "URL-ul imaginii: " + imageUrl);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Uri uri = Uri.parse(imageUrl);
                            String fileName = uri.getLastPathSegment();
                            StorageReference imageRef = storage.getReference().child(fileName);

                            imageRef.delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firebase", "Imaginea a fost ștearsă cu succes.");
                                        removeAnimalFromFavorites(animalId);
                                    })
                                    .addOnFailureListener(e -> Log.w("Firebase", "Eroare la ștergerea imaginii.", e));
                        } else {
                            removeAnimalFromFavorites(animalId);
                        }
                    } else {
                        Log.d("Firebase", "Animalul nu există în Firestore.");
                    }
                })
                .addOnFailureListener(e -> Log.w("Firebase", "Eroare la obținerea datelor animalului.", e));
    }

    private void removeAnimalFromFavorites(String animalId) {
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
                                deleteAnimalAndApplications(animalId);
                            })
                            .addOnFailureListener(e -> Log.w("Firebase", "Eroare la actualizarea listei de favorite.", e));
                })
                .addOnFailureListener(e -> Log.w("Firebase", "Eroare la căutarea utilizatorilor.", e));
    }

    private void deleteAnimalAndApplications(String animalId) {
        WriteBatch batch = db.batch();
        DocumentReference animalRef = db.collection("Animals").document(animalId);
        batch.delete(animalRef);

        db.collection("AdoptionApplications")
                .whereEqualTo("animalId", animalId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DocumentReference applicationRef = document.getReference();
                        batch.delete(applicationRef);
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                readAnimalsFromDB("admin"); // Actualizăm UI-ul doar dacă rolul este "admin"
                                Log.d("Firebase", "Animalul și cererile de adopție au fost șterse din Firestore.");
                            })
                            .addOnFailureListener(e -> Log.w("Firebase", "Eroare la ștergerea datelor.", e));
                })
                .addOnFailureListener(e -> Log.w("Firebase", "Eroare la preluarea cererilor de adopție.", e));
    }

    @Override
    protected void onResume() {
        super.onResume();
        readAnimalsFromDB(getUserRole()); // Reîmprospătăm lista la revenire
    }
}