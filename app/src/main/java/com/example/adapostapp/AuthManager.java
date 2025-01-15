package com.example.adapostapp;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthManager {

    private static final int RC_SIGN_IN = 9001; //Un cod unic pentru intentul de sign in

    private final GoogleSignInClient mGoogleSignInClient;
    private final FirebaseAuth mAuth;
    private final Activity activity;

    public interface AuthCallback {
        void onSignInSuccess(FirebaseUser user);
        void onSignInFailure(String message);
    }

    private AuthCallback authCallback;

    public AuthManager(Activity activity, AuthCallback authCallback) {
        this.activity = activity;
        this.authCallback = authCallback;

        // Initializare Google Sign-in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
        mAuth = FirebaseAuth.getInstance();

    }

    public void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleSignInResult(int requestCode, int resultCode, Intent data){
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInTask(task);
        }
    }

    private void handleSignInTask(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            firebaseAuthWithGoogle(account.getIdToken());

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.e("AuthManager", "signInResult:failed code=" + e.getStatusCode());
            authCallback.onSignInFailure("signInResult:failed code=" + e.getStatusCode());
        }
    }
    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        if(user != null){
                            Log.d("AuthManager","Succesfully signed in as: "+ user.getUid());
                            authCallback.onSignInSuccess(user);
                        }

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.e("AuthManager","Autentification failed");
                        authCallback.onSignInFailure("Autentification failed");
                        Toast.makeText(activity, "Authentication Failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public FirebaseAuth getFirebaseAuth(){
        return mAuth;
    }
}