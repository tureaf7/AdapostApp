package com.example.adapostapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.ProgressDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adapostapp.utils.UserUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class RegisterActivity extends AppCompatActivity {
    private EditText nameET, emailET, passwordET, confirmPasswordET;
    private Button registerButton;
    private ImageView profileImageView;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameET = findViewById(R.id.name);
        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password);
        confirmPasswordET = findViewById(R.id.passwordConfirm);
        registerButton = findViewById(R.id.registerButton);
        profileImageView = findViewById(R.id.imageViewProfile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_pictures");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Se înregistrează...");

        // Alegere imagine din galerie
        profileImageView.setOnClickListener(view -> selectImage());

        registerButton.setOnClickListener(view -> registerUser());
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selectează o imagine"), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    private void registerUser() {
        String name = nameET.getText().toString().trim();
        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();
        String confirmPassword = confirmPasswordET.getText().toString().trim();

        if (password.length() < 6) {
            passwordET.setError("Parola trebuie să aibă cel puțin 6 caractere!");
            passwordET.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordET.setError("Parolele nu se potrivesc!");
            confirmPasswordET.requestFocus();
            return;
        }
        if (name.isEmpty()) {
            nameET.setError("Acest câmp este obligatoriu!");
            nameET.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            emailET.setError("Acest câmp este obligatoriu!");
            emailET.requestFocus();
            return;
        }

        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password) // Creaza utilizatorul
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser(); // Obținem utilizatorul creat
                        if (user != null) {
                            Log.d("Firebase", user.getUid());
                            uploadImageAndSaveData(user, name, email); // Salvează datele și imaginea
                        }
                    } else {
                        progressDialog.dismiss();
                        try {
                            throw Objects.requireNonNull(task.getException());
                        } catch (FirebaseAuthUserCollisionException e) {
                            emailET.setError("Email deja înregistrat");
                            emailET.requestFocus();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            emailET.setError("Email invalid");
                            emailET.requestFocus();
                        } catch (Exception e) {
                            Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        Toast.makeText(getApplicationContext(), "Autentificare eșuată!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void uploadImageAndSaveData(FirebaseUser user, String name, String email) {
        if (imageUri == null) {
            // Dacă utilizatorul nu a ales o imagine, salvăm datele fără imagine
            saveUserData(user, name, email, null);
            return;
        }

        StorageReference fileRef = storageRef.child(user.getUid() + ".jpg");
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveUserData(user, name, email, uri.toString())))
                .addOnFailureListener(e -> {
                    user.delete().addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this,
                                "Eroare la încărcarea imaginii! Înregistrarea a fost anulată.",
                                Toast.LENGTH_LONG).show();
                    });
                });
    }

    private void saveUserData(FirebaseUser firebaseUser, String name, String email, String imageUrl) {
        String finalImageUrl = (imageUrl != null) ? imageUrl : ""; // Dacă nu există imagine, folosim un string gol

        User user = new User(name, finalImageUrl, email, "user");

        UserUtils.addUser(firebaseUser, user, new UserUtils.AddUserCallback() {
            @Override
            public void onAddSuccess() {
                UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name);
                if (!finalImageUrl.isEmpty()) {
                    profileUpdates.setPhotoUri(Uri.parse(finalImageUrl));
                }
                firebaseUser.updateProfile(profileUpdates.build());

                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "Înregistrare reușită!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, ProfileActivity.class));
                finish();
            }

            @Override
            public void onAddFailure(Exception e) {
                if (!finalImageUrl.isEmpty()) {
                    deleteUserAndImage(firebaseUser, finalImageUrl);
                } else {
                    firebaseUser.delete().addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Eroare la salvarea datelor! Înregistrarea a fost anulată.",
                                Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }


    private void deleteUserAndImage(FirebaseUser user, String imageUrl) {
        //Ștergem poza din Firebase Storage
        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        imageRef.delete().addOnCompleteListener(task -> {
            //După ce poza a fost ștearsă, ștergem utilizatorul din Firebase Authentication
            user.delete().addOnCompleteListener(deleteTask -> {
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "Eroare la salvarea datelor! Înregistrarea a fost anulată.",
                        Toast.LENGTH_LONG).show();
            });
        });
    }

}