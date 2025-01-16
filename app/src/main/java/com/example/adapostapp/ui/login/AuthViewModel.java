package com.example.adapostapp.ui.login;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthViewModel extends AndroidViewModel {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private final MutableLiveData<FirebaseUser> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

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

    public void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        currentUser.setValue(user);
                    } else {
                        Log.e("AuthViewModel", "Login failed: " + task.getException().getMessage());
                    }
                });
    }

    public void register(String email, String password, String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserDataToFirestore(user, name);
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    // Save user data to Firestore
    private void saveUserDataToFirestore(FirebaseUser user, String name) {
        if (user != null) {
            String uid = user.getUid();
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            Log.d("AuthViewModel", "Name: " + user.getDisplayName());
            userData.put("email", user.getEmail());  // Salvare email, dacă este disponibil


            db.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> currentUser.postValue(user))
                    .addOnFailureListener(e -> handleError(e));
        }
    }

    public void logout() {
        mAuth.signOut();
        currentUser.setValue(null);
    }



    private void handleError(Exception e) {
        String errorMessage = e != null ? e.getMessage() : "A occurred an unexpected error";
        this.errorMessage.postValue(errorMessage); // Post on main thread using postValue.
        Log.e("AuthViewModel", "Error: " + errorMessage);
    }
}