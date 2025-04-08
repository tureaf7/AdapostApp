package com.example.adapostapp;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages;
    private String currentUserId;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.textViewMessage.setText(message.getMessage());

        // Afișează timestamp-ul într-un format lizibil
        Timestamp timestamp = message.getTimestamp();
        if (timestamp != null) {
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.textViewTimestamp.setText(sdf.format(date));
            holder.textViewTimestamp.setVisibility(View.GONE);
        } else {
            holder.textViewTimestamp.setText("Se trimite...");
            holder.textViewTimestamp.setVisibility(View.VISIBLE);
        }

        // LayoutParams pentru CardView (în LinearLayout)
        LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) holder.cardView.getLayoutParams();
        // LayoutParams pentru textViewTimestamp (în LinearLayout)
        LinearLayout.LayoutParams timeParams = (LinearLayout.LayoutParams) holder.textViewTimestamp.getLayoutParams();

        if (message.getSenderId().equals(currentUserId)) {
            holder.textViewMessage.setBackgroundResource(android.R.color.holo_blue_light);
            cardParams.gravity = Gravity.END; // Mută CardView-ul la dreapta
            timeParams.gravity = Gravity.END; // Mută timestamp-ul la dreapta
        } else {
            holder.textViewMessage.setBackgroundResource(android.R.color.darker_gray);
            cardParams.gravity = Gravity.START; // Mută CardView-ul la stânga
            timeParams.gravity = Gravity.START; // Mută timestamp-ul la stânga
        }

        holder.cardView.setLayoutParams(cardParams);
        holder.textViewTimestamp.setLayoutParams(timeParams);

        holder.itemView.setOnClickListener(v -> {
            // Comută vizibilitatea timestamp-ului
            if (holder.textViewTimestamp.getVisibility() == View.VISIBLE) {
                holder.textViewTimestamp.setVisibility(View.GONE);
            } else {
                holder.textViewTimestamp.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage, textViewTimestamp;
        CardView cardView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            cardView = itemView.findViewById(R.id.messageCardView); // ID-ul implicit al CardView
        }
    }
}