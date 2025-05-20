package com.example.adapostapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.adapostapp.utils.UserUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.adapostapp.PhotoFragment;

public class AnimalProfileActivity extends BaseActivity {

    private ViewPager2 viewPagerPhotos;
    private ImageButton backButton, favoriteButton;
    private TextView animalName, arrivalDate, animalBreed, animalYears, animalMonths, animalColor,
            animalDescription, statusApplication, animalSize;
    private ImageView animalSex;
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
        viewPagerPhotos = findViewById(R.id.viewPagerPhotos);
        animalYears = findViewById(R.id.animalYears);
        animalMonths = findViewById(R.id.animalMonths);
        animalBreed = findViewById(R.id.animalBreed);
        animalColor = findViewById(R.id.animalColor);
        animalDescription = findViewById(R.id.animalDescription);
        animalName = findViewById(R.id.animalName);
        animalSex = findViewById(R.id.animalSex);
        arrivalDate = findViewById(R.id.arrivalDate);
        backButton = findViewById(R.id.backButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        animalStatusLayout = findViewById(R.id.animalStatusLayout);
        adoptButton = findViewById(R.id.adoptButton);
        statusApplication = findViewById(R.id.statusApplication);
        animalSize = findViewById(R.id.animalSize);

        TabLayout tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);

        // Inițializare Firestore și FirebaseAuth
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Recuperarea obiectului Animal din Intent
        animalId = getIntent().getStringExtra("animal");
        if (animalId != null) {
            getAnimalDetails(animalId, user);
        } else {
            animalName.setText("Animal necunoscut");
        }

        // Adaugă logica pentru butonul "Back"
        backButton.setOnClickListener(v -> onBackPressed());
    }

    @Override
    protected int getSelectedItemId() {
        return 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (animalId != null && user != null && !isAdmin()) {
            isApplicated(animalId);
        }
    }

    private void checkUserRole(Animal animal) {
        Log.d("AnimalProfileActivity", "checkUserRole - Verificare rol pentru utilizatorul " + user.getUid() + " " + user.getDisplayName());
        if (isAdmin()) {
            favoriteButton.setImageResource(R.drawable.ic_edit);
            favoriteButton.setOnClickListener(v -> {
                Intent intent = new Intent(AnimalProfileActivity.this, EditAnimalActivity.class);
                intent.putExtra("animal", animalId);
                startActivity(intent);
            });
            if (animal.isAdopted()) {
                statusApplication.setText("Animalul a fost adoptat");
                statusApplication.setVisibility(View.VISIBLE);
            }
            adoptButton.setText("Vezi cereri");
            adoptButton.setVisibility(View.VISIBLE);
            adoptButton.setOnClickListener(v -> {
                Intent intent = new Intent(AnimalProfileActivity.this, ApplicationsListActivity.class);
                intent.putExtra("animal", animalId);
                startActivity(intent);
            });
        } else {
            isFavorite(animalId);
            if (animal.isAdopted()) {
                favoriteButton.setVisibility(View.GONE);
            }
            isApplicated(animalId);
            favoriteButton.setOnClickListener(v -> {
                if ("favorite".equals(favoriteButton.getTag())) {
                    removeFromFavorite(animalId);
                } else {
                    addToFavorite(animalId);
                }
            });
        }
    }

    private void isApplicated(String animalId) {
        db.collection("AdoptionApplications")
                .whereEqualTo("animalId", animalId)
                .whereEqualTo("userId", user.getUid())
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d("Firebase", "Cererea de adopție a fost găsită.");
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            AdoptionApplication adoptionApplication = document.toObject(AdoptionApplication.class);
                            statusApplication.setText("Cererea ta este: " + adoptionApplication.getStatus());
                            statusApplication.setVisibility(View.VISIBLE);
                            adoptButton.setVisibility(View.GONE);
                            favoriteButton.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d("Firebase", "Nu au fost găsite cereri de adopție pentru acest animal și utilizator.");
                        adoptButton.setVisibility(View.VISIBLE);
                        adoptButton.setOnClickListener(v -> {
                            Intent intent = new Intent(AnimalProfileActivity.this, AdoptionFormActivity.class);
                            intent.putExtra("animal", animalId);
                            startActivity(intent);
                        });
                    }
                }).addOnFailureListener(e -> Log.e("Firebase", "Eroare la verificarea cererii", e));
    }

    private void isFavorite(String animalId) {
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<?> favoriteAnimalsRaw = (List<?>) documentSnapshot.get("favorites");
                        if (favoriteAnimalsRaw != null) {
                            boolean isFavorite = favoriteAnimalsRaw.stream()
                                    .filter(obj -> obj instanceof DocumentReference)
                                    .map(obj -> ((DocumentReference) obj).getId())
                                    .anyMatch(id -> id.equals(animalId));
                            if (isFavorite) {
                                Log.d("Firebase", "Animalul este favorit!");
                                favoriteButton.setTag("favorite");
                                favoriteButton.setImageResource(R.drawable.ic_favorite_red);
                            } else {
                                Log.d("Firebase", "Animalul NU este favorit!");
                                favoriteButton.setTag("not_favorite");
                                favoriteButton.setImageResource(R.drawable.ic_favorite);
                            }
                        } else {
                            Log.d("Firebase", "Lista favorite nu există!");
                            favoriteButton.setTag("not_favorite");
                            favoriteButton.setImageResource(R.drawable.ic_favorite);
                        }
                    } else {
                        Log.d("Firebase", "Documentul utilizatorului nu există!");
                        favoriteButton.setTag("not_favorite");
                        favoriteButton.setImageResource(R.drawable.ic_favorite);
                    }
                }).addOnFailureListener(e -> {
                    Log.w("Firebase", "Eroare la verificarea favorite.", e);
                    favoriteButton.setTag("not_favorite");
                    favoriteButton.setImageResource(R.drawable.ic_favorite);
                });
    }

    private void statusAnimal(String status) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.animal_status_container, animalStatusLayout, false);
        TextView statusTextView = itemView.findViewById(R.id.statusAnimal);
        statusTextView.setText("✅" + status);
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
                            populateAnimalProfile(animal);
                            if (user != null) {
                                checkUserRole(animal);
                            } else {
                                Log.d("AnimalProfileActivity", "Utilizatorul nu este autentificat.");
                                favoriteButton.setVisibility(View.GONE);
                                adoptButton.setVisibility(View.GONE);
                                statusApplication.setVisibility(View.VISIBLE);
                                statusApplication.setText("Autentifică-te pentru a adopta animalul.");
                            }
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

    private void populateAnimalProfile(Animal animal) {
        animalName.setText(animal.getName());
        animalBreed.setText(animal.getBreed());
        animalYears.setText(animal.getYears() + (animal.getYears() == 1 ? " an" : " ani"));
        animalMonths.setText(animal.getMonths() + (animal.getMonths() == 1 ? " luna" : " luni"));
        animalColor.setText(animal.getColor());
        animalDescription.setText(animal.getDescription());
        animalSex.setImageResource(animal.getGen().equals("Mascul") ? R.drawable.ic_male : R.drawable.ic_female);
        animalSize.setText(animal.getAnimalSize());

        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
        arrivalDate.setText(dateFormat.format(animal.getArrivalDate().toDate()));

        if (animal.isVaccinated()) {
            statusAnimal("Vaccinat");
        }
        if (animal.isSterilized()) {
            statusAnimal("Sterilizat");
        }

        // Configurare carusel cu poze
        List<String> photoUrls = new ArrayList<>();
        if (animal.getPhoto() != null) {
            photoUrls.add(animal.getPhoto()); // Adaugă poza principală
        }
        if (animal.getMorePhotos() != null) {
            photoUrls.addAll(animal.getMorePhotos()); // Adaugă pozele suplimentare
        }
        if (!photoUrls.isEmpty()) {
            PhotoAdapter photoAdapter = new PhotoAdapter(this, photoUrls);
            viewPagerPhotos.setAdapter(photoAdapter);

            // Configurare TabLayout pentru indicatori
            new TabLayoutMediator(findViewById(R.id.tabLayoutIndicator), viewPagerPhotos,
                    (tab, position) -> {
                    }
            ).attach();
        } else {
            Log.w("AnimalProfile", "Nicio poză disponibilă pentru animal.");
        }
    }

    private void addToFavorite(String documentId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid();
        db.collection("users").document(uid)
                .update("favorites", FieldValue.arrayUnion(db.collection("Animals").document(documentId)))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Animal adăugat în favorite!");
                    Toast.makeText(AnimalProfileActivity.this, "Animal adăugat în favorite!", Toast.LENGTH_SHORT).show();
                    favoriteButton.setImageResource(R.drawable.ic_favorite_red);
                    favoriteButton.setTag("favorite");
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Eroare la actualizarea favorite: ", e));
    }

    private void removeFromFavorite(String documentId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Firestore", "Utilizatorul nu este autentificat.");
            return;
        }

        String uid = currentUser.getUid();
        db.collection("users").document(uid)
                .update("favorites", FieldValue.arrayRemove(db.collection("Animals").document(documentId)))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Animal șters din favorite!");
                    Toast.makeText(AnimalProfileActivity.this, "Animal șters din favorite!", Toast.LENGTH_SHORT).show();
                    favoriteButton.setImageResource(R.drawable.ic_favorite);
                    favoriteButton.setTag("not_favorite");
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Eroare la actualizarea favorite: ", e));
    }
}

// Adapter personalizat pentru ViewPager2
class PhotoAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {

    private List<String> photoUrls;

    public PhotoAdapter(AppCompatActivity activity, List<String> photoUrls) {
        super(activity);
        this.photoUrls = photoUrls;
    }

    @Override
    public androidx.fragment.app.Fragment createFragment(int position) {
        return PhotoFragment.newInstance(photoUrls.get(position));
    }

    @Override
    public int getItemCount() {
        return photoUrls != null ? photoUrls.size() : 0;
    }
}


