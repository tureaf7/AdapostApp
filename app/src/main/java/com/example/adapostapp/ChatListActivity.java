package com.example.adapostapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
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

public class ChatListActivity extends BaseActivity implements ChatListAdapter.OnChatClickListener {
    private RecyclerView recyclerViewChatList;
    private AutoCompleteTextView searchAutoComplete;
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
    private static ChatListActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        instance = this;
        recyclerViewChatList = findViewById(R.id.recyclerViewChatList);
        searchAutoComplete = findViewById(R.id.searchAutoComplete);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        ImageButton buttonBackToMain = findViewById(R.id.buttonBackToMain);
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
            saveFcmToken(currentUserId);
        }

        adapter = new ChatListAdapter(chats, this, currentUserId);
        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChatList.setAdapter(adapter);

        userAdapter = new UserAdapter(this, usersList);
        searchAutoComplete.setAdapter(userAdapter);
        searchAutoComplete.setThreshold(0); // Afișăm dropdown-ul chiar și fără text
        searchAutoComplete.setDropDownBackgroundResource(android.R.color.white);

        // Afișăm dropdown-ul la apăsare
        searchAutoComplete.setOnTouchListener((v, event) -> {
            searchAutoComplete.showDropDown(); // Forțează afișarea dropdown-ului
            return false;
        });

        searchAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userAdapter.getItem(position);
            if (selectedUser != null) {
                Log.d("ChatListActivity", "Utilizator selectat: " + selectedUser.getName());
                openChat(selectedUser);
                searchAutoComplete.setText(""); // Resetăm textul după selecție
            }
        });

        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        setupUI();
    }

    @Override
    protected int getSelectedItemId() {
        return R.id.navigation_messages;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListener();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            setupUI();
        } else {
            Toast.makeText(this, "Utilizatorul nu este autentificat!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            finish();
        }
        setupBottomNavigation(R.id.navigation_messages);
    }

    private void setupUI() {
        String role = isAdmin() ? "user" : "admin";
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
                        if (!user.getId().equals(currentUserId)) { // Excludem utilizatorul curent
                            usersList.add(user);
                        }
                        Log.d("ChatListActivity", "Utilizator: " + user.getName());
                    }
                    userAdapter.updateUsers(usersList);
                    Log.d("ChatListActivity", "Utilizatori încărcați: " + usersList.size());
                    // Afișăm dropdown-ul după încărcarea utilizatorilor
                    if (!usersList.isEmpty()) {
                        searchAutoComplete.showDropDown();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Eroare la preluarea utilizatorilor!", Toast.LENGTH_SHORT).show();
                    Log.e("ChatListActivity", "Eroare la preluarea utilizatorilor!", e);
                });
    }

    private void loadChats() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.d("ChatListActivity", "Utilizatorul nu este autentificat, nu inițiem listener-ul.");
            return; // Nu inițializăm listener-ul dacă utilizatorul nu este autentificat
        }

        currentUserId = user.getUid();
        Log.d("ChatListActivity---------", "Current user ID: " + user.getUid());
        chatsListener = db.collection("Chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Eroare la preluarea conversațiilor!", Toast.LENGTH_SHORT).show();
                        Log.e("ChatListActivity", "Eroare la preluarea conversațiilor!", e);
                        return;
                    }

                    Log.d("ChatListActivity", "Preluare conversații (listener pornit)");
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

    // Metodă pentru a opri listener-ul la logout
    public void stopListener() {
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
            Log.d("ChatListActivity", "Listener ChatListActivity oprit");
        }
        if (adapter != null) {
            adapter.stopListening(); // Oprește listenerii din adapter
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

    public static ChatListActivity getInstance() {
        return instance;
    }
}