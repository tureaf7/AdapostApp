package com.example.adapostapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class VolunteerApplicationDetailsActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private TextView userName, userPhoneNumber, userEmail, experience, experienceAnswer,
            experienceDetailsText, motivationText, applicationStatusRejected, applicationStatusApproved,
            adminName, detailsApplication;
    private ImageView userPhoto;
    private ImageButton callButton, emailButton, messageButton, backButton;
    private Button approveButton, rejectButton;
    private String applicationId;
    private LinearLayout linearLayoutButtons, linearLayoutStatusInfo, linearLayoutAvailability;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_application_details);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseUser = auth.getCurrentUser();

        setupBottomNavigation(R.id.navigation_applications);
        applicationId = getIntent().getStringExtra("volunteerApplication");

        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        userPhoneNumber = findViewById(R.id.user_phone_number);
        linearLayoutButtons = findViewById(R.id.linearLayoutButtons);
        linearLayoutStatusInfo = findViewById(R.id.linearLayoutStatusInfo);
        linearLayoutAvailability = findViewById(R.id.linearLayoutAvailability);
        adminName = findViewById(R.id.adminName);
        detailsApplication = findViewById(R.id.detailsApplication);

        userPhoto = findViewById(R.id.imageViewUser);
        experience = findViewById(R.id.experience);
        experienceAnswer = findViewById(R.id.experienceAnswer);
        experienceDetailsText = findViewById(R.id.experienceDetailsText);
        motivationText = findViewById(R.id.motivationText);
        applicationStatusRejected = findViewById(R.id.applicationStatusRejected);
        applicationStatusApproved = findViewById(R.id.applicationStatusApproved);

        callButton = findViewById(R.id.imageButtonCall);
        emailButton = findViewById(R.id.imageButtonEmail);
        messageButton = findViewById(R.id.imageButtonMessage);
        backButton = findViewById(R.id.buttonBackToMain);

        approveButton = findViewById(R.id.buttonApproveVolunteer);
        rejectButton = findViewById(R.id.buttonRejectVolunteer);
        getVolunteerApplicationDetails();
        if (!isAdmin()){
            callButton.setVisibility(View.GONE);
            emailButton.setVisibility(View.GONE);
            messageButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_applications;
    }

    private void getVolunteerApplicationDetails() {
        db.collection("VolunteerRequests")
                .document(applicationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        VolunteerApplications volunteerApplications = documentSnapshot.toObject(VolunteerApplications.class);
                        db.collection("users")
                                .document(volunteerApplications.getUserId())
                                .get()
                                .addOnSuccessListener(documentSnapshot1 -> {
                                    if (documentSnapshot1.exists()) {
                                        User user = documentSnapshot1.toObject(User.class);
                                        userName.setText(user.getName());
                                        userEmail.setText(user.getEmail());
                                        Glide.with(this)
                                                .load(user.getProfileImageUrl())
                                                .error(R.drawable.ic_profile)
                                                .into(userPhoto);
                                    }
                                });
                        setVolunteerApplicationDetails(volunteerApplications);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("VolunteerApplicationDetailsActivity", "Error getting documents: VolunteerApplications");
                });
    }

    private void displayAvailability(Map<String, String> availability) {
        List<String> days = Arrays.asList("Luni", "Marți", "Miercuri", "Joi", "Vineri", "Sâmbătă", "Duminică");
        for (String day : days) {
            String availabilityHour = availability.get(day);
            if (availabilityHour != null) {
                TextView availabilityTextView = new TextView(this);
                availabilityTextView.setText(Html.fromHtml("<b>" + day + "</b>: " + availabilityHour, Html.FROM_HTML_MODE_LEGACY));
                linearLayoutAvailability.addView(availabilityTextView);
            }
        }
    }

    private void setVolunteerApplicationDetails(VolunteerApplications volunteerApplications) {
        userPhoneNumber.setText(volunteerApplications.getPhoneNumber());
        experienceAnswer.setText(volunteerApplications.getExperience() ? "Da ✅" : "Nu ❌");
        motivationText.setText(volunteerApplications.getMotivation());

        Map<String, String> availability = volunteerApplications.getAvailability();
        displayAvailability(availability);

        if (volunteerApplications.getExperience()) {
            experienceDetailsText.setText(volunteerApplications.getExperienceDetails());
            experienceDetailsText.setVisibility(View.VISIBLE);
        }

        if ("Respins".equals(volunteerApplications.getStatus()) || "Aprobat".equals(volunteerApplications.getStatus())) {
            linearLayoutButtons.setVisibility(View.GONE);
            linearLayoutStatusInfo.setVisibility(View.VISIBLE);
            getAdminName(volunteerApplications);
            detailsApplication.setText(volunteerApplications.getDetails());
            if ("Respins".equals(volunteerApplications.getStatus())) {
                applicationStatusRejected.setVisibility(View.VISIBLE);
            } else if ("Aprobat".equals(volunteerApplications.getStatus())) {
                applicationStatusApproved.setVisibility(View.VISIBLE);
            }
        } else {
            approveButton.setOnClickListener(v -> manageApplication("Aprobat", "Cererea a fost aprobata!"));
            rejectButton.setOnClickListener(v -> manageApplication("Respins", "Cererea a fost respinsa!"));
        }

        backButton.setOnClickListener(v -> onBackPressed());

        callButton.setOnClickListener(v -> callUser());
        emailButton.setOnClickListener(v -> sendEmail());
        messageButton.setOnClickListener(v -> sendMessage());
    }

    private void getAdminName(VolunteerApplications volunteerApplications) {
        db.collection("users")
                .document(volunteerApplications.getAdminId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        adminName.setText(" de către " + user.getName());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("VolunteerApplicationDetailsActivity", "Error getting documents: User");
                    Toast.makeText(this, "Eroare la preluarea detaliilor utilizatorului!", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendMessage() {
        Log.d("AdoptionApplicationDetailsActivity", "applicationId: " + applicationId);
        // Preia userId din AdoptionApplication
        db.collection("VolunteerRequests")
                .document(applicationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        VolunteerApplications volunteerApplications = documentSnapshot.toObject(VolunteerApplications.class);
                        assert volunteerApplications != null;
                        String userId = volunteerApplications.getUserId();
                        String adminId = firebaseUser.getUid();

                        db.collection("users")
                                .document(adminId)
                                .collection("chats")
                                .whereEqualTo("otherUserId", userId)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        // Conversație existentă
                                        Chat chat = queryDocumentSnapshots.getDocuments().get(0).toObject(Chat.class);
                                        if (chat != null) {
                                            openChat(chat.getChatId(), userId);
                                        }
                                    } else {
                                        // Conversație nouă
                                        openChat(null, userId);
                                    }
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(this, "Eroare la preluarea conversațiilor!", Toast.LENGTH_SHORT).show();
                                    Log.e("AdoptionApplicationDetailsActivity", "Error getting documents: Chats", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la preluarea detaliilor cererii!", Toast.LENGTH_SHORT).show();
                    Log.e("AdoptionApplicationDetailsActivity", "Error getting documents: AdoptionApplications", e);
                });
    }

    private void openChat(String chatId, String otherUserId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("otherUserId", otherUserId);
        startActivity(intent);
    }

    private void sendEmail() {
        String recipient = userEmail.getText().toString();
        String subject = "Cerere de voluntariat";
        String message = "Bună ziua, " + userName.getText().toString() + ",\n\n" +
                "Vă mulțumim pentru interesul acordat de a deveni voluntar la adăpostul nostru de animale. " +
                "Am analizat cererea dumneavoastră și dorim să discutăm mai multe detalii pentru a ne asigura că " +
                "această experiență de voluntariat este potrivită atât pentru dumneavoastră, cât și pentru nevoile noastre.\n\n" +
                "Vă rugăm să ne confirmați disponibilitatea pentru o discuție sau o întâlnire la adăpost pentru a stabili următorii pași. " +
                "Dacă aveți întrebări suplimentare, nu ezitați să ne contactați.\n\n" +
                "Așteptăm răspunsul dumneavoastră!\n\n" +
                "Cu respect,\n" +
                firebaseUser.getDisplayName() + "\n" +
                "[Numărul de contact]\n" +
                "[Adăpostul de animale]";

        String uriText = "mailto:" + recipient +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(message);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uriText));

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Nu există nicio aplicație de email instalată!", Toast.LENGTH_SHORT).show();
        }
    }


    private void callUser() {
        String phoneNumber = userPhoneNumber.getText().toString();
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }


    private void manageApplication(String status, String details) {
        db.collection("VolunteerRequests")
                .document(applicationId)
                .update("status", status, "adminId", firebaseUser.getUid(),
                        "details", details, "dateAnswer", Calendar.getInstance().getTime())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, details, Toast.LENGTH_SHORT).show();
                    onBackPressed();
                }).addOnFailureListener(e -> {
                    Log.e("VolunteerApplicationsActivity", "Error managing application: " + e.getMessage());
                    Toast.makeText(this, "Eroare la gestionarea cererii!", Toast.LENGTH_SHORT).show();
                });
    }
}
