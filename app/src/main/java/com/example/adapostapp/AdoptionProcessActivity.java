package com.example.adapostapp;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AdoptionProcessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption_process);

        TextView textViewAdoptionProcess = findViewById(R.id.textViewAdoptionProcess);

        // Suport pentru formatare HTML
        textViewAdoptionProcess.setText(Html.fromHtml(getString(R.string.adoption_process_description)));

    }
}