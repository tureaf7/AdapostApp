package com.example.adapostapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MessagesActivity extends AppCompatActivity {
    private Button backToMainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        backToMainButton = findViewById(R.id.buttonBackToMain);
        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(MessagesActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}