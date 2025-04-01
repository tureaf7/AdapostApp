package com.example.adapostapp;


import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend, buttonBack;
    private CircleImageView imageViewProfile;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private FirebaseFirestore db;
    private TextView nameTextView;
    private String chatId, otherUserId, currentUserId, photoUrl;
    private ListenerRegistration messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);
        buttonBack = findViewById(R.id.buttonBack);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        nameTextView = findViewById(R.id.textViewOtherUser);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        Log.d("ChatActivity", "Other user ID: " + otherUserId);
        loadOtherUser(otherUserId);
        if (chatId != null) {
            loadMessages();
            markMessagesAsRead();
        }
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
                            Glide.with(this)
                                    .load(otherUser.getProfileImageUrl())
                                    .error(R.drawable.ic_profile)
                                    .into(imageViewProfile);
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

    private void loadMessages() {
        db.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("ChatActivity", "Eroare la încărcarea mesajelor!", e);
                        return;
                    }
                    messages.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Message message = doc.toObject(Message.class);
                        messages.add(message);
                    }
                    messageAdapter.notifyDataSetChanged();
                    recyclerViewMessages.scrollToPosition(messages.size() - 1);
                });
    }

    private void markMessagesAsRead() {
        // Monitorizează mesajele necitite din acest chat în timp real
        messagesListener = db.collection("Chats")
                .document(chatId)
                .collection("Messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Eroare la ascultarea mesajelor necitite", error);
                        return;
                    }

                    if (snapshot != null) {
                        // Procesează mesajele necitite
                        for (QueryDocumentSnapshot document : snapshot) {
                            // Marchează mesajul ca citit
                            document.getReference().update("isRead", true)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Mesaj marcat ca citit: " + document.getId()))
                                    .addOnFailureListener(e -> Log.w(TAG, "Eroare la marcarea mesajului ca citit", e));
                        }

                        // Verifică dacă mai există mesaje necitite
                        if (snapshot.isEmpty()) {
                            // Nu mai există mesaje necitite, actualizează users/{userId}/chats
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
    protected void onDestroy() {
        super.onDestroy();
        // Oprește listener-ul pentru a evita scurgeri de memorie
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }

    private void createChatAndSendMessage(String messageText) {
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
        Message message = new Message(currentUserId, otherUserId, messageText);
        // Nu setăm timestamp, Firestore îl va seta automat datorită @ServerTimestamp

        db.collection("Chats").document(chatId)
                .collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    // Mesajul a fost trimis
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Eroare la trimiterea mesajului!", e);
                    Toast.makeText(this, "Eroare la trimiterea mesajului!", Toast.LENGTH_SHORT).show();
                });
    }
}