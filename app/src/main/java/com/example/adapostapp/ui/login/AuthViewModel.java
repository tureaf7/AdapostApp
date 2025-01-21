package com.example.adapostapp.ui.login;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthViewModel extends AndroidViewModel {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private final MutableLiveData<FirebaseUser> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUser.setValue(user);
        }
    }

    public LiveData<FirebaseUser> getCurrentUser() {
        return currentUser;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        currentUser.setValue(user);
                        successMessage.postValue("Login successful!");
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    public void register(String email, String password, String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Actualizează utilizatorul curent
                            currentUser.setValue(user);

                            // Salvează datele utilizatorului în Firestore
                            saveUserDataToFirestore(user, name);

                            // Postează mesajul de succes
                            successMessage.postValue("Registration successful and user logged in!");
                        }
                    } else {
                        handleError(task.getException());
                    }
                });
    }


    private void saveUserDataToFirestore(FirebaseUser user, String name) {
        // Verificare dacă utilizatorul este autentificat
        if (user != null) {
            String uid = user.getUid();
            String displayName = name != null ? name : user.getDisplayName();
            int[] favorite = new int[0];
            // Datele utilizatorului
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", displayName);
            userData.put("email", user.getEmail());
            userData.put("uid", uid);

            // Salvare în Firestore
            db.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("AuthViewModel", "User data saved successfully in Firestore.");
                        successMessage.postValue("User data saved successfully!");
                    })
                    .addOnFailureListener(this::handleError);
        }
    }

    private void handleError(Exception e) {
        String errorMessage = e != null ? e.getLocalizedMessage() : "Unexpected error occurred. Please try again.";
        this.errorMessage.postValue(errorMessage);
        Log.e("AuthViewModel", "Error: " + errorMessage);
    }



}
