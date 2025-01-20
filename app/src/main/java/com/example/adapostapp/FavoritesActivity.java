package com.example.adapostapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FavoritesActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ImageButton buttonBackToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.navigation_favorites);

        buttonBackToMain.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(FavoritesActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(FavoritesActivity.this, MessagesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(FavoritesActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

    }
}