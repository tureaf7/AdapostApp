package com.example.adapostapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatListActivity extends AppCompatActivity implements ChatListAdapter.OnChatClickListener {
    private RecyclerView recyclerViewChatList;
    private AutoCompleteTextView searchAutoComplete;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBar;
    private ListenerRegistration chatsListener;
    private TextView textViewEmpty;
    private ChatListAdapter adapter;
    private List<Chat> chats;
    private List<User> usersList;
    private UserAdapter userAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser firebaseUser;
    private String currentUserId;
    private boolean isAdmin = false;
    private static final String PREFS_NAME = "AdapostAppPrefs";
    private static final String KEY_HAS_UNREAD_MESSAGES = "hasUnreadMessages";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.navigation_messages);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                return true;
            } else if (itemId == R.id.navigation_animals) {
                startActivity(new Intent(this, ListAnimalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        recyclerViewChatList = findViewById(R.id.recyclerViewChatList);
        searchAutoComplete = findViewById(R.id.searchAutoComplete);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        chats = new ArrayList<>();
        usersList = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            Toast.makeText(this, "Utilizatorul nu este autentificat!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            finish();
            return;
        } else {
            currentUserId = firebaseUser.getUid();
            Log.d("ChatListActivity", "Current user ID: " + currentUserId);
            saveFcmToken();
        }

        adapter = new ChatListAdapter(chats, this, currentUserId);
        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChatList.setAdapter(adapter);

        userAdapter = new UserAdapter(this, usersList);
        searchAutoComplete.setAdapter(userAdapter);
        searchAutoComplete.setThreshold(1);
        searchAutoComplete.setDropDownBackgroundResource(android.R.color.white);
        searchAutoComplete.setDropDownHeight(400);
        searchAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userAdapter.getItem(position);
            if (selectedUser != null) {
                Log.d("ChatListActivity", "Utilizator selectat: " + selectedUser.getName());
                openChat(selectedUser);
            }
        });

        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        isAdmin = "admin".equals(role);
                        setupUI();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Eroare: Utilizatorul nu există!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Eroare la verificarea rolului!", Toast.LENGTH_SHORT).show();
                    Log.e("ChatListActivity", "Eroare la verificarea rolului!", e);
                });

        updateMessagesIcon(sharedPreferences.getBoolean(KEY_HAS_UNREAD_MESSAGES, false));
    }

    private void updateMessagesIcon(boolean hasUnreadMessages) {
        int iconResId = hasUnreadMessages ? R.drawable.ic_message_red : R.drawable.ic_message;
        bottomNavigationView.getMenu().findItem(R.id.navigation_messages).setIcon(iconResId);
    }

    private void setupUI() {
        String role = isAdmin ? "user" : "admin";
        loadUsers(role);
        loadChats();
    }

    private void loadUsers(String role) {
        db.collection("users")
                .whereEqualTo("role", role)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    usersList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setId(doc.getId());
                        usersList.add(user);
                        Log.d("ChatListActivity", "Utilizator: " + user.getName());
                    }
                    userAdapter.updateUsers(usersList);
                    Log.d("ChatListActivity", "Utilizatori încărcați: " + usersList.size());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Eroare la preluarea utilizatorilor!", Toast.LENGTH_SHORT).show();
                    Log.e("ChatListActivity", "Eroare la preluarea utilizatorilor!", e);
                });
    }

    private void loadChats() {
        chatsListener = db.collection("Chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Eroare la preluarea conversațiilor!", Toast.LENGTH_SHORT).show();
                        Log.e("ChatListActivity", "Eroare la preluarea conversațiilor!", e);
                        return;
                    }

                    chats.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Chat chat = doc.toObject(Chat.class);
                        chat.setChatId(doc.getId());
                        chats.add(chat);
                    }
                    Log.d("ChatListActivity", "Number of chats: " + chats.size());
                    updateUIAfterLoad();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Oprește listener-ul pentru a evita scurgeri de memorie
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }
    }

    private void updateUIAfterLoad() {
        if (!chats.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            textViewEmpty.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        } else {
            progressBar.setVisibility(View.GONE);
            textViewEmpty.setVisibility(View.VISIBLE);
        }
    }


    private void openChat(User user) {
        db.collection("Chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Chat existingChat = null;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Chat chat = doc.toObject(Chat.class);
                        chat.setChatId(doc.getId());
                        if (chat.getParticipants().contains(user.getId())) {
                            existingChat = chat;
                            break;
                        }
                    }
                    Log.d("ChatListActivity", "Image URL: " + user.getProfileImageUrl());

                    Intent intent = new Intent(this, ChatActivity.class);
                    if (existingChat != null) {
                        intent.putExtra("chatId", existingChat.getChatId());
                        intent.putExtra("otherUserId", user.getId());
                        startActivity(intent);
                    } else {
                        intent.putExtra("chatId", (String) null); // chatId este null
                        intent.putExtra("otherUserId", user.getId());
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare la căutarea conversațiilor!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onChatClick(Chat chat) {
        String otherUserId = chat.getParticipants().get(0).equals(currentUserId) ? chat.getParticipants().get(1) : chat.getParticipants().get(0);

        // Preia profileImageUrl al celuilalt utilizator
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String photoUrl = null;
                    String name = "";
                    if (documentSnapshot.exists()) {
                        name = documentSnapshot.getString("name");
                        photoUrl = documentSnapshot.getString("profileImageUrl");
                        Log.d("ChatListActivity", "Profile image URL for user " + otherUserId + ": " + photoUrl);
                    } else {
                        Log.w("ChatListActivity", "Utilizatorul " + otherUserId + " nu există în Firestore");
                    }

                    // Creează intent-ul și trece photoUrl
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("chatId", chat.getChatId());
                    intent.putExtra("otherUserId", otherUserId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatListActivity", "Eroare la preluarea datelor utilizatorului " + otherUserId, e);
                    // În caz de eroare, trecem fără photoUrl
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("chatId", chat.getChatId());
                    intent.putExtra("otherUserId", otherUserId);
                    startActivity(intent);
                });
    }

    private void saveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("ChatListActivity", "Eroare la obținerea token-ului FCM", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    db.collection("users").document(currentUserId)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> Log.d("ChatListActivity", "Token FCM salvat: " + token))
                            .addOnFailureListener(e -> Log.e("ChatListActivity", "Eroare la salvarea token-ului FCM", e));
                });
    }
}