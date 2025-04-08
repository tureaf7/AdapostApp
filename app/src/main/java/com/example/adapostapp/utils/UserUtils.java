package com.example.adapostapp.utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.adapostapp.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserUtils {

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Verifică dacă utilizatorul există
    public static void isUserExist(FirebaseUser firebaseUser, final UserExistCallback callback) {
        db.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    callback.onUserExist(documentSnapshot.exists());
                })
                .addOnFailureListener(callback::onError);
    }

    // Obține informațiile unui utilizator
    public static void getUserInfo(FirebaseUser firebaseUser, final UserInfoCallback callback) {
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
                .addOnFailureListener(callback::onError);
    }

    // Șterge un utilizator din Firestore
    public static void deleteUser(FirebaseUser user, final DeleteUserCallback callback) {
        db.collection("users").document(user.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> callback.onDeleteSuccess())
                .addOnFailureListener(callback::onDeleteFailure);
    }

    // Adauga un utilizator in Firestore
    public static void addUser(FirebaseUser firebaseUser, User user,  final AddUserCallback callback) {
        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Adăugare cu succes
                    callback.onAddSuccess();
                })
                .addOnFailureListener(callback::onAddFailure);
    }

    // Verifică rolul utilizatorului
    public static void checkUserRole(FirebaseUser user, UserRoleCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("users")
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
    public interface UserExistCallback {
        void onUserExist(boolean exists);
        void onError(Exception e);
    }

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