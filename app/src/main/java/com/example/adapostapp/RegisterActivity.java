package com.example.adapostapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.adapostapp.ui.login.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;


public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private AuthViewModel authViewModel;
    private Uri imageUri;
    private ImageView imageViewProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inițializare referințe la elemente din layout
        nameEditText = findViewById(R.id.name);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.passwordConfirm);
        registerButton = findViewById(R.id.registerButton);
        imageViewProfile = findViewById(R.id.imageViewProfile);

        // Initialize AuthViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        imageViewProfile.setOnClickListener(v -> openImageChooser());

        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String confirmPassword = confirmPasswordEditText.getText().toString();

            // Validări (esential!)
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || password.isEmpty()) {
                if (!password.equals(confirmPassword)){
                    Toast.makeText(RegisterActivity.this, "Parolele nu se potrivesc", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(RegisterActivity.this, "Vă rugăm să completați toate câmpurile corect.", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(email, password, name);
        });


        // Observarea LiveData pentru rezultatele înregistrării
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                Toast.makeText(RegisterActivity.this, "Înregistrare reușită!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        // Observarea erorilor
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

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

            // Afișează imaginea aleasă în interfața utilizatorului
            Glide.with(this).load(imageUri).into(imageViewProfile);

            // Poți adăuga acum logica pentru a salva imaginea în Firebase Storage
            uploadImageToFirebase();
        }
    }

    // Funcție pentru încărcarea imaginii în Firebase Storage
    private void uploadImageToFirebase() {
        if (imageUri != null) {

            // Definirea unui nume unic pentru fișierul imaginii
            String fileName = "profile_images/" + System.currentTimeMillis() + ".jpg";
            // Referința către Firebase Storage
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference(fileName);

            // Încărcarea imaginii
            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Obține URL-ul imaginii încărcate
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            // Poți folosi imageUrl pentru a actualiza informațiile utilizatorului în Firestore
                            updateUserProfileImage(imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(RegisterActivity.this, "Eroare la încărcarea imaginii.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Actualizează profilul utilizatorului cu URL-ul imaginii
    private void updateUserProfileImage(String imageUrl) {
        FirebaseUser user = authViewModel.getCurrentUser().getValue();
        if (user != null) {
            // Actualizează documentul utilizatorului din Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> userData = new HashMap<>();
            userData.put("profileImage", imageUrl);

            db.collection("users").document(user.getUid())
                    .update(userData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(RegisterActivity.this, "Imaginea a fost actualizată.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(RegisterActivity.this, "Eroare la actualizarea imaginii.", Toast.LENGTH_SHORT).show();
                    });
        }
        else {
            Toast.makeText(RegisterActivity.this, "Utilizatorul nu este autentificat. firestore nu s-a actualizat", Toast.LENGTH_SHORT).show();
        }
    }
}