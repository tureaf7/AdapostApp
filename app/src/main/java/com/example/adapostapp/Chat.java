package com.example.adapostapp;

import java.util.List;

public class Chat {
    private String chatId;
    private List<String> participants;

    public Chat() {}

    public Chat(String chatId, List<String> participants) {
        this.chatId = chatId;
        this.participants = participants;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
}