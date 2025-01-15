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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.adapostapp.ui.login.LoginFragment;
import com.example.adapostapp.ui.login.RegisterFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.fragment.app.FragmentTransaction;

public class ProfileActivity extends AppCompatActivity {

    private Button signInButton, logOutButton;
    private ImageButton backButton;
    private FirebaseAuth mAuth;
    private TextView userNameTextView;
    private ImageView userPhotoImageView;
    private LinearLayout layoutUserInfo;
    private FrameLayout fragmentContainer;
    private ScrollView scrollViewProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        backButton = findViewById(R.id.buttonBackToMain);
        signInButton = findViewById(R.id.signInButton);
        userNameTextView = findViewById(R.id.userNameTextView);
        userPhotoImageView = findViewById(R.id.userPhotoImageView);
        logOutButton = findViewById(R.id.logOutButton);
        layoutUserInfo = findViewById(R.id.layoutUserInfo);
        scrollViewProfile = findViewById(R.id.scrollViewProfile);

        mAuth = FirebaseAuth.getInstance();
        backButton.setOnClickListener(v -> finish()); // Pur și simplu închide activitatea pentru a te întoarce
        signInButton.setOnClickListener(v -> signInWithGoogle());

        fragmentContainer = findViewById(R.id.fragment_container);
        signInButton.setOnClickListener(v -> signInWithGoogle());
        Button loginButton = findViewById(R.id.logInbutton); // Butonul de Login din XML
        loginButton.setOnClickListener(v -> showLoginFragment());

        logOutButton.setOnClickListener(v -> signOut());

        updateUI(mAuth.getCurrentUser()); // Actualizează UI pe baza stării curente a utilizatorului

    }

    public void setFragmentContainerVisibility(int visibility) {
        fragmentContainer.setVisibility(visibility);
    }
    private void showLoginFragment() {
        LoginFragment loginFragment = new LoginFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, loginFragment);
        transaction.addToBackStack(null); // Permite utilizatorului să se întoarcă
        transaction.commit();
        scrollViewProfile.setVisibility(View.GONE); // O ascundere
        fragmentContainer.setVisibility(View.VISIBLE); // Face containerul vizibil
    }

    private void showRegisterFragment() {
        RegisterFragment registerFragment = new RegisterFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, registerFragment);
        transaction.addToBackStack(null);
        transaction.commit();
        fragmentContainer.setVisibility(View.VISIBLE);
    }


    private void signInWithGoogle() {
        AuthManager authManager = new AuthManager(this, new AuthManager.AuthCallback() {
            @Override
            public void onSignInSuccess(FirebaseUser user) {
                Log.d("ProfileActivity", "Autentificare reușită");
                Toast.makeText(ProfileActivity.this, "Autentificare reușită", Toast.LENGTH_SHORT).show();
                updateUI(user);
            }

            @Override
            public void onSignInFailure(String message) {
                Log.e("ProfileActivity", "Eroare la autentificare: " + message);
                Toast.makeText(ProfileActivity.this, "Eroare la autentificare: " + message, Toast.LENGTH_SHORT).show();
            }
        });
        authManager.signIn();
    }


    private void signOut() {
        mAuth.signOut();
        Toast.makeText(this, "Deconectare reușită", Toast.LENGTH_SHORT).show();
        updateUI(null); // Actualizează UI pentru a reflecta starea de deconectare
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            fragmentContainer.setVisibility(View.GONE);
            layoutUserInfo.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.GONE);
            logOutButton.setVisibility(View.VISIBLE);

            String name = user.getDisplayName();
            Uri photoUrl = user.getPhotoUrl();

            userNameTextView.setText(name != null && !name.isEmpty() ? name : "Utilizator necunoscut");

            if (photoUrl != null) {
                Glide.with(this).load(photoUrl).into(userPhotoImageView);
            } else {
                userPhotoImageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            fragmentContainer.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE); // Afișează butonul Google Sign-In
            Button loginButton = findViewById(R.id.logInbutton); // Butonul de Login din XML
            loginButton.setVisibility(View.VISIBLE); // Afișează butonul Login
            logOutButton.setVisibility(View.GONE);
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AuthManager authManager = new AuthManager(this,null); // Transmite un callback fictiv, deoarece este deja gestionat
        authManager.handleSignInResult(requestCode, resultCode, data);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            updateUI(user);
        }
    }
}