package com.example.adapostapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private CircleImageView imageViewProfile;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView nameTextView;
    private String chatId, otherUserId, currentUserId;
    private ListenerRegistration messagesListener; // Pentru markMessagesAsRead
    private ListenerRegistration loadMessagesListener; // Pentru loadMessages
    private static ChatActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        instance = this;
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        ImageButton buttonSend = findViewById(R.id.buttonSend);
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        nameTextView = findViewById(R.id.textViewOtherUser);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        Log.d("ChatActivity", "Other user ID: " + otherUserId);

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }

        loadOtherUser(otherUserId);
        buttonBack.setOnClickListener(v -> finish());

        buttonSend.setOnClickListener(v -> {
            String messageText = editTextMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                if (chatId == null) {
                    createChatAndSendMessage(messageText);
                } else {
                    sendMessage(messageText);
                }
                editTextMessage.setText("");
            }
        });
    }

    private void loadOtherUser(String otherUserId) {
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User otherUser = documentSnapshot.toObject(User.class);
                        if (otherUser != null) {
                            nameTextView.setText(otherUser.getName());
                            if (!isFinishing() && !isDestroyed()) {
                                Glide.with(this)
                                        .load(otherUser.getProfileImageUrl())
                                        .error(R.drawable.ic_profile)
                                        .into(imageViewProfile);
                            }
                            Log.d("ChatActivity", "Other user name: " + otherUser.getName());
                            Log.d("ChatActivity", "Other user profile image URL: " + otherUser.getProfileImageUrl());
                        } else {
                            Log.d("ChatActivity", "Utilizatorul cu ID-ul " + otherUserId + " nu a fost găsit.");
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Eroare la preluarea utilizatorului!", e);
                    Toast.makeText(this, "Eroare la preluarea utilizatorului!", Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadMessages() {
        if (auth.getCurrentUser() == null) {
            Log.d("ChatActivity", "Utilizatorul nu este autentificat, nu inițiem listener-ul.");
            return;
        }

        loadMessagesListener = db.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("ChatActivity", "Eroare la încărcarea mesajelor!", e);
                        return;
                    }
                    messages.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Message message = doc.toObject(Message.class);
                            messages.add(message);
                        }
                        messageAdapter.notifyDataSetChanged();
                        recyclerViewMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void markMessagesAsRead() {
        if (auth.getCurrentUser() == null) return;

        messagesListener = db.collection("Chats")
                .document(chatId)
                .collection("Messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Eroare la ascultarea mesajelor necitite", error);
                        return;
                    }

                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            document.getReference().update("read", true)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Mesaj marcat ca citit: " + document.getId()))
                                    .addOnFailureListener(e -> Log.w(TAG, "Eroare la marcarea mesajului ca citit", e));
                        }

                        if (snapshot.isEmpty()) {
                            db.collection("users")
                                    .document(currentUserId)
                                    .collection("chats")
                                    .document(chatId)
                                    .update("hasUnreadMessages", false)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat marcat ca citit"))
                                    .addOnFailureListener(e -> Log.w(TAG, "Eroare la actualizarea chat-ului", e));
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }
        if (chatId != null) {
            loadMessages();
            markMessagesAsRead();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeners();
    }

    private void createChatAndSendMessage(String messageText) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }

        String newChatId = db.collection("Chats").document().getId();
        List<String> participants = Arrays.asList(currentUserId, otherUserId);
        Chat newChat = new Chat(newChatId, participants);

        db.collection("Chats").document(newChatId)
                .set(newChat)
                .addOnSuccessListener(aVoid -> {
                    chatId = newChatId;
                    sendMessage(messageText);
                    loadMessages();
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Eroare la crearea conversației!", e);
                    Toast.makeText(this, "Eroare la crearea conversației!", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendMessage(String messageText) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }

        Message message = new Message(currentUserId, otherUserId, messageText, false);

        db.collection("Chats").document(chatId)
                .collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d("ChatActivity", "Mesaj trimis cu succes!");
                    updateChatSummary(chatId, otherUserId, message, true);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Eroare la trimiterea mesajului!", e);
                    Toast.makeText(this, "Eroare la trimiterea mesajului!", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChatSummary(String chatId, String otherUserId, Message message, boolean hasUnreadMessages) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }

        ChatSummary chatSummary = new ChatSummary(chatId, currentUserId, message.getMessage(), hasUnreadMessages);
        db.collection("users")
                .document(otherUserId)
                .collection("chats")
                .document(chatId)
                .set(chatSummary, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat actualizat pentru user " + otherUserId))
                .addOnFailureListener(e -> Log.w(TAG, "Eroare la actualizarea chat-ului pentru user " + otherUserId, e));
    }

    // Metodă pentru a opri listener-ii doar la logout
    public void stopListeners() {
        if (loadMessagesListener != null) {
            loadMessagesListener.remove();
            loadMessagesListener = null;
        }
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    public static ChatActivity getInstance() {
        return instance;
    }

    static class ChatSummary {
        private String chatId;
        private String otherUserId;
        private String lastMessage;
        @ServerTimestamp
        private Timestamp timestamp;
        private boolean hasUnreadMessages;

        public ChatSummary() {
        }

        public ChatSummary(String chatId, String otherUserId, String lastMessage, boolean hasUnreadMessages) {
            this.chatId = chatId;
            this.otherUserId = otherUserId;
            this.lastMessage = lastMessage;
            this.hasUnreadMessages = hasUnreadMessages;
        }

        public String getChatId() {
            return chatId;
        }

        public String getOtherUserId() {
            return otherUserId;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public boolean isHasUnreadMessages() {
            return hasUnreadMessages;
        }
    }
}