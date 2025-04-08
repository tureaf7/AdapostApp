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

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
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

public class ApplicationsListActivity extends BaseActivity {
    private ProgressBar progressBar;
    private LinearLayout linearLayout;
    private TextView noneFavoriteTextView;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
    private Spinner spinnerFilter;
    private TabLayout tabLayout;
    private List<VolunteerApplications> allVolunteerApplications = new ArrayList<>();
    private List<AdoptionApplication> allAdoptionApplications = new ArrayList<>(); // Presupunem că ai o clasă AdoptionApplications
    private ImageButton buttonBackToMain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applications_list);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        setupBottomNavigation(R.id.navigation_applications);

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        progressBar = findViewById(R.id.progressBar);
        linearLayout = findViewById(R.id.linearLayout);
        noneFavoriteTextView = findViewById(R.id.textViewEmpty);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        tabLayout = findViewById(R.id.tabLayout);

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        // Opțiuni pentru filtrare
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("Toate");
        filterOptions.add("În așteptare");
        filterOptions.add("Aprobat");
        filterOptions.add("Respins");

        // Adapter pentru Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterOptions);
        spinnerFilter.setAdapter(adapter);

        // Gestionare selecție Spinner
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = parent.getItemAtPosition(position).toString();
                filterRequests(selectedStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nu este necesar să faci ceva aici
            }
        });

        int selectedTab = getIntent().getIntExtra("selectedTab", 0);

        // Gestionare TabLayout
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    // Tab "Adopție"
                    fetchAdoptionApplications();
                } else if (position == 1) {
                    // Tab "Voluntariat"
                    fetchVolunteerApplications();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Nu este necesar să faci ceva aici
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab); // Reîmprospătăm datele dacă tab-ul este reselectionat
            }
        });

        tabLayout.getTabAt(selectedTab).select();
        if (selectedTab == 0) {
            fetchAdoptionApplications();
        } else {
            fetchVolunteerApplications();
        }
    }

    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_applications;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reîmprospătăm datele pentru tab-ul curent
        if (tabLayout.getSelectedTabPosition() == 0) {
            fetchAdoptionApplications();
        } else {
            fetchVolunteerApplications();
        }
    }

    private void fetchVolunteerApplications() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("VolunteerRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allVolunteerApplications.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            VolunteerApplications volunteerApplication = documentSnapshot.toObject(VolunteerApplications.class);
                            if (volunteerApplication != null) {
                                volunteerApplication.setId(documentSnapshot.getId());
                                allVolunteerApplications.add(volunteerApplication);
                            }
                        }
                        Collections.sort(allVolunteerApplications, (a1, a2) -> a2.getSubmittedAt().compareTo(a1.getSubmittedAt()));
                        filterRequests(spinnerFilter.getSelectedItem().toString());
                    } else {
                        noneFavoriteTextView.setVisibility(View.VISIBLE);
                        linearLayout.removeAllViews();
                    }
                    progressBar.setVisibility(View.GONE);
                }).addOnFailureListener(e -> {
                    Log.e("Firebase", "Eroare la preluarea cererilor de voluntariat", e);
                    noneFavoriteTextView.setVisibility(View.VISIBLE);
                    linearLayout.removeAllViews();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void fetchAdoptionApplications() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("AdoptionApplications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allAdoptionApplications.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            AdoptionApplication adoptionApplication = documentSnapshot.toObject(AdoptionApplication.class);
                            if (adoptionApplication != null) {
                                adoptionApplication.setId(documentSnapshot.getId());
                                allAdoptionApplications.add(adoptionApplication);
                            }
                        }
                        Collections.sort(allAdoptionApplications, (a1, a2) -> a2.getApplicationDate().compareTo(a1.getApplicationDate()));
                        filterRequests(spinnerFilter.getSelectedItem().toString());
                    } else {
                        noneFavoriteTextView.setVisibility(View.VISIBLE);
                        linearLayout.removeAllViews();
                    }
                    progressBar.setVisibility(View.GONE);
                }).addOnFailureListener(e -> {
                    Log.e("Firebase", "Eroare la preluarea cererilor de adopție", e);
                    noneFavoriteTextView.setVisibility(View.VISIBLE);
                    linearLayout.removeAllViews();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void addVolunteerApplicationCard(VolunteerApplications volunteerApplication) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.volunteer_application, linearLayout, false);

        db.collection("users")
                .document(volunteerApplication.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            TextView nameTextView = itemView.findViewById(R.id.userNameTextView);
                            nameTextView.setText(user.getName());

                            TextView dateTextView = itemView.findViewById(R.id.requestDateTextView);
                            dateTextView.setText("Cerere trimisă: " + dateFormat.format(volunteerApplication.getSubmittedAt()));

                            ImageView imageView = itemView.findViewById(R.id.userImageView);
                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                FirebaseStorage.getInstance().getReferenceFromUrl(user.getProfileImageUrl())
                                        .getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            if (!isFinishing() && !isDestroyed() && imageView.getContext() != null) {
                                                Glide.with(imageView.getContext())
                                                        .load(uri)
                                                        .error(R.drawable.cat)
                                                        .into(imageView);
                                            }
                                        });
                            }
                        }
                    }
                });

        TextView statusTextView;
        TextView adminName = itemView.findViewById(R.id.adminNameTextView);
        TextView dateAnswer = itemView.findViewById(R.id.dateAnswer);

        if ("Respins".equals(volunteerApplication.getStatus())) {
            statusTextView = itemView.findViewById(R.id.statusRejectedTextView);
            statusTextView.setVisibility(View.VISIBLE);
            if (volunteerApplication.getDateAnswer() != null) {
                dateAnswer.setText(dateFormat.format(volunteerApplication.getDateAnswer()));
            }
            updateAdminName(volunteerApplication.getAdminId(), adminName, "Respins de: ");
        } else if ("Aprobat".equals(volunteerApplication.getStatus())) {
            statusTextView = itemView.findViewById(R.id.statusApprovedTextView);
            statusTextView.setVisibility(View.VISIBLE);
            if (volunteerApplication.getDateAnswer() != null) {
                dateAnswer.setText(dateFormat.format(volunteerApplication.getDateAnswer()));
            }
            updateAdminName(volunteerApplication.getAdminId(), adminName, "Aprobat de: ");
        } else {
            statusTextView = itemView.findViewById(R.id.statusPendingTextView);
            statusTextView.setVisibility(View.VISIBLE);
        }

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, VolunteerApplicationDetailsActivity.class);
            intent.putExtra("volunteerApplication", volunteerApplication.getId());
            startActivity(intent);
        });

        linearLayout.addView(itemView);
    }

    private void addAdoptionApplicationCard(AdoptionApplication adoptionApplication) {
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

        TextView statusTextView;
        TextView adminName = itemView.findViewById(R.id.adminNameTextView);
        TextView dateAnswer = itemView.findViewById(R.id.dateAnswer);

        if ("Respins".equals(adoptionApplication.getStatus())) {
            statusTextView = itemView.findViewById(R.id.adoptionStatusRejectedTextView);
            statusTextView.setVisibility(View.VISIBLE);
            if (adoptionApplication.getDateAnswer() != null) {
                dateAnswer.setText(dateFormat.format(adoptionApplication.getDateAnswer()));
            }
            updateAdminName(adoptionApplication.getAdminId(), adminName, "Respins de: ");
        } else if ("Aprobat".equals(adoptionApplication.getStatus())) {
            statusTextView = itemView.findViewById(R.id.adoptionStatusApprovedTextView);
            statusTextView.setVisibility(View.VISIBLE);
            if (adoptionApplication.getDateAnswer() != null) {
                dateAnswer.setText(dateFormat.format(adoptionApplication.getDateAnswer()));
            }
            updateAdminName(adoptionApplication.getAdminId(), adminName, "Aprobat de: ");
        } else {
            statusTextView = itemView.findViewById(R.id.adoptionStatusPendingTextView);
            statusTextView.setVisibility(View.VISIBLE);
        }

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdoptionApplicationDetailsActivity.class); // Presupunem că ai o activitate pentru detalii adopție
            intent.putExtra("adoptionApplication", adoptionApplication.getId());
            startActivity(intent);
        });

        linearLayout.addView(itemView);
    }

    private void updateAdminName(String adminId, TextView adminNameTextView, String prefix) {
        if (adminId != null) {
            db.collection("users")
                    .document(adminId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User admin = documentSnapshot.toObject(User.class);
                            if (admin != null) {
                                adminNameTextView.setText(prefix + admin.getName());
                            }
                        }
                    });
        }
    }

    private void filterRequests(String status) {
        linearLayout.removeAllViews();
        boolean hasVisibleItems = false;

        if (tabLayout.getSelectedTabPosition() == 0) {
            // Filtrare cereri de adopție
            for (AdoptionApplication application : allAdoptionApplications) {
                if ("Toate".equals(status) || application.getStatus().equals(status)) {
                    addAdoptionApplicationCard(application);
                    hasVisibleItems = true;
                }
            }
        } else {
            // Filtrare cereri de voluntariat
            for (VolunteerApplications application : allVolunteerApplications) {
                if ("Toate".equals(status) || application.getStatus().equals(status)) {
                    addVolunteerApplicationCard(application);
                    hasVisibleItems = true;
                }
            }
        }

        noneFavoriteTextView.setVisibility(hasVisibleItems ? View.GONE : View.VISIBLE);
    }
}