package com.example.adapostapp;

// Firebase

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

// Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import androidx.appcompat.app.AlertDialog;

// Android
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;

import com.example.adapostapp.ui.login.AuthViewModel;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;


public class ProfileActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private Button registerButton, loginButton, signInButton, logOutButton;
    private ImageView userPhotoImageView;
    private TextView userNameTextView, textViewAuth;
    private LinearLayout layoutUserInfo;
    private AuthViewModel authViewModel;
    private ImageButton buttonBackToMain;
    private BottomNavigationView bottomNavigationView;

    @SuppressLint({"ResourceAsColor", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);
        signInButton = findViewById(R.id.signInGoogleButton);
        logOutButton = findViewById(R.id.logOutButton);
        userPhotoImageView = findViewById(R.id.userPhotoImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        layoutUserInfo = findViewById(R.id.layoutUserInfo);
        textViewAuth = findViewById(R.id.textViewAuth);
        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(ProfileActivity.this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(ProfileActivity.this, MessagesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                return true; // Rămâi în Profile
            }
            return false;
        });

        // Inițializează FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, 9001);
        });


        // Configurează butoanele
        buttonBackToMain.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        loginButton.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        logOutButton.setOnClickListener(v -> showSignOutConfirmationDialog());

        // Observă utilizatorul logat
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserExists(currentUser);
        } else {
            profileHide();
        }

        authViewModel.getCurrentUser().observe(this, this::updateUI);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Autentificare cu Google eșuată.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Autentificare reușită!", Toast.LENGTH_SHORT).show();
                        updateUI(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Eroare: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            profileShow();
            String email = user.getEmail();       // Email-ul utilizatorului
            String photoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null;

            // Verifică dacă utilizatorul este logat prin Google
            boolean isGoogleUser = false;
            for (UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) {
                    isGoogleUser = true;
                    break;
                }
            }

            if (isGoogleUser) {
                // Dacă utilizatorul s-a autentificat cu Google
                String name = user.getDisplayName();   // Numele complet (din Google, dacă este disponibil)
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_launcher_foreground) // Imagine de rezervă (opțional)
                        .into(userPhotoImageView);
                userNameTextView.setText(name != null ? name : email);
            } else {
                // Dacă utilizatorul s-a autentificat cu email și parolă, ia datele din Firestore
                // Obține documentul corespunzător din colecția "users"
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .document(user.getUid())  // Folosește UID-ul utilizatorului pentru a găsi documentul corect
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Dacă documentul există în Firestore, extrage datele
                                String name = documentSnapshot.getString("name");
                                String urlImage = documentSnapshot.getString("profileImage");


                                // Încarcă imaginea în ImageView folosind Glide
                                Glide.with(this)
                                        .load(urlImage)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .into(userPhotoImageView);

                                userNameTextView.setText(name != null ? name : email);
                            } else {
                                // Dacă nu există datele utilizatorului în Firestore, afișează emailul
                                userNameTextView.setText(email);
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Dacă există o eroare la accesarea Firestore, poți trata eroarea aici
                            Toast.makeText(this, "Eroare la obținerea datelor utilizatorului din Firestore.", Toast.LENGTH_SHORT).show();
                            userNameTextView.setText(email);  // În caz de eroare, arată emailul
                        });

            }
            saveUserDataToFirestore(user);
        } else {
            // Utilizatorul nu este autentificat, actualizează UI pentru ecranul de login
            Toast.makeText(this, "Autentificare nereușită sau utilizator deconectat.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserDataToFirestore(FirebaseUser user) {
        // Verifică dacă există date în Firestore pentru utilizatorul curent
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();
        if(user != null){
            // Caută utilizatorul în Firestore
            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            // Dacă nu există, creează un document nou
                            int[] favorite = new int[0];
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", user.getDisplayName());
                            userData.put("email", user.getEmail());
                            userData.put("profileImage", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
                            userData.put("uid", uid);
                            db.collection("users").document(uid).set(userData);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Gestionează eventualele erori
                        Toast.makeText(this, "Eroare la incarcarea datelor in firestore.", Toast.LENGTH_SHORT).show();
                        Log.e("ProfileActivity", "Error checking or updating user data in Firestore: ", e);
                    });

        }
    }


    private void checkUserExists(FirebaseUser currentUser) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Dacă utilizatorul nu există în Firestore, deconectează-l
                        signOut();
                        Toast.makeText(ProfileActivity.this, "Contul a fost șters. Te rog să te autentifici din nou.", Toast.LENGTH_SHORT).show();
                    } else {
                        updateUI(currentUser);
                    }
                })
                .addOnFailureListener(e -> {
                    // Dacă există o eroare la accesarea Firestore, tratează-o
                    Toast.makeText(ProfileActivity.this, "Eroare la accesarea datelor utilizatorului.", Toast.LENGTH_SHORT).show();
                    signOut();
                });
    }


    private void showSignOutConfirmationDialog() {
        // Creează un dialog de confirmare
        new AlertDialog.Builder(this)
                .setTitle("Deconectare")
                .setMessage("Ești sigur că vrei să te deconectezi?")
                .setPositiveButton("Da", (dialog, which) -> {
                    // Apelează metoda signOut dacă utilizatorul confirmă
                    signOut();
                })
                .setNegativeButton("Anulează", (dialog, which) -> {
                    // Închide dialogul fără nicio acțiune
                    dialog.dismiss();
                })
                .show();
    }
    private void signOut() {
        mAuth.signOut();
        profileHide();
        Toast.makeText(this, "Utilizatorul a fost deconectat.", Toast.LENGTH_SHORT).show();
    }

    private void profileShow() {
        textViewAuth.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);
        signInButton.setVisibility(View.GONE);
        logOutButton.setVisibility(View.VISIBLE);
        layoutUserInfo.setVisibility(View.VISIBLE);
    }

    private void profileHide() {
        textViewAuth.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.VISIBLE);
        signInButton.setVisibility(View.VISIBLE);
        logOutButton.setVisibility(View.GONE);
        layoutUserInfo.setVisibility(View.GONE);
    }

}