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
import com.example.adapostapp.utils.UserUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ListAnimalActivity extends BaseActivity {
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
        gridLayout = findViewById(R.id.GridLayout);
        linearLayout = findViewById(R.id.linearLayout);
        noneFavoriteTextView = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        setupBottomNavigation(R.id.navigation_animals);
        readAnimalsFromDB(getUserRole());
    }

    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_animals;
    }

    private void readAnimalsFromDB(String role) {
        db.collection("Animals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("Firebase", "Nu au fost găsite animale.");
                        noneFavoriteTextView.setVisibility(View.VISIBLE);
                    } else {
                        gridLayout.removeAllViews();
                        linearLayout.removeAllViews();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Animal animal = document.toObject(Animal.class);
                            if ("admin".equals(role)) {
                                addCardToUIAdmin(animal);
                            } else {
                                if (!animal.isAdopted()) {
                                    addAnimalCardToUI(animal);
                                }
                            }
                        }
                    }
                    // Adăugăm butonul doar dacă utilizatorul este admin
                    if ("admin".equals(role)) {
                        View itemView = LayoutInflater.from(ListAnimalActivity.this)
                                .inflate(R.layout.add_animal, linearLayout, false);

                        itemView.findViewById(R.id.imageButton).setOnClickListener(v -> {
                            Intent intent = new Intent(ListAnimalActivity.this, AddAnimalActivity.class);
                            startActivity(intent);
                        });
                        Log.d("Firebase", "Utilizatorul este admin. Se afișează butonul de adăugare.");
                        linearLayout.addView(itemView);
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("Firebase", "Eroare la preluarea datelor", e);
                    Toast.makeText(ListAnimalActivity.this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
                    noneFavoriteTextView.setVisibility(View.VISIBLE);
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
        animalAge.setText(animal.getYears() + (animal.getYears() == 1 ? " an" : " ani"));

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
            startActivity(intent);
        });

        // Adaugă cardul la GridLayout
        gridLayout.addView(itemView);
    }

    private void addCardToUIAdmin(Animal animal) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.card_item_edit, linearLayout, false);

        // Obține referințele la elementele din card
        ImageView animalPhoto = itemView.findViewById(R.id.imageItemImageView);
        TextView animalName = itemView.findViewById(R.id.textViewName);
        TextView animalBreed = itemView.findViewById(R.id.textViewBreed);
        TextView animalAge = itemView.findViewById(R.id.textViewAge);
        TextView animalAdoptedStatus = itemView.findViewById(R.id.textViewAdoptedStatus);
        ImageButton imageEdit = itemView.findViewById(R.id.imageEdit);
        ImageButton imageButtonDelete = itemView.findViewById(R.id.imageButtonDelete);

        // Populează datele animalului
        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalAge.setText(animal.getYears() + (animal.getYears() == 1 ? " an" : " ani") +
                " si " + animal.getMonths() + (animal.getMonths() == 1 ? " luna" : " luni"));
        animalAdoptedStatus.setText(animal.isAdopted() ? "Adoptat" : "Disponibil");

        if (!isFinishing() && !isDestroyed()) {
            if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
                Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
            }
        } else {
            Log.w("ListAnimalActivity", "Activitatea este distrusă, nu încarcăm imaginea pentru " + animal.getName());
        }

        imageEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditAnimalActivity.class);
            intent.putExtra("animal", animal.getId());
            startActivity(intent);
        });
        imageButtonDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog(animal.getId());
        });

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimalProfileActivity.class);
            intent.putExtra("animal", animal.getId());
            intent.putExtra("favorite", "favorite");
            startActivity(intent);
        });

        // Adaugă cardul la LinearLayout
        linearLayout.addView(itemView);
    }

    private void showDeleteConfirmationDialog(String Id) {
        new AlertDialog.Builder(this)
                .setTitle("Ștergere animal din baza de date")
                .setMessage("Ești sigur că vrei să ștergi acest animal din baza de date?")
                .setPositiveButton("Da", (dialog, which) -> {
                    deleteAnimalAndImage(Id);
                })
                .setNegativeButton("Anulează", (dialog, which) -> {
                    dialog.dismiss();
                })
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
                                deleteAnimalAndApplications(animalId);
                            })
                            .addOnFailureListener(e -> Log.w("Firebase", "Eroare la actualizarea listei de favorite.", e));
                })
                .addOnFailureListener(e -> Log.w("Firebase", "Eroare la căutarea utilizatorilor.", e));
    }

    private void deleteAnimalAndApplications(String animalId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
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
}