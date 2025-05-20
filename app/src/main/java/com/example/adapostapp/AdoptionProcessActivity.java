package com.example.adapostapp;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AdoptionProcessActivity extends AppCompatActivity {
private ImageButton buttonBackToMain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_process);

        TextView textViewAdoptionProcess = findViewById(R.id.textViewAdoptionProcess);

        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        // Suport pentru formatare HTML
        textViewAdoptionProcess.setText(Html.fromHtml(getString(R.string.adoption_process_description)));
    }
}