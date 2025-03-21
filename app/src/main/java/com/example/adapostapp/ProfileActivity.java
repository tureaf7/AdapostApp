package com.example.adapostapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.adapostapp.utils.UserUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private GoogleSignInClient googleSignInClient;
    private TextView userNameTextView, textViewRegister, adminNameTextView, textViewForgotPassword;
    private CircleImageView userPhotoImageView, adminPhotoImageView;
    private BottomNavigationView bottomNavigationView;
    private EditText emailET, passwordET;
    private ProgressDialog progressDialog;
    private LinearLayout editProfileLayout, notificationLayout, favoritesLayout, adoptionsLayout, logoutLayout, linearLayoutUser,
            linearLayoutAdmin, addAnimalLayout, editAnimalLayout, adminLogOutLayout, linearLayoutLogin, adoptionApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        linearLayoutUser = findViewById(R.id.linearLayoutUser);
        linearLayoutAdmin = findViewById(R.id.linearLayoutAdmin);
        linearLayoutLogin = findViewById(R.id.linearLayoutLogin);
        ImageButton buttonBackToMain = findViewById(R.id.buttonBackToMain);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
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
            } else if (itemId == R.id.navigation_animals) {
                startActivity(new Intent(ProfileActivity.this, ListAnimalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                return true;
            }
            return false;
        });


        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.loginButton);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Se autentifică...");
        loginButton.setOnClickListener(view -> loginUser());
        Button signInGoogleButton = findViewById(R.id.signInGoogleButton);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        logoutLayout = findViewById(R.id.logoutLayout);
        adoptionsLayout = findViewById(R.id.AdoptionsLayout);
        favoritesLayout = findViewById(R.id.favoritesLayout);
        notificationLayout = findViewById(R.id.notificationLayout);
        editProfileLayout = findViewById(R.id.editProfileLayout);
        userNameTextView = findViewById(R.id.userNameTextView);
        userPhotoImageView = findViewById(R.id.userPhotoImageView);

        adminPhotoImageView = findViewById(R.id.adminPhotoImageView);
        adminNameTextView = findViewById(R.id.adminNameTextView);
        textViewRegister = findViewById(R.id.textViewRegister);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        addAnimalLayout = findViewById(R.id.addAnimalLayout);
        editAnimalLayout = findViewById(R.id.editAnimalLayout);
        adoptionApplication = findViewById(R.id.adoptionApplicationLinearLayout);
        adminLogOutLayout = findViewById(R.id.adminLogoutLayout);
        textViewRegister.setText(Html.fromHtml("Nu ai un cont? <font color='#06D6A0'><b>Înregistrează-te</b></font>"));

        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) {
                // Tastatura este afișată
                bottomNavigationView.setVisibility(View.GONE);
            } else {
                // Tastatura este ascunsă
                bottomNavigationView.setVisibility(View.VISIBLE);
            }
        });


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        user = auth.getCurrentUser();
        if (user != null) {
            Log.d("ProfileActivity", "onCreate verificare - Utilizatorul este autentificat!");
            linearLayoutLogin.setVisibility(View.GONE);
            isUserExist(user);
        } else {
            Log.d("ProfileActivity", "onCreate verificare - Utilizatorul nu este autentificat!");
            linearLayoutLogin.setVisibility(View.VISIBLE);
            linearLayoutUser.setVisibility(View.GONE);
            linearLayoutAdmin.setVisibility(View.GONE);
        }

        editProfileLayout.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));
        textViewRegister.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, RegisterActivity.class)));
        textViewForgotPassword.setOnClickListener(v -> {
            String email = emailET.getText().toString().trim();
            if (email.isEmpty()) {
                emailET.setError("Introduceti email-ul!");
                emailET.requestFocus();
                return;
            }
            resetPassword(email);
        });
        buttonBackToMain.setOnClickListener(v -> onBackPressed());
        signInGoogleButton.setOnClickListener(v -> signInWithGoogle());
        logoutLayout.setOnClickListener(v -> showSignOutConfirmationDialog());
        addAnimalLayout.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, AddAnimalActivity.class)));
        editAnimalLayout.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, ListAnimalActivity.class)));
        adoptionApplication.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, AdoptionApplicationActivity.class)));
        adminLogOutLayout.setOnClickListener(v -> showSignOutConfirmationDialog());
    }

    private void resetPassword(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(task -> {
                    Toast.makeText(this, "Link de resetare a parolei a fost trimis!. Verificati emailul.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "A aparut o eroare. Te rugam sa mai incerci o data!", Toast.LENGTH_SHORT).show();
                });
    }

    private void showUserInfo(String role) {
        user = FirebaseAuth.getInstance().getCurrentUser();

        UserUtils.getUserInfo(user, new UserUtils.UserInfoCallback() {
            @Override
            public void onUserInfoRetrieved(DocumentSnapshot documentSnapshot) {
                if ("admin".equals(role)) {
                    adminNameTextView.setText(documentSnapshot.getString("name"));
//                    String imageUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null;
                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    Log.d("ImageURL", "Image URL: " + imageUrl);
                    Glide.with(ProfileActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(adminPhotoImageView);
                }else {
                    userNameTextView.setText(documentSnapshot.getString("name"));
//                    String imageUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null;
                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    Log.d("ImageURL", "Image URL: " + imageUrl);
                    Glide.with(ProfileActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(userPhotoImageView);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("ProfileActivity showUserInfo", "Utilizatorul nu a fost găsit în Firestore!");
                Toast.makeText(ProfileActivity.this, "Utilizatorul nu a fost găsit în Firestore!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            task.addOnSuccessListener(account -> {
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                auth.signInWithCredential(credential)
                        .addOnCompleteListener(this, authTask -> {
                            if (authTask.isSuccessful()) {
                                FirebaseUser firebaseUser = auth.getCurrentUser();
                                if (firebaseUser != null) {
                                    Log.d("GoogleAuth", "Utilizatorul a fost gasit " + firebaseUser.getUid());
                                    checkAndSaveUserGoogle(firebaseUser, account);
                                }
                            } else {
                                Toast.makeText(this, "Firebase Authentication Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
            });
        }
    }


    private void showSignOutConfirmationDialog() {
        // Creează un dialog de confirmare
        new AlertDialog.Builder(this)
                .setTitle("Deconectare")
                .setMessage("Ești sigur că vrei să te deconectezi?")
                .setPositiveButton("Da", (dialog, which) -> {
                    // Apelează metoda signOut dacă utilizatorul confirmă
                    logOut();
                })
                .setNegativeButton("Anulează", (dialog, which) -> {
                    // Închide dialogul fără nicio acțiune
                    dialog.dismiss();
                })
                .show();
    }

    private void logOut() {
        FirebaseAuth.getInstance().signOut();
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            FirebaseFirestore.getInstance().clearPersistence().addOnCompleteListener(clearTask -> {
                // Restart activitate pentru a reseta UI-ul
                Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Oprește activitatea curentă pentru a preveni revenirea la sesiunea veche
            });
        });
    }

    private void loginUser() {
        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Introdu email-ul și parola!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            isUserExist(user);
                        }
                    } else {
                        // Verificăm tipul erorii
                        Toast.makeText(ProfileActivity.this, "Cont inexistent sau parolă greșită.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRole(FirebaseUser user) {
        Log.d("ProfileActivity", "checkUserRole - Verificare rol pentru utilizatorul " + user.getUid() + " " + user.getDisplayName());
        UserUtils.checkUserRole(user, new UserUtils.UserRoleCallback() {
            @Override
            public void onRoleRetrieved(String role) {
                updateUserInfo(role, user);
            }

            @Override
            public void onError(Exception e) {
                Log.e("Firestore", "Eroare la preluarea rolului", e);
            }
        });
    }

    private void checkAndSaveUserGoogle(FirebaseUser firebaseUser, GoogleSignInAccount account) {
        if (firebaseUser == null) {
            Log.e("Firestore", "FirebaseUser este null!");
            return;
        }

        String userId = firebaseUser.getUid();

        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    checkUserRole(firebaseUser);
                } else {
                    if (account == null) {
                        Log.e("Firestore", "GoogleSignInAccount este null!");
                        return;
                    }

                    String photoUrl = (account.getPhotoUrl() != null) ? account.getPhotoUrl().toString() : "";
                    User user = new User(account.getDisplayName(), photoUrl, account.getEmail(), "user");
                    Log.d("Firestore", "User ID: " + userId);
                    Log.d("Firestore", "User Name: " + account.getDisplayName());
                    Log.d("Firestore", "User Email: " + account.getEmail());
                    Log.d("Firestore", "User Photo URL: " + photoUrl);

                    db.collection("users").document(userId).set(user)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "Utilizatorul a fost salvat în Firestore!");
                                updateUserInfo("user", firebaseUser); // Așteptăm să fie salvat înainte de actualizarea UI
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Eroare la salvarea utilizatorului", e);
                                Toast.makeText(this, "Eroare la salvarea utilizatorului!", Toast.LENGTH_SHORT).show();
                            });
                }
            } else {
                Log.e("Firestore", "Eroare la preluarea utilizatorului!", task.getException());
                Toast.makeText(this, "Eroare la verificarea utilizatorului!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateUserInfo(String role, FirebaseUser user) {
        // Actualizează UI pe baza rolului
        if ("admin".equals(role)) {
            linearLayoutAdmin.setVisibility(View.VISIBLE);
            linearLayoutUser.setVisibility(View.GONE);
            linearLayoutLogin.setVisibility(View.GONE);
        } else if ("user".equals(role)) {
            linearLayoutLogin.setVisibility(View.GONE);
            linearLayoutUser.setVisibility(View.VISIBLE);
            linearLayoutAdmin.setVisibility(View.GONE);
            notificationLayout.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, NotificationActivity.class)));
            favoritesLayout.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, FavoritesActivity.class)));
        }
        if (isUserLoggedInWithGoogle(user)) { // Ascunde butonul de editare profil daca e GoogleAuth
            editProfileLayout.setVisibility(View.GONE);
            Log.d("ProfileActivity", "Utilizatorul este autentificat cu Google!");
        }
        showUserInfo(role);
    }

    private void isUserExist(FirebaseUser user) {
        // Verifică dacă utilizatorul există
        UserUtils.isUserExist(user, new UserUtils.UserExistCallback() {
            @Override
            public void onUserExist(boolean exists) {
                if (exists) {
                    // Utilizatorul există
                    Log.d("AnotherActivity", "Utilizatorul există!");
                    checkUserRole(user);
                } else {
                    // Utilizatorul nu există
                    Log.e("AnotherActivity", "Utilizatorul nu există sau a fost sters!");
                    Toast.makeText(ProfileActivity.this, "Utilizatorul nu există sau a fost sters!", Toast.LENGTH_SHORT).show();
                    logOut();
                }
            }

            @Override
            public void onError(Exception e) {
                // Gestionarea erorii
                Log.e("AnotherActivity", "Eroare la verificarea utilizatorului", e);
            }
        });
    }

    private boolean isUserLoggedInWithGoogle(FirebaseUser user) {
        if (user != null) {
            for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {
                    return true; // Utilizatorul este logat cu Google
                }
            }
        }
        return false; // Utilizatorul este logat cu email și parolă
    }

}
