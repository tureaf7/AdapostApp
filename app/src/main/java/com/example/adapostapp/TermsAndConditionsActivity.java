package com.example.adapostapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TermsAndConditionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");

        if (title == null) {
            Toast.makeText(this, "Eroare: titlul nu a fost specificat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (title.equals("adoption")) {
            setContentView(R.layout.activity_terms_and_conditions_adoption);
        } else if (title.equals("volunteer")) {
            setContentView(R.layout.activity_terms_and_conditions_volunteer);
        } else {
            Toast.makeText(this, "Eroare: titlul nu a fost specificat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView textViewTermsAndConditions = findViewById(R.id.textViewTermsAndConditions);
        textViewTermsAndConditions.setText(Html.fromHtml(getString(R.string.adoption_process_description)));

        ImageButton backButton = findViewById(R.id.buttonBackToMain);
        backButton.setOnClickListener(v -> finish());
    }
}