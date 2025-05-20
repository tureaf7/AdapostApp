package com.example.adapostapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import android.widget.Spinner;
import androidx.appcompat.widget.SwitchCompat;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AddAnimalActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText nameEditText, breedEditText, colorEditText, descriptionEditText;
    private ProgressBar progressBar;
    private RadioGroup speciesGroup, genGroup;
    private RadioButton dogButton, catButton, maleButton, femaleButton;
    private FirebaseFirestore db;
    private TextView dateTextView;
    private Button submitButton, buttonPickDate, buttonSelectImage;
    private ImageView imageAnimal;
    private ImageButton buttonBackToMain;
    private SwitchCompat sterilizedSwitch, vaccinatedSwitch;
    private Uri imageUri;
    private StorageReference storageReference;
    private NumberPicker numberPickerYears, numberPickerMonths;
    private int years, months;
    private Spinner spinnerFilter;
    private String animalSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_animal);

        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("animal_images");

        nameEditText = findViewById(R.id.phoneEditText);
        breedEditText = findViewById(R.id.emailEditText);
        colorEditText = findViewById(R.id.colorEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        progressBar = findViewById(R.id.progressBar);
        buttonPickDate = findViewById(R.id.buttonPickDate);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        imageAnimal = findViewById(R.id.imageProfile);
        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        dateTextView = findViewById(R.id.dateTextView);
        speciesGroup = findViewById(R.id.speciesGroup);
        genGroup = findViewById(R.id.genGroup);
        sterilizedSwitch = findViewById(R.id.sterilizedSwitch);
        vaccinatedSwitch = findViewById(R.id.vaccinatedSwitch);
        submitButton = findViewById(R.id.submitButton);
        numberPickerYears = findViewById(R.id.numberPickerYears);
        numberPickerMonths = findViewById(R.id.numberPickerMonths);
        spinnerFilter = findViewById(R.id.spinner);

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("Mică");
        filterOptions.add("Medie");
        filterOptions.add("Mare");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterOptions);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                animalSize = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        numberPickerYears.setMinValue(0);
        numberPickerYears.setMaxValue(30);
        numberPickerYears.setWrapSelectorWheel(true);
        numberPickerMonths.setMinValue(0);
        numberPickerMonths.setMaxValue(11);
        numberPickerMonths.setWrapSelectorWheel(true);

        // Ascultă modificările și schimbă din nou culoarea textului}
        numberPickerYears.setOnValueChangedListener((picker, oldVal, newVal) -> {
            years = newVal;
        });
        numberPickerMonths.setOnValueChangedListener((picker, oldVal, newVal) -> {
            months = newVal;
        });


        catButton = findViewById(R.id.radioButtonPisica);
        dogButton = findViewById(R.id.radioButtonCaine);
        maleButton = findViewById(R.id.radioButtonMale);
        femaleButton = findViewById(R.id.radioButtonFemale);

        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);

        buttonSelectImage.setOnClickListener(v -> openImageChooser());
        imageAnimal.setOnClickListener(v -> openImageChooser());
        buttonPickDate.setOnClickListener(v -> showDatePicker());
        buttonBackToMain.setOnClickListener(v -> onBackPressed());

        submitButton.setOnClickListener(v -> {
            if (validateFields()) {
                saveAnimalData();
            }
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year1, monthOfYear, dayOfMonth);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            dateTextView.setText(dateFormat.format(selectedDate.getTime()));
        }, year, month, day).show();
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selectați imaginea"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageAnimal.setImageURI(imageUri);
        }
    }

    private boolean validateFields() {
        if (TextUtils.isEmpty(nameEditText.getText().toString())) {
            nameEditText.setError("Introduceți numele animalului");
            return false;
        }
        if (TextUtils.isEmpty(breedEditText.getText().toString())) {
            breedEditText.setError("Introduceți rasa animalului");
            return false;
        }
        if (TextUtils.isEmpty(dateTextView.getText().toString())) {
            dateTextView.setError("Introduceți data de sosire");
            return false;
        }
        if (TextUtils.isEmpty(colorEditText.getText().toString())) {
            colorEditText.setError("Introduceți culoarea animalului");
            return false;
        }
        if (TextUtils.isEmpty(descriptionEditText.getText().toString())) {
            descriptionEditText.setError("Introduceți descrierea animalului");
            return false;
        }
        if (imageUri == null) {
            Toast.makeText(this, "Selectați o imagine!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getSelectedRadioText(speciesGroup).isEmpty()) {
            Toast.makeText(this, "Selectați tipul animalului!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getSelectedRadioText(genGroup).isEmpty()) {
            Toast.makeText(this, "Selectați genul animalului!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveAnimalData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Autentificați-vă pentru a adăuga un animal!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        if (imageUri != null) {
            uploadImageAndSaveAnimal();
        } else {
            saveAnimalToFirestore("");
        }
    }

    private void uploadImageAndSaveAnimal() {
        StorageReference fileRef = storageReference.child(UUID.randomUUID().toString() + ".jpg");
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            saveAnimalToFirestore(uri.toString());
                        }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(this, "Eroare la încărcarea imaginii!", Toast.LENGTH_SHORT).show();
                });
    }

    private Timestamp parseDate(String date) {
        try {
            Date parsedDate = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).parse(date);
            return new Timestamp(parsedDate);
        } catch (ParseException e) {
            return null;
        }
    }

    private void saveAnimalToFirestore(String imageUrl) {
        String name = nameEditText.getText().toString();
        String gen = getSelectedRadioText(genGroup);
        String speciesSelected = getSelectedRadioText(speciesGroup);
        boolean isSterilized = getSelectedSwitchText(sterilizedSwitch);
        boolean isVaccinated = getSelectedSwitchText(vaccinatedSwitch);
        String color = colorEditText.getText().toString();
        String description = descriptionEditText.getText().toString();
        String breed = breedEditText.getText().toString();

        Timestamp arrivalDate = parseDate(dateTextView.getText().toString());

        if (arrivalDate == null) {
            Toast.makeText(this, "Dată invalidă!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creează un obiect Animal fără ID
        Animal animal = new Animal(name, gen, speciesSelected, isSterilized, isVaccinated, color, description, breed, years, months, arrivalDate, false, imageUrl, animalSize);

        // Adaugă animalul în Firestore
        db.collection("Animals").add(animal)
                .addOnSuccessListener(documentReference -> {
                    // Setează ID-ul documentului în obiectul Animal
                    String documentId = documentReference.getId();
                    animal.setId(documentId);  // Setează câmpul `id` cu ID-ul generat de Firestore

                    // Actualizează documentul cu ID-ul în Firestore
                    db.collection("Animals").document(documentId).set(animal)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Animal adăugat cu succes!", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                submitButton.setEnabled(true);
                                clearFields();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void clearFields() {
        nameEditText.setText("");
        breedEditText.setText("");
        numberPickerMonths.setValue(0);
        numberPickerYears.setValue(0);
        dateTextView.setText("");
        colorEditText.setText("");
        descriptionEditText.setText("");
        dogButton.setChecked(false);
        catButton.setChecked(false);
        maleButton.setChecked(false);
        femaleButton.setChecked(false);
        sterilizedSwitch.setChecked(false);
        vaccinatedSwitch.setChecked(false);
        imageAnimal.setImageResource(R.drawable.ic_launcher_foreground); // Placeholder
    }

    private String getSelectedRadioText(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        View radioButton = radioGroup.findViewById(selectedId);
        int index = radioGroup.indexOfChild(radioButton);
        return ((TextView) radioGroup.getChildAt(index)).getText().toString();
    }

    private boolean getSelectedSwitchText(SwitchCompat switchCompat) {
        return switchCompat.isChecked();
    }
}
