package com.example.adapostapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
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
    private SearchView searchViewAnimals;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private AnimalsAdapter adapter;
    private List<String> favoriteAnimalIds = new ArrayList<>();
    private List<Animal> allAnimals = new ArrayList<>();
    private boolean hasStartedTyping = false; // Variabilă pentru a urmări dacă utilizatorul a început să tasteze

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_animal);

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        recyclerViewAnimals = findViewById(R.id.recyclerViewAnimals);
        noneFavoriteTextView = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);
        searchViewAnimals = findViewById(R.id.searchViewAnimals);
        FloatingActionButton fabAddAnimal = findViewById(R.id.fab_add_animal);

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user != null && !isAdmin()) {
            readFavoritesFromDB();
        }

        adapter = new AnimalsAdapter(this, isAdmin(), this, favoriteAnimalIds);
        recyclerViewAnimals.setAdapter(adapter);
        if (isAdmin()) {
            fabAddAnimal.setVisibility(View.VISIBLE);
            fabAddAnimal.setOnClickListener(v -> startActivity(new Intent(this, AddAnimalActivity.class)));
            recyclerViewAnimals.setLayoutManager(new LinearLayoutManager(this));
        } else {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            recyclerViewAnimals.setLayoutManager(gridLayoutManager);
            recyclerViewAnimals.setPadding(0, 0, 0, 0);
        }

        AutoCompleteTextView searchAutoComplete = searchViewAnimals.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchAutoComplete != null) {
            searchAutoComplete.setTextColor(getResources().getColor(android.R.color.black)); // Setează textul la negru
        }

        // Setăm listener-ul pentru SearchView
        searchViewAnimals.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText != null && !newText.isEmpty()) {
                    hasStartedTyping = true; // Utilizatorul a început să tasteze
                    filterAnimals(newText);
                } else if (hasStartedTyping) {
                    // Dacă utilizatorul a șters tot textul, reaplicăm filtrul pentru a afișa lista inițială
                    filterAnimals(newText);
                }
                return true;
            }
        });

//        setupBottomNavigation(R.id.navigation_animals);
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
                    allAnimals.clear();
                    List<Object> items = new ArrayList<>();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("Firebase", "Nu au fost găsite animale.");
                        noneFavoriteTextView.setVisibility(View.VISIBLE);
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Animal animal = document.toObject(Animal.class);
                            animal.setId(document.getId());
                            allAnimals.add(animal);
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

    private void filterAnimals(String query) {
        List<Object> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            // Afișăm lista inițială doar dacă utilizatorul a început să tasteze și apoi a șters textul
            for (Animal animal : allAnimals) {
                if ("admin".equals(getUserRole()) || !animal.isAdopted()) {
                    filteredList.add(animal);
                }
            }
        } else {
            // Filtrăm după nume
            String filterPattern = query.toLowerCase().trim();
            for (Animal animal : allAnimals) {
                if (animal.getName() != null && animal.getName().toLowerCase().contains(filterPattern)) {
                    if ("admin".equals(getUserRole()) || !animal.isAdopted()) {
                        filteredList.add(animal);
                    }
                }
            }
        }
        adapter.setItems(filteredList);
        noneFavoriteTextView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void readFavoritesFromDB() {
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    favoriteAnimalIds.clear();
                    if (documentSnapshot.exists() && documentSnapshot.contains("favorites")) {
                        List<DocumentReference> favorites = (List<DocumentReference>) documentSnapshot.get("favorites");
                        if (favorites != null) {
                            for (DocumentReference ref : favorites) {
                                favoriteAnimalIds.add(ref.getId());
                            }
                        }
                    }
                    Log.d("ListAnimalActivity", "Favorite IDs: " + favoriteAnimalIds.toString());
                    adapter.updateFavorites(favoriteAnimalIds);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Eroare la preluarea listei de favorite: ", e));
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

    @Override
    public void onFavoriteClicked(String animalId) {
        toggleFavorite(animalId);
    }

    private void toggleFavorite(String animalId) {
        if (user == null) {
            Toast.makeText(this, "Trebuie să fii autentificat pentru a adăuga la favorite!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (favoriteAnimalIds.contains(animalId)) {
            removeFromFavorites(animalId);
        } else {
            addToFavorites(animalId);
        }
    }

    private void addToFavorites(String animalId) {
        db.collection("users").document(user.getUid())
                .update("favorites", FieldValue.arrayUnion(db.collection("Animals").document(animalId)))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Animal adăugat în favorite!");
                    Toast.makeText(ListAnimalActivity.this, "Animal adăugat în favorite!", Toast.LENGTH_SHORT).show();
                    favoriteAnimalIds.add(animalId);
                    adapter.updateFavorites(favoriteAnimalIds);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Eroare la actualizarea favorite: ", e));
    }

    private void removeFromFavorites(String animalId) {
        db.collection("users").document(user.getUid())
                .update("favorites", FieldValue.arrayRemove(db.collection("Animals").document(animalId)))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Animal șters din favorite!");
                    Toast.makeText(ListAnimalActivity.this, "Animal șters din favorite!", Toast.LENGTH_SHORT).show();
                    favoriteAnimalIds.remove(animalId);
                    adapter.updateFavorites(favoriteAnimalIds);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Eroare la actualizarea favorite: ", e));
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
                                readAnimalsFromDB("admin");
                                Log.d("Firebase", "Animalul și cererile de adopție au fost șterse din Firestore.");
                            })
                            .addOnFailureListener(e -> Log.w("Firebase", "Eroare la ștergerea datelor.", e));
                })
                .addOnFailureListener(e -> Log.w("Firebase", "Eroare la preluarea cererilor de adopție.", e));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user != null && !isAdmin()) {
            readFavoritesFromDB();
        }
        readAnimalsFromDB(getUserRole());
        setupBottomNavigation(R.id.navigation_animals);
        Log.d("ListAnimalActivity", "onResume called");
    }
}