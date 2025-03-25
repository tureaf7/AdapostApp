package com.example.adapostapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class User {
    private String id;
    private String name;
    private String profileImageUrl;
    private String email; // Add email
    private String role;

    public User() {} // Important constructor

    public User(String name, String profileImageUrl, String email, String role) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.role = role;
    }

    public User(String id, String name, String profileImageUrl, String email, String role) {
        this.id = id;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.role = role;
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
