package com.example.adapostapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String senderId;
    private String receiverId;
    private String message;
    @ServerTimestamp
    private Timestamp timestamp;
    private boolean isRead;

    public Message() {}

    public Message(String senderId, String receiverId, String message, boolean isRead) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.isRead = isRead;
    }


    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}