package com.example.adapostapp;

public class UserData {
    private String name;
    private String profileImageUrl;
    private String email; // Add email

    public UserData() {} // Important constructor


    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getEmail() {return email; }
    public void setEmail(String email) {this.email = email; }
}
