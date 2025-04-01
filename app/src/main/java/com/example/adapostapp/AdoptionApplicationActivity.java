package com.example.adapostapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AdoptionApplicationActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private LinearLayout linearLayout;
    private TextView noneFavoriteTextView;
    private String animalId;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
    private Spinner spinnerFilter;
    private List<AdoptionApplication> allApplications = new ArrayList<>();
    private ImageButton buttonBackToMain;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_application);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        progressBar = findViewById(R.id.progressBar);
        linearLayout = findViewById(R.id.linearLayout);
        noneFavoriteTextView = findViewById(R.id.textViewEmpty);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(this, ChatListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_animals) {
                startActivity(new Intent(this, ListAnimalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        animalId = getIntent().getStringExtra("animal");

        spinnerFilter = findViewById(R.id.spinnerFilter);

        // Opțiuni pentru filtrare
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("Toate");
        filterOptions.add("În așteptare");
        filterOptions.add("Aprobat");
        filterOptions.add("Respins");

        // Adapter pentru Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterOptions);
        spinnerFilter.setAdapter(adapter);

        // Gestionare selecție
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = parent.getItemAtPosition(position).toString();
                filterAdoptionRequests(selectedStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        fetchAdoptionApplications();

    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAdoptionApplications();
    }

    private void fetchAdoptionApplications() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("AdoptionApplications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allApplications.clear(); // Curăță lista înainte de a adăuga noi elemente
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            AdoptionApplication adoptionApplication = documentSnapshot.toObject(AdoptionApplication.class);
                            adoptionApplication.setApplicationId(documentSnapshot.getId());
                            if (animalId == null || animalId.isEmpty() || animalId.equals(adoptionApplication.getAnimalId())) {
                                allApplications.add(adoptionApplication);
                            }
                        }
                        Collections.sort(allApplications, (a1, a2) -> a2.getApplicationDate().compareTo(a1.getApplicationDate()));
                        filterAdoptionRequests(spinnerFilter.getSelectedItem().toString()); // Filtrare inițială

                    }
                }).addOnFailureListener(e -> {
                    Log.e("Firebase", "Eroare la preluarea datelor", e);
                    noneFavoriteTextView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                });
    }


    private void addApplicationCards(AdoptionApplication adoptionApplication) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.adoption_application, linearLayout, false);
        db.collection("Animals").document(adoptionApplication.getAnimalId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Animal animal = documentSnapshot.toObject(Animal.class);
                        TextView nameTextView = itemView.findViewById(R.id.animalNameTextView);
                        nameTextView.setText(animal.getName());
                        TextView dateTextView = itemView.findViewById(R.id.adoptionRequestDateTextView);

                        dateTextView.setText("Cerere trimisa: " + dateFormat.format(adoptionApplication.getApplicationDate()));

                        ImageView imageView = itemView.findViewById(R.id.animalImageView);
                        FirebaseStorage.getInstance().getReferenceFromUrl(animal.getPhoto()).getDownloadUrl().addOnSuccessListener(uri -> {
                            if (imageView.getContext() != null) {
                                Glide.with(imageView.getContext()).load(uri).error(R.drawable.cat).into(imageView);
                            }

                        });
                    }
                });

        if ("Respins".equals(adoptionApplication.getStatus())) {
            TextView statusTextView = itemView.findViewById(R.id.adoptionStatusRejectedTextView);
            TextView adminName = itemView.findViewById(R.id.adminNameTextView);
            TextView dateAnswer = itemView.findViewById(R.id.dateAnswer);
            dateAnswer.setText(dateFormat.format(adoptionApplication.getDateAnswer()));
            statusTextView.setVisibility(View.VISIBLE);
            Log.d("Status", "Respins");
            db.collection("users")
                    .document(adoptionApplication.getAdminId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                adminName.setText(user.getName());
                            }
                        }
                    });

        } else if ("Aprobat".equals(adoptionApplication.getStatus())) {
            TextView statusTextView = itemView.findViewById(R.id.adoptionStatusApprovedTextView);
            TextView adminName = itemView.findViewById(R.id.adminNameTextView);
            TextView dateAnswer = itemView.findViewById(R.id.dateAnswer);
            dateAnswer.setText(dateFormat.format(adoptionApplication.getDateAnswer()));
            statusTextView.setVisibility(View.VISIBLE);
            Log.d("Status", "Aprobat");
            db.collection("users")
                    .document(adoptionApplication.getAdminId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                adminName.setText(user.getName());
                            }
                        }
                    });
        } else {
            TextView statusTextView = itemView.findViewById(R.id.adoptionStatusPendingTextView);
            statusTextView.setVisibility(View.VISIBLE);
            Log.d("Status", "In asteptare");
        }
//        statusTextView.setVisibility(View.VISIBLE);
        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdoptionApplicationDetailsActivity.class);
            intent.putExtra("adoptionApplication", adoptionApplication.getApplicationId());
            startActivity(intent);
        });
        linearLayout.addView(itemView);
    }

    private void filterAdoptionRequests(String status) {
        linearLayout.removeAllViews(); // Curăță layout-ul înainte de a adăuga elemente noi

        for (AdoptionApplication application : allApplications) {
            if (status.equals("Toate") || application.getStatus().equals(status)) {
                addApplicationCards(application);
            }
        }
        progressBar.setVisibility(View.GONE);
    }


}
