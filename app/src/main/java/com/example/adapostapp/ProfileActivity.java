package com.example.adapostapp;

// Firebase

import com.bumptech.glide.Glide;
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

// Android
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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


public class ProfileActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private Button registerButton, loginButton, signInButton, logOutButton;
    private ImageView userPhotoImageView;
    private TextView userNameTextView, textViewAuth;
    private LinearLayout layoutUserInfo;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);
        signInButton = findViewById(R.id.signInButton);
        logOutButton = findViewById(R.id.logOutButton);
        userPhotoImageView = findViewById(R.id.userPhotoImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        layoutUserInfo = findViewById(R.id.layoutUserInfo);
        textViewAuth = findViewById(R.id.textViewAuth);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

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
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        logOutButton.setOnClickListener(v -> signOut());

        // Observă utilizatorul logat
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                updateUI(user);
            } else {
                Toast.makeText(ProfileActivity.this, "Nu sunteți autentificat!", Toast.LENGTH_SHORT).show();
            }
        });
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
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        Toast.makeText(this, "Eroare: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            profileShow();
            // Obține informații despre utilizator
            String name = user.getDisplayName();   // Numele complet (din Google, dacă este disponibil)
            String email = user.getEmail();       // Email-ul utilizatorului
            String photoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null;
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_launcher_foreground) // Imagine de rezervă (opțional)
                    .into(userPhotoImageView);
            userNameTextView.setText(name != null ? name : email);
        } else {
            // Utilizatorul nu este autentificat, actualizează UI pentru ecranul de login
            Toast.makeText(this, "Autentificare nereușită sau utilizator deconectat.", Toast.LENGTH_SHORT).show();
        }
    }

    private void signOut() {
        mAuth.signOut();
        profileHide();
        Toast.makeText(this, "Utilizatorul a fost deconectat.", Toast.LENGTH_SHORT).show();
    }

    private void profileShow(){
        textViewAuth.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);
        signInButton.setVisibility(View.GONE);
        logOutButton.setVisibility(View.VISIBLE);
        layoutUserInfo.setVisibility(View.VISIBLE);
    }

    private void profileHide(){
        textViewAuth.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.VISIBLE);
        signInButton.setVisibility(View.VISIBLE);
        logOutButton.setVisibility(View.GONE);
        layoutUserInfo.setVisibility(View.GONE);
    }

}