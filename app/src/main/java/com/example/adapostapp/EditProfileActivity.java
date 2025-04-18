package com.example.adapostapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText nameEditText;
    private EditText emailEditText;
    private ProgressBar progressBar;
    private Button submitButton;
    private ImageView imageProfile;
    private ImageButton editImageButton;
    private User user;
    private Uri imageUri;
    private StorageReference storageReference;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser userFireStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        nameEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        progressBar = findViewById(R.id.progressBar);
        submitButton = findViewById(R.id.submitButton);
        imageProfile = findViewById(R.id.imageProfile);
        editImageButton = findViewById(R.id.editImageButton);

        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("profile_pictures");
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        userFireStore = auth.getCurrentUser();

        submitButton.setOnClickListener(v -> {
            if (validateFields()) {
                progressBar.setVisibility(View.VISIBLE);
                updateProfileData();
            }
        });

        editImageButton.setOnClickListener(v -> openImageChooser());
        populateFields();
    }

    private void populateFields() {
        db.collection("users").document(userFireStore.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            nameEditText.setText(user.getName());
                            emailEditText.setText(user.getEmail());
                            Glide.with(this).load(user.getProfileImageUrl()).into(imageProfile);
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la obținerea detaliilor utilizatorului:");
                });
    }

    private boolean validateFields() {
        if (TextUtils.isEmpty(nameEditText.getText().toString())) {
            nameEditText.setError("Introduceți numele");
            return false;
        }
        if (TextUtils.isEmpty(emailEditText.getText().toString())) {
            emailEditText.setError("Introduceți emailul");
            return false;
        }
        return true;
    }

    private void updateProfileData() {
        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", name);
        updateData.put("email", email);

        // Verificăm dacă există o imagine veche
        boolean hasOldImage = user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty();

        if (imageUri != null) {
            // Dacă există o imagine veche, o ștergem înainte de a încărca una nouă
            if (hasOldImage) {
                deleteOldImage(updateData);
            } else {
                uploadImageAndUpdateUser(updateData); // Dacă nu există imagine veche, încărcăm direct noua imagine
            }
        } else {
            // Dacă utilizatorul nu a selectat o imagine nouă, actualizăm doar numele și emailul
            updateData.put("profileImageUrl", hasOldImage ? user.getProfileImageUrl() : ""); // Păstrăm vechiul URL sau îl golim
            db.collection("users").document(userFireStore.getUid())
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profilul a fost actualizat!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Eroare la actualizare: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void deleteOldImage(Map<String, Object> updateData) {
        String imageUrl = user.getProfileImageUrl();

        if (imageUrl == null || imageUrl.isEmpty()) {
            // Dacă nu există o imagine veche, trecem direct la încărcarea noii imagini
            uploadImageAndUpdateUser(updateData);
            return;
        }

        Log.d("Firebase", "Ștergerea imaginii vechi: " + imageUrl);
        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

        imageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Imaginea a fost ștearsă cu succes.");
                    uploadImageAndUpdateUser(updateData);
                })
                .addOnFailureListener(e -> {
                    Log.w("Firebase", "Eroare la ștergerea imaginii.", e);
                    uploadImageAndUpdateUser(updateData); // Continuăm cu actualizarea chiar dacă ștergerea imaginii eșuează
                });
    }

    private void uploadImageAndUpdateUser(Map<String, Object> updateData) {
        if (imageUri == null) {
            // Dacă nu există o nouă imagine, doar actualizăm datele
            db.collection("users").document(userFireStore.getUid())
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profilul a fost actualizat!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Eroare la actualizare: " + e.getMessage(), Toast.LENGTH_LONG).show());
            return;
        }

        String fileName = userFireStore.getUid() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_pictures/" + fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateData.put("profileImageUrl", uri.toString());
                    db.collection("users").document(userFireStore.getUid())
                            .update(updateData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Profilul a fost actualizat!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Eroare la actualizare: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare la încărcarea imaginii: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selectați imaginea"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageProfile.setImageURI(imageUri);
        }
    }
}
