package com.example.adapostapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.util.Map;
import java.util.Objects;

public class AdoptionApplicationDetailsActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private TextView userName, userAddress, userPhoneNumber, animalName, petBefore, petCurrent, adoptedBefore,
            livingEnvironment, rentOrOwn, ownerPermission, allergicMember, careAnimal, vacationPlan, healthBehavior, shelterMessage,
            userEmail, adminName, animalBreed, animalAge, animalArrivalDate, petPreviousDetails, petCurrentDetails,
            applicationStatusRejected, applicationStatusApproved, detailsApplication;
    private ImageView userPhoto, animalPhoto;
    private ImageButton callButton, emailButton, messageButton, backButton;
    private Button approveButton, rejectButton;
    private String applicationId, userId;
    private LinearLayout linearLayoutButtons, linearLayoutStatusInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_application_details);

        setupBottomNavigation(R.id.navigation_applications);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        firebaseUser = auth.getCurrentUser();

        applicationId = getIntent().getStringExtra("adoptionApplication");

        userName = findViewById(R.id.user_name);
        userAddress = findViewById(R.id.user_address);
        userPhoneNumber = findViewById(R.id.user_phone_number);
        animalName = findViewById(R.id.animal_name);
        userEmail = findViewById(R.id.user_email);
        adminName = findViewById(R.id.adminName);
        animalBreed = findViewById(R.id.animal_breed);
        animalAge = findViewById(R.id.animal_age);
        animalArrivalDate = findViewById(R.id.animal_arrival_date);
        petBefore = findViewById(R.id.experienceAnswer);
        petCurrent = findViewById(R.id.user_have_other_pets);
        petPreviousDetails = findViewById(R.id.user_previous_pets_details);
        petCurrentDetails = findViewById(R.id.user_other_pets_details);
        adoptedBefore = findViewById(R.id.user_adopted_before);
        livingEnvironment = findViewById(R.id.user_living_environment);
        rentOrOwn = findViewById(R.id.user_rent_or_own);
        ownerPermission = findViewById(R.id.user_owner_permission);
        allergicMember = findViewById(R.id.user_allergic_family_member);
        careAnimal = findViewById(R.id.user_care_of_animal);
        vacationPlan = findViewById(R.id.user_vacation_plan);
        healthBehavior = findViewById(R.id.user_health_behavior_issues);
        shelterMessage = findViewById(R.id.user_message_to_shelter);
        applicationStatusRejected = findViewById(R.id.applicationStatusRejected);
        applicationStatusApproved = findViewById(R.id.applicationStatusAproved);
        linearLayoutButtons = findViewById(R.id.linearLayoutButtons);
        detailsApplication = findViewById(R.id.detailsApplication);
        linearLayoutStatusInfo = findViewById(R.id.linearLayoutStatusInfo);

        userPhoto = findViewById(R.id.imageViewUser);
        animalPhoto = findViewById(R.id.imageViewAnimal);

        callButton = findViewById(R.id.imageButtonCall);
        emailButton = findViewById(R.id.imageButtonEmail);
        messageButton = findViewById(R.id.imageButtonMessage);
        backButton = findViewById(R.id.buttonBackToMain);

        approveButton = findViewById(R.id.buttonApproveAdoption);
        rejectButton = findViewById(R.id.buttonRejectAdoption);
        getAdoptionApplicationDetails();
    }

    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_applications;
    }

    private void getAdoptionApplicationDetails() {
        db.collection("AdoptionApplications")
                .document(applicationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AdoptionApplication adoptionApplication = documentSnapshot.toObject(AdoptionApplication.class);
                        setAdoptionApplicationDetails(adoptionApplication);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("AdoptionApplicationDetailsActivity", "Error getting documents: AdoptionApplications");
                });
    }

    private void userAndAnimalInfo(String userId, String animalId) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        userName.setText(user.getName());
                        userEmail.setText(user.getEmail());
                        Glide.with(this).load(user.getProfileImageUrl()).into(userPhoto);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("AdoptionApplicationDetailsActivity", "Error getting documents: Users");
                });

        db.collection("Animals")
                .document(animalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Animal animal = documentSnapshot.toObject(Animal.class);
                        animalName.setText(animal.getName());
                        animalBreed.setText("Rasa: " + animal.getBreed());
                        animalAge.setText("Varsta: " + animal.getYears() + " ani si " + animal.getMonths() + " luni");

                        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
                        animalArrivalDate.setText("In adapost din: " + dateFormat.format(animal.getArrivalDate().toDate()));
                        Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("AdoptionApplicationDetailsActivity", "Error getting documents: Animals");
                });

    }

    private void setAdoptionApplicationDetails(AdoptionApplication adoptionApplication) {
        userAndAnimalInfo(adoptionApplication.getUserId(), adoptionApplication.getAnimalId());
        userAddress.setText(adoptionApplication.getAddress());
        userPhoneNumber.setText(adoptionApplication.getPhoneNumber());
        petBefore.setText(adoptionApplication.isHavePetsBefore() ? "Da" : "Nu");
        petPreviousDetails.setText(adoptionApplication.getPreviousPetsDetails());
        petCurrent.setText(adoptionApplication.isHaveOtherPets() ? "Da" : "Nu");
        petCurrentDetails.setText("Specie: " + adoptionApplication.getPetSpecie()
                + "\nVarsta: " + adoptionApplication.getPetAge()
                + "\nTemperament" + adoptionApplication.getPetTemperament());
        adoptedBefore.setText(adoptionApplication.isAdoptedBefore() ? "Da" : "Nu");
        livingEnvironment.setText(adoptionApplication.getLivingEnvironment());
        rentOrOwn.setText(adoptionApplication.getRentOrOwn());
        ownerPermission.setText(adoptionApplication.getOwnerPermission() ? "Da" : "Nu");
        allergicMember.setText(adoptionApplication.getAllergicFamilyMember() ? "Da" : "Nu");
        careAnimal.setText(adoptionApplication.getCareOfAnimal());
        vacationPlan.setText(adoptionApplication.getVacationPlan());
        healthBehavior.setText(adoptionApplication.getHealthBehaviorIssues());
        shelterMessage.setText(adoptionApplication.getMessageToShelter());

        if (adoptionApplication.isHavePetsBefore()) {
            petPreviousDetails.setVisibility(View.VISIBLE);
        }
        if (adoptionApplication.isHaveOtherPets()) {
            petCurrentDetails.setVisibility(View.VISIBLE);
        }
        if ("Chirie".equals(adoptionApplication.getRentOrOwn())) {
            ownerPermission.setVisibility(View.VISIBLE);
        }
        if (shelterMessage.getText().toString().isEmpty()) {
            shelterMessage.setVisibility(View.GONE);
        }

        if ("Respins".equals(adoptionApplication.getStatus()) || "Aprobat".equals(adoptionApplication.getStatus())) {
            linearLayoutButtons.setVisibility(View.GONE);
            linearLayoutStatusInfo.setVisibility(View.VISIBLE);
            getAdminName(adoptionApplication);
            detailsApplication.setText(adoptionApplication.getDetails());
            if ("Respins".equals(adoptionApplication.getStatus())) {
                applicationStatusRejected.setVisibility(View.VISIBLE);
            } else if ("Aprobat".equals(adoptionApplication.getStatus())) {
                applicationStatusApproved.setVisibility(View.VISIBLE);
            }
        } else {
            approveButton.setOnClickListener(v -> approveAdoption(adoptionApplication));
            rejectButton.setOnClickListener(v -> rejectAdoption(adoptionApplication));
        }

        backButton.setOnClickListener(v -> onBackPressed());

        callButton.setOnClickListener(v -> callUser());
        emailButton.setOnClickListener(v -> sendEmail());
        messageButton.setOnClickListener(v -> sendMessage(adoptionApplication.getUserId(), firebaseUser.getUid()));

        animalPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimalProfileActivity.class);
            intent.putExtra("animal", adoptionApplication.getAnimalId());
            startActivity(intent);
        });
    }

    private void getAdminName(AdoptionApplication adoptionApplication) {
        db.collection("users")
                .document(adoptionApplication.getAdminId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        adminName.setText(user.getName());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("VolunteerApplicationDetailsActivity", "Error getting documents: User");
                    Toast.makeText(this, "Eroare la preluarea detaliilor utilizatorului!", Toast.LENGTH_SHORT).show();
                });
    }

//    private void sendMessage() {
//        // Preia userId din AdoptionApplication
//        db.collection("AdoptionApplications")
//                .document(applicationId)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        AdoptionApplication adoptionApplication = documentSnapshot.toObject(AdoptionApplication.class);
//                        String userId = adoptionApplication.getUserId();
//                        String adminId = firebaseUser.getUid();
//
//                        db.collection("users")
//                                .document(adminId)
//                                .collection("chats")
//                                .whereEqualTo("otherUserId", userId)
//                                .get()
//                                .addOnSuccessListener(queryDocumentSnapshots -> {
//                                    if (!queryDocumentSnapshots.isEmpty()) {
//                                        // Conversație existentă
//                                        Chat chat = queryDocumentSnapshots.getDocuments().get(0).toObject(Chat.class);
//                                        if (chat != null) {
//                                            openChat(chat.getChatId(), userId);
//                                        }
//                                    } else {
//                                        // Conversație nouă
//                                        openChat(null, userId);
//                                    }
//                                }).addOnFailureListener(e -> {
//                                    Toast.makeText(this, "Eroare la preluarea conversațiilor!", Toast.LENGTH_SHORT).show();
//                                    Log.e("AdoptionApplicationDetailsActivity", "Error getting documents: Chats", e);
//                                });
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Eroare la preluarea detaliilor cererii!", Toast.LENGTH_SHORT).show();
//                    Log.e("AdoptionApplicationDetailsActivity", "Error getting documents: AdoptionApplications", e);
//                });
//    }

    private void sendMessage(String userId, String adminId) {
        db.collection("Chats")
                .whereArrayContains("participants", userId) // Verifică dacă userId1 este în listă
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        List<String> participants = (List<String>) document.get("participants");
                        Log.d("AdoptionApplicationDetailsActivity", "participants: " + participants);
                        if (participants != null && participants.contains(adminId)) {
                            // Am găsit o conversație care conține ambii utilizatori
                            String chatId = document.getString("chatId");
                            openChat(chatId, adminId);
                            return;
                        }
                    }
                    // Dacă nu există conversație, creează una nouă
                    openChat(null, adminId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la preluarea conversațiilor!", Toast.LENGTH_SHORT).show();
                    Log.e("AdoptionApplicationDetailsActivity", "Error finding chat: " + e.getMessage(), e);
                    openChat(null, adminId); // Fallback
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
        String subject = "Cerere de adopție";
        String message = "Bună ziua, " + userName.getText().toString() + ",\n\n" +
                "Vă mulțumim pentru interesul acordat adopției animalului " + animalName.getText().toString() +
                ". Am analizat cererea dumneavoastră și dorim să discutăm mai multe detalii pentru a ne asigura că această adopție " +
                "este cea mai potrivită atât pentru dumneavoastră, cât și pentru " + animalName.getText().toString() + ".\n\n" +
                "Vă rugăm să ne confirmați disponibilitatea pentru o discuție sau o întâlnire la adăpost. " +
                "De asemenea, dacă aveți întrebări suplimentare, nu ezitați să ne contactați.\n\n" +
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

    private void rejectAdoption(AdoptionApplication adoptionApplication) {
        db.collection("AdoptionApplications")
                .document(applicationId)
                .update("status", "Respins", "adminId", firebaseUser.getUid(),
                        "details", "Cererea a fost respinsa!", "dateAnswer", Calendar.getInstance().getTime())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cererea a fost respinsa!", Toast.LENGTH_SHORT).show();
//                    sendNotificationToUser(adoptionApplication.getUserId(), "Cererea ta a fost respinsa!");
                    onBackPressed();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la gestionarea cererii!", Toast.LENGTH_SHORT).show();
                });
    }

    private void approveAdoption(AdoptionApplication adoptionApplication) {
        WriteBatch batch = db.batch();

        // Pasul 1: Actualizează cererea curentă și animalul
        DocumentReference applicationRef = db.collection("AdoptionApplications").document(applicationId);
        batch.update(applicationRef, "status", "Aprobat", "adminId", firebaseUser.getUid(),
                "details", "Cererea a fost acceptata!", "dateAnswer", Calendar.getInstance().getTime());

        DocumentReference animalRef = db.collection("Animals").document(adoptionApplication.getAnimalId());
        batch.update(animalRef, "adopted", true);

        // Elimină din favorite
        db.collection("users")
                .whereArrayContains("favorites", animalRef)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DocumentReference userRef = document.getReference();
                        batch.update(userRef, "favorites", FieldValue.arrayRemove(animalRef));
                    }

                    // Comite primul batch (aprobare + animal + favorite)
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                // Pasul 2: După ce aprobarea e confirmată, respinge celelalte cereri
                                rejectOtherApplications(adoptionApplication.getAnimalId());
                            })
                            .addOnFailureListener(e -> {
                                Log.e("AdoptionApplicationDetailsActivity", "Eroare la aprobarea cererii!", e);
                                Toast.makeText(this, "Eroare la aprobarea cererii!", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la eliminarea din favorite!", Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectOtherApplications(String animalId) {
        WriteBatch rejectBatch = db.batch();

        db.collection("AdoptionApplications")
                .whereEqualTo("animalId", animalId)
                .whereNotEqualTo("status", "Aprobat") // Exclude cererea aprobată
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DocumentReference applicationsRef = document.getReference();
                        rejectBatch.update(applicationsRef, "status", "Respins", "adminId", firebaseUser.getUid(),
                                "details", "Cererea a fost respinsa!", "dateAnswer", Calendar.getInstance().getTime());
                    }

                    rejectBatch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Cererea a fost acceptată și celelalte respinse!", Toast.LENGTH_SHORT).show();
                                onBackPressed();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Eroare la respingerea celorlalte cereri!", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la procesarea cererilor!", Toast.LENGTH_SHORT).show();
                });
    }

}
