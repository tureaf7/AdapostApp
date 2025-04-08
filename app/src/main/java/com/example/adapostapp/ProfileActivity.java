package com.example.adapostapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.adapostapp.utils.UserUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private GoogleSignInClient googleSignInClient;
    private TextView userNameTextView, textViewRegister, adminNameTextView, textViewForgotPassword;
    private CircleImageView userPhotoImageView, adminPhotoImageView;
    private EditText emailET, passwordET;
    private ProgressBar progressBar;
    private LinearLayout editProfileLayout, notificationLayout, favoritesLayout, adoptionsLayout, logoutLayout, linearLayoutUser,
            linearLayoutAdmin, addAnimalLayout, editAnimalLayout, adminLogOutLayout, linearLayoutLogin, adoptionApplication, volunteersLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        linearLayoutUser = findViewById(R.id.linearLayoutUser);
        linearLayoutAdmin = findViewById(R.id.linearLayoutAdmin);
        linearLayoutLogin = findViewById(R.id.linearLayoutLogin);
        ImageButton buttonBackToMain = findViewById(R.id.buttonBackToMain);

        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        loginButton.setOnClickListener(view -> loginUser());
        Button signInGoogleButton = findViewById(R.id.signInGoogleButton);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        setupBottomNavigation(R.id.navigation_profile);
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
        volunteersLayout = findViewById(R.id.volunteerApplicationLinearLayout);
        adminLogOutLayout = findViewById(R.id.adminLogoutLayout);
        textViewRegister.setText(Html.fromHtml("Nu ai un cont? <font color='#06D6A0'><b>Înregistrează-te</b></font>"));

        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (bottomNavigationView != null) {
                if (keypadHeight > screenHeight * 0.15) {
                    bottomNavigationView.setVisibility(View.GONE);
                } else {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

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
        adoptionApplication.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ApplicationsListActivity.class);
            intent.putExtra("selectedTab", 0);
            startActivity(intent);
        });
        volunteersLayout.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ApplicationsListActivity.class);
            intent.putExtra("selectedTab", 1);
            startActivity(intent);
        });
        adminLogOutLayout.setOnClickListener(v -> showSignOutConfirmationDialog());
    }

    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_profile;
    }

    private void resetPassword(String email) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Nu există conexiune la internet!", Toast.LENGTH_SHORT).show();
            return;
        }
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(task -> Toast.makeText(this, "Link de resetare a parolei a fost trimis!. Verificati emailul.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "A aparut o eroare. Te rugam sa mai incerci o data!", Toast.LENGTH_SHORT).show());
    }

    private void showUserInfo(String role) {
        user = FirebaseAuth.getInstance().getCurrentUser();
        UserUtils.getUserInfo(user, new UserUtils.UserInfoCallback() {
            @Override
            public void onUserInfoRetrieved(DocumentSnapshot documentSnapshot) {
                if (Constants.ROLE_ADMIN.equals(role)) {
                    adminNameTextView.setText(documentSnapshot.getString("name"));
                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    Log.d("ImageURL", "Image URL: " + imageUrl);
                    Glide.with(ProfileActivity.this)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(adminPhotoImageView);
                } else {
                    userNameTextView.setText(documentSnapshot.getString("name"));
                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    Log.d("ImageURL", "Image URL: " + imageUrl);
                    Glide.with(ProfileActivity.this)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(userPhotoImageView);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("ProfileActivity showUserInfo", "Utilizatorul nu a fost găsit în Firestore!", e);
                Toast.makeText(ProfileActivity.this, "Utilizatorul nu a fost găsit în Firestore!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signInWithGoogle() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Nu există conexiune la internet!", Toast.LENGTH_SHORT).show();
            return;
        }
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
            }).addOnFailureListener(e -> Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show());
        }
    }

    private void showSignOutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Deconectare")
                .setMessage("Ești sigur că vrei să te deconectezi?")
                .setPositiveButton("Da", (dialog, which) -> logOut())
                .setNegativeButton("Anulează", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void logOut() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Constants.KEY_USER_ROLE);
        editor.apply();

        String userId = user.getUid();
        if (isNetworkAvailable()) {
            db.collection("users")
                    .document(userId)
                    .update("fcmToken", null)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ProfileActivity", "Token FCM șters din Firestore");
                        proceedWithLogout();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileActivity", "Eroare la ștergerea token-ului FCM", e);
                        Toast.makeText(this, "Eroare la deconectare. Încearcă din nou.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Nu există conexiune la internet! Deconectare locală.", Toast.LENGTH_SHORT).show();
            proceedWithLogout();
        }
    }

    private void proceedWithLogout() {
        FirebaseAuth.getInstance().signOut();
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            FirebaseFirestore.getInstance().clearPersistence().addOnCompleteListener(clearTask -> {
                Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
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

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Nu există conexiune la internet!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveFcmToken(user.getUid());
                            isUserExist(user);
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Cont inexistent sau parolă greșită.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAndSaveUserGoogle(FirebaseUser firebaseUser, GoogleSignInAccount account) {
        if (firebaseUser == null) {
            Log.e("Firestore", "FirebaseUser este null!");
            return;
        }

        if (account == null) {
            Log.e("Firestore", "GoogleSignInAccount este null!");
            Toast.makeText(this, "Eroare la autentificare cu Google. Te rugăm să încerci din nou.", Toast.LENGTH_SHORT).show();
            logOut();
            return;
        }

        String userId = firebaseUser.getUid();
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Nu există conexiune la internet!", Toast.LENGTH_SHORT).show();
            updateUserInfo(getUserRole(), firebaseUser);
            setupBottomNavigation(R.id.navigation_profile);
            return;
        }

        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tokenTask -> {
                    if (tokenTask.isSuccessful()) {
                        String token = tokenTask.getResult();
                        if (document.exists()) {
                            db.collection("users").document(userId)
                                    .update("fcmToken", token)
                                    .addOnSuccessListener(aVoid -> Log.d("FCM", "Token actualizat pentru utilizatorul existent: " + userId))
                                    .addOnFailureListener(e -> Log.w("FCM", "Eroare la actualizarea token-ului", e));
                            checkRole(firebaseUser); // Actualizăm rolul și meniul
                        } else {
                            String photoUrl = (account.getPhotoUrl() != null) ? account.getPhotoUrl().toString() : "";
                            User user = new User(account.getDisplayName(), photoUrl, account.getEmail(), Constants.ROLE_USER);
                            user.setFcmToken(token);
                            db.collection("users").document(userId).set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "Utilizatorul a fost salvat în Firestore cu token!");
                                        checkRole(firebaseUser); // Actualizăm rolul și meniul
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Eroare la salvarea utilizatorului", e);
                                        Toast.makeText(this, "Eroare la salvarea utilizatorului!", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.w("FCM", "Eroare la obținerea token-ului", tokenTask.getException());
                        if (document.exists()) {
                            checkRole(firebaseUser); // Actualizăm rolul și meniul
                        }
                    }
                });
            } else {
                Log.e("Firestore", "Eroare la preluarea utilizatorului!", task.getException());
                Toast.makeText(this, "Eroare la verificarea utilizatorului!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserInfo(String role, FirebaseUser user) {
        if (Constants.ROLE_ADMIN.equals(role)) {
            linearLayoutAdmin.setVisibility(View.VISIBLE);
            linearLayoutUser.setVisibility(View.GONE);
            linearLayoutLogin.setVisibility(View.GONE);
        } else if (Constants.ROLE_USER.equals(role)) {
            linearLayoutLogin.setVisibility(View.GONE);
            linearLayoutUser.setVisibility(View.VISIBLE);
            linearLayoutAdmin.setVisibility(View.GONE);
            notificationLayout.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, NotificationsActivity.class)));
            favoritesLayout.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, FavoritesActivity.class)));
        }
        if (isUserLoggedInWithGoogle(user)) {
            editProfileLayout.setVisibility(View.GONE);
            Log.d("ProfileActivity", "Utilizatorul este autentificat cu Google!");
        }
        showUserInfo(role);
    }

    private void isUserExist(FirebaseUser user) {
        UserUtils.isUserExist(user, new UserUtils.UserExistCallback() {
            @Override
            public void onUserExist(boolean exists) {
                if (exists) {
                    Log.d("AnotherActivity", "Utilizatorul există!");
                    updateUserInfo(getUserRole(), user); // Actualizăm rolul și meniul
                } else {
                    Log.e("AnotherActivity", "Utilizatorul nu există sau a fost sters!");
                    Toast.makeText(ProfileActivity.this, "Utilizatorul nu există sau a fost sters!", Toast.LENGTH_SHORT).show();
                    logOut();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("AnotherActivity", "Eroare la verificarea utilizatorului", e);
            }
        });
    }

    private boolean isUserLoggedInWithGoogle(FirebaseUser user) {
        if (user != null) {
            for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkRole(FirebaseUser user) {
        UserUtils.checkUserRole(user, new UserUtils.UserRoleCallback() {
            @Override
            public void onRoleRetrieved(String role) {
                userRole = role;
                Log.d("ProfileActivity", "Rol actualizat din Firestore: " + role);
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Constants.KEY_USER_ROLE, role);
                editor.apply();
                setupBottomNavigation(R.id.navigation_profile);
            }

            @Override
            public void onError(Exception e) {
                Log.e("ProfileActivity", "Eroare la preluarea rolului", e);
            }
        });
    }
}