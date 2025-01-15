package com.example.adapostanimale;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private HorizontalScrollView horizontalScrollView;
    private LinearLayout horizontalLinearLayout;
    private Button dogsButton, catsButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        horizontalScrollView = findViewById(R.id.horizontalScrollView);
        horizontalLinearLayout = findViewById(R.id.horizontalLinearLayout);
        dogsButton = findViewById(R.id.dogsButton);
        catsButton = findViewById(R.id.catsButton);

        db = FirebaseFirestore.getInstance();

        dogsButton.setOnClickListener(v -> fetchAnimals("câini"));
        catsButton.setOnClickListener(v -> fetchAnimals("pisici"));

        // Preia toate animalele la lansare
        fetchAnimals("");
    }

    private void fetchAnimals(String species) {
        horizontalLinearLayout.removeAllViews(); // Curăță lista înainte de a adăuga altele noi

        Query query = db.collection("animale");

        if (!species.isEmpty()) {
            query = query.whereEqualTo("specie", species);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Animal> animals = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Animal animal = document.toObject(Animal.class);
                if (animal != null) {
                    animals.add(animal);
                }
            }

            for (Animal animal : animals) {
                View itemView = LayoutInflater.from(this).inflate(R.layout.image_item, horizontalLinearLayout, false);
                ImageView animalPhoto = itemView.findViewById(R.id.imageItemImageView);
                TextView animalName = itemView.findViewById(R.id.textViewName);

                animalName.setText(animal.getName());
                if (animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
                    Glide.with(this).load(animal.getPhoto()).into(animalPhoto);
                }

                horizontalLinearLayout.addView(itemView);
            }
        }).addOnFailureListener(e -> {
            Log.w("Firebase", "Error getting documents.", e);
            Toast.makeText(this, "Eroare la preluarea datelor", Toast.LENGTH_SHORT).show();
        });
    }
}