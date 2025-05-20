package com.example.adapostapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.adapostapp.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserUtils {

    // Verifică dacă utilizatorul există
    public static void isUserExist(String userId, OnUserCheckListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Obține o instanță nouă
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    listener.onResult(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    Log.e("UserUtils", "Eroare la verificarea utilizatorului: " + e.getMessage(), e);
                    if (e.getMessage().contains("The client has already been terminated")) {
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot2 -> listener.onResult(documentSnapshot2.exists()))
                                .addOnFailureListener(e2 -> listener.onResult(false));
                    } else {
                        listener.onResult(false);
                    }
                });
    }

    public interface OnUserCheckListener {
        void onResult(boolean exists);
    }

    // Obține informațiile unui utilizator
    public static void getUserInfo(FirebaseUser firebaseUser, final UserInfoCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Obține o instanță nouă
        db.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onUserInfoRetrieved(documentSnapshot);
                    } else {
                        callback.onError(new Exception("Utilizatorul nu a fost găsit"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserUtils", "Eroare la obținerea informațiilor utilizatorului: " + e.getMessage(), e);
                    if (e.getMessage().contains("The client has already been terminated")) {
                        db.collection("users")
                                .document(firebaseUser.getUid())
                                .get()
                                .addOnSuccessListener(callback::onUserInfoRetrieved)
                                .addOnFailureListener(callback::onError);
                    } else {
                        callback.onError(e);
                    }
                });
    }

    // Șterge un utilizator din Firestore
    public static void deleteUser(FirebaseUser user, final DeleteUserCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> callback.onDeleteSuccess())
                .addOnFailureListener(callback::onDeleteFailure);
    }

    // Adaugă un utilizator în Firestore
    public static void addUser(FirebaseUser firebaseUser, User user, final AddUserCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onAddSuccess())
                .addOnFailureListener(callback::onAddFailure);
    }

    // Verifică rolul utilizatorului
    public static void checkUserRole(FirebaseUser user, UserRoleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if (role != null) {
                        callback.onRoleRetrieved(role);
                    } else {
                        callback.onError(new Exception("Rolul nu a fost găsit."));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public static String getSavedRole(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("user_role", null);
    }

    // Callback-uri pentru fiecare funcție
    public interface UserInfoCallback {
        void onUserInfoRetrieved(DocumentSnapshot documentSnapshot);
        void onError(Exception e);
    }

    public interface DeleteUserCallback {
        void onDeleteSuccess();
        void onDeleteFailure(Exception e);
    }

    public interface UserRoleCallback {
        void onRoleRetrieved(String role);
        void onError(Exception e);
    }

    public interface AddUserCallback {
        void onAddSuccess();
        void onAddFailure(Exception e);
    }
}