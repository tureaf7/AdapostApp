package com.example.adapostapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ApplicationsListActivity extends BaseActivity {
    private ProgressBar progressBar;
    private RecyclerView recyclerViewApplications;
    private TextView noneFavoriteTextView;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
    private Spinner spinnerFilter;
    private TabLayout tabLayout;
    private ApplicationsAdapter adapter;
    private List<VolunteerApplications> allVolunteerApplications = new ArrayList<>();
    private List<AdoptionApplication> allAdoptionApplications = new ArrayList<>();
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
        recyclerViewApplications = findViewById(R.id.recyclerViewApplications);
        noneFavoriteTextView = findViewById(R.id.textViewEmpty);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        tabLayout = findViewById(R.id.tabLayout);

        adapter = new ApplicationsAdapter(this);
        recyclerViewApplications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewApplications.setAdapter(adapter);

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("Toate");
        filterOptions.add("În așteptare");
        filterOptions.add("Aprobat");
        filterOptions.add("Respins");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterOptions);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = parent.getItemAtPosition(position).toString();
                filterRequests(selectedStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        int selectedTab = getIntent().getIntExtra("selectedTab", 0);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    fetchAdoptionApplications();
                } else if (position == 1) {
                    fetchVolunteerApplications();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
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
                        adapter.setApplications(new ArrayList<>());
                        noneFavoriteTextView.setVisibility(View.VISIBLE);
                    }
                    progressBar.setVisibility(View.GONE);
                }).addOnFailureListener(e -> {
                    Log.e("Firebase", "Eroare la preluarea cererilor de voluntariat", e);
                    adapter.setApplications(new ArrayList<>());
                    noneFavoriteTextView.setVisibility(View.VISIBLE);
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
                        adapter.setApplications(new ArrayList<>());
                        noneFavoriteTextView.setVisibility(View.VISIBLE);
                    }
                    progressBar.setVisibility(View.GONE);
                }).addOnFailureListener(e -> {
                    Log.e("Firebase", "Eroare la preluarea cererilor de adopție", e);
                    adapter.setApplications(new ArrayList<>());
                    noneFavoriteTextView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void filterRequests(String status) {
        List<Object> filteredApplications = new ArrayList<>();
        boolean hasVisibleItems = false;

        if (tabLayout.getSelectedTabPosition() == 0) {
            for (AdoptionApplication application : allAdoptionApplications) {
                if ("Toate".equals(status) || application.getStatus().equals(status)) {
                    filteredApplications.add(application);
                    hasVisibleItems = true;
                }
            }
        } else {
            for (VolunteerApplications application : allVolunteerApplications) {
                if ("Toate".equals(status) || application.getStatus().equals(status)) {
                    filteredApplications.add(application);
                    hasVisibleItems = true;
                }
            }
        }

        adapter.setApplications(filteredApplications);
        noneFavoriteTextView.setVisibility(hasVisibleItems ? View.GONE : View.VISIBLE);
    }
}