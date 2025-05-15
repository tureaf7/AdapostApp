package com.example.adapostapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {
    private List<Chat> chats;
    private OnChatClickListener onChatClickListener;
    private String currentUserId;
    private Map<String, ListenerRegistration> messageListeners; // Ține evidența listener-ilor pentru mesaje
    private Map<String, ListenerRegistration> unreadMessageListeners; // Ține evidența listener-ilor pentru hasUnreadMessages

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatListAdapter(List<Chat> chats, OnChatClickListener listener, String currentUserId) {
        this.chats = chats;
        this.onChatClickListener = listener;
        this.currentUserId = currentUserId;
        this.messageListeners = new HashMap<>();
        this.unreadMessageListeners = new HashMap<>(); // Inițializează
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
        Chat chat = chats.get(position);
        String otherUserId = chat.getParticipants().get(0).equals(currentUserId) ? chat.getParticipants().get(1) : chat.getParticipants().get(0);

        // Preia numele și imaginea celuilalt participant
        FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        holder.textViewParticipant.setText(name != null ? name : "Utilizator necunoscut");
                        Glide.with(holder.itemView.getContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .into(holder.imageViewProfile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatListAdapter", "Eroare la preluarea datelor utilizatorului:", e);
                    holder.textViewParticipant.setText("Utilizator necunoscut");
                    holder.imageViewProfile.setImageResource(R.drawable.ic_profile);
                });

        // Preia ultimul mesaj în timp real
        if (chat.getChatId() != null) {
            // Oprește listener-ul anterior pentru mesaje, dacă există
            if (messageListeners.containsKey(chat.getChatId())) {
                messageListeners.get(chat.getChatId()).remove();
                messageListeners.remove(chat.getChatId());
            }

            // Adaugă un nou listener pentru ultimul mesaj
            ListenerRegistration messageListener = FirebaseFirestore.getInstance().collection("Chats").document(chat.getChatId())
                    .collection("Messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            holder.textViewLastMessage.setText("Eroare la preluarea mesajului.");
                            return;
                        }
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Message lastMessage = queryDocumentSnapshots.getDocuments().get(0).toObject(Message.class);
                            holder.textViewSender.setVisibility(lastMessage.getSenderId().equals(currentUserId) ? View.VISIBLE : View.GONE);
                            holder.textViewLastMessage.setText(lastMessage.getMessage());
                        } else {
                            holder.textViewLastMessage.setText("Niciun mesaj încă.");
                        }
                    });
            messageListeners.put(chat.getChatId(), messageListener);

            // Oprește listener-ul anterior pentru hasUnreadMessages, dacă există
            if (unreadMessageListeners.containsKey(chat.getChatId())) {
                unreadMessageListeners.get(chat.getChatId()).remove();
                unreadMessageListeners.remove(chat.getChatId());
            }

               // Verifică dacă există mesaje necitite pentru acest chat
            ListenerRegistration unreadListener = FirebaseFirestore.getInstance()
                    .collection("users").document(currentUserId)
                    .collection("chats").document(chat.getChatId())
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e("ChatListAdapter", "Eroare la verificarea mesajelor necitite pentru chat " + chat.getChatId(), e);
                            holder.imageViewUnreadMessage.setVisibility(View.GONE);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Boolean hasUnreadMessages = documentSnapshot.getBoolean("hasUnreadMessages");
                            if (hasUnreadMessages != null && hasUnreadMessages) {
                                holder.imageViewUnreadMessage.setVisibility(View.VISIBLE);
                            } else {
                                holder.imageViewUnreadMessage.setVisibility(View.GONE);
                            }
                        } else {
                            holder.imageViewUnreadMessage.setVisibility(View.GONE);
                        }
                    });
            unreadMessageListeners.put(chat.getChatId(), unreadListener);
        } else {
            holder.textViewLastMessage.setText("Începe o conversație!");
            holder.imageViewUnreadMessage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> onChatClickListener.onChatClick(chat));
    }

    @Override
    public void onViewRecycled(@NonNull ChatListViewHolder holder) {
        super.onViewRecycled(holder);
        // Oprește listener-ul pentru chat-ul care nu mai este vizibil
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            Chat chat = chats.get(position);
            if (messageListeners.containsKey(chat.getChatId())) {
                messageListeners.get(chat.getChatId()).remove();
                messageListeners.remove(chat.getChatId());
            }
            if (unreadMessageListeners.containsKey(chat.getChatId())) {
                unreadMessageListeners.get(chat.getChatId()).remove();
                unreadMessageListeners.remove(chat.getChatId());
            }
        }
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    // Oprește toți listener-ii când adapter-ul este distrus
    public void stopListening() {
        for (ListenerRegistration listener : messageListeners.values()) {
            listener.remove();
        }
        messageListeners.clear();

        for (ListenerRegistration listener : unreadMessageListeners.values()) {
            listener.remove();
        }
        unreadMessageListeners.clear();
    }

    static class ChatListViewHolder extends RecyclerView.ViewHolder {
        TextView textViewParticipant, textViewLastMessage, textViewSender;
        CircleImageView imageViewProfile;
        ImageView imageViewUnreadMessage;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfile = itemView.findViewById(R.id.imageView4);
            textViewParticipant = itemView.findViewById(R.id.textViewParticipant);
            textViewLastMessage = itemView.findViewById(R.id.textViewLastMessage);
            textViewSender = itemView.findViewById(R.id.textViewSender);
            imageViewUnreadMessage = itemView.findViewById(R.id.imageViewUnreadMessage);
        }
    }
}