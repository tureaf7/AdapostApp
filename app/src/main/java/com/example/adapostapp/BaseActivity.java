package com.example.adapostapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adapostapp.utils.UserUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

public abstract class BaseActivity extends AppCompatActivity {
    protected BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration unreadMessagesListener;
    protected String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Inițializează userRole din SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        userRole = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_USER); // Valoare implicită "user"
        Log.d("BaseActivity", "Rol inițial din SharedPreferences: " + userRole);

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
    }

    protected abstract int getSelectedItemId();

    protected void setupBottomNavigation(int selectedItemId) {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView == null) {
            Log.e("BaseActivity", "BottomNavigationView nu a fost găsit!");
            return;
        }
        bottomNavigationView.getMenu().clear();

        if (Constants.ROLE_ADMIN.equals(userRole)) {
            bottomNavigationView.inflateMenu(R.menu.bottom_navigation_admin_menu);
            Log.d("BaseActivity", "Meniul admin a fost inflat");
        } else {
            bottomNavigationView.inflateMenu(R.menu.bottom_navigation_menu);
            Log.d("BaseActivity", "Meniul utilizator a fost inflat");
        }

        bottomNavigationView.setSelectedItemId(selectedItemId);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                intent = new Intent(this, MainActivity.class);
            } else if (itemId == R.id.navigation_favorites) {
                intent = new Intent(this, FavoritesActivity.class);
            } else if (itemId == R.id.navigation_messages) {
                intent = new Intent(this, ChatListActivity.class);
            } else if (itemId == R.id.navigation_animals) {
                intent = new Intent(this, ListAnimalActivity.class);
            } else if (itemId == R.id.navigation_profile) {
                intent = new Intent(this, ProfileActivity.class);
            } else if (itemId == R.id.navigation_notifications) {
                intent = new Intent(this, NotificationsActivity.class);
            } else if (itemId == R.id.navigation_applications) {
                intent = new Intent(this, ApplicationsListActivity.class);
            }
            if (intent != null) {
                startActivity(intent);
                return true;
            }
            return false;
        });

        updateMessagesBadge();
        Log.d("Badge", "Badge updated");
    }

    private void updateMessagesBadge() {
        Log.d("Badge", "Updating messages badge");
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            unreadMessagesListener = db.collection("users")
                    .document(userId)
                    .collection("chats")
                    .whereEqualTo("hasUnreadMessages", true)
                    .addSnapshotListener((value, error) -> {
                        Log.d("Badge", "Value: " + value + ", Error: " + error);
                        if (error != null) {
                            Log.e("Badge", "Eroare la actualizarea badge-ului", error);
                            return;
                        }
                        if (bottomNavigationView != null && value != null) {
                            if (!value.isEmpty()) {
                                BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.navigation_messages);
                                badge.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                                badge.setBadgeGravity(BadgeDrawable.TOP_END);
                                badge.setVisible(true);
                                badge.setNumber(value.size());
                                badge.setBadgeTextColor(getResources().getColor(android.R.color.white));
                            } else {
                                bottomNavigationView.removeBadge(R.id.navigation_messages);
                            }
                        }
                    });
        }
    }

    public void stopAllListeners() {
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
            unreadMessagesListener = null;
            Log.d("BaseActivity", "Listener unreadMessages oprit la logout");
        }
        if (ChatListActivity.getInstance() != null) {
            ChatListActivity.getInstance().stopListener();
            Log.d("BaseActivity", "Listener ChatListActivity oprit");
        }
        if (ChatActivity.getInstance() != null) {
            ChatActivity.getInstance().stopListeners();
            Log.d("BaseActivity", "Listener ChatActivity oprit");
        }
    }

    protected void checkUserRole(FirebaseUser user) {
        Log.d("BaseActivity", "checkUserRole");
        UserUtils.checkUserRole(user, new UserUtils.UserRoleCallback() {
            @Override
            public void onRoleRetrieved(String role) {
                userRole = role;
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Constants.KEY_USER_ROLE, role);
                editor.apply();
                Log.d("BaseActivity", "Rol actualizat din Firestore: " + role);
            }

            @Override
            public void onError(Exception e) {
                Log.e("BaseActivity", "Eroare la preluarea rolului", e);
            }
        });
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        FirebaseUser user = auth.getCurrentUser();
//        if (user != null) {
//            if (unreadMessagesListener == null) {
//                updateMessagesBadge();
//            }
//            checkUserRole(user);
//            saveFcmToken(user.getUid());
//        }
//    }

    @Override
    protected void onStop() {
        super.onStop();
        // Nu eliminăm listener-ul aici, doar la logout
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nu eliminăm listener-ul aici, doar la logout
    }

    protected void saveFcmToken(String currentUserId) {
        if (!isNetworkAvailable()) {
            Log.w("BaseActivity", "No network available, FCM token not saved");
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("BaseActivity", "Eroare la obținerea token-ului FCM", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    db.collection("users").document(currentUserId)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> Log.d("BaseActivity", "Token FCM salvat: " + token))
                            .addOnFailureListener(e -> Log.e("BaseActivity", "Eroare la salvarea token-ului FCM", e));
                });
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(userRole);
    }

    public String getUserRole() {
        return userRole;
    }

    protected boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}