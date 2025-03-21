package com.example.adapostapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditAnimalActivity extends AppCompatActivity {

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
    private Animal animal;
    private NumberPicker numberPickerYears, numberPickerMonths;
    private int years, months;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_animals);

        String animalId = getIntent().getStringExtra("animal");

        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("animal_images");

        nameEditText = findViewById(R.id.phoneEditText);
        breedEditText = findViewById(R.id.emailEditText);
        numberPickerMonths = findViewById(R.id.numberPickerMonths);
        numberPickerYears = findViewById(R.id.numberPickerYears);
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

        numberPickerYears.setMinValue(0);
        numberPickerYears.setMaxValue(30);
        numberPickerYears.setWrapSelectorWheel(true);
        numberPickerMonths.setMinValue(0);
        numberPickerMonths.setMaxValue(11);
        numberPickerMonths.setWrapSelectorWheel(true);

        // Ascultă modificările și schimbă din nou culoarea textului}
        numberPickerYears.setOnValueChangedListener((picker, oldVal, newVal) -> {
            years = newVal;
            Log.d("Animal", "Ani: " + years + " Luni: " + months);
        });
        numberPickerMonths.setOnValueChangedListener((picker, oldVal, newVal) -> {
            months = newVal;
            Log.d("Animal", "Ani: " + years + " Luni: " + months);
        });

        if (animalId != null) {
            populateFields(animalId);
        }

        submitButton.setOnClickListener(v -> {
            if (validateFields()) {
                progressBar.setVisibility(View.VISIBLE);
                submitButton.setEnabled(false);
                updateAnimalData();
            }
        });
    }

    private void populateFields(String animalId) {
        db.collection("Animals").document(animalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        animal = documentSnapshot.toObject(Animal.class);
                        if (animal != null) {
                            Log.d("Animal", "Animalul a fost găsit");
                            Log.d("Animal", "Animalul este: " + animal.getName() + " " + animal.getId() +" " + animal.isVaccinated());
                            nameEditText.setText(animal.getName());
                            breedEditText.setText(animal.getBreed());
                            years = animal.getYears();
                            months = animal.getMonths();
                            numberPickerYears.setValue(years);
                            numberPickerMonths.setValue(months);
                            Log.d("Animal", "Ani: " + animal.getYears() + " Luni: " + animal.getMonths());

                            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                            dateTextView.setText(dateFormat.format(animal.getArrivalDate().toDate()));

                            colorEditText.setText(animal.getColor());
                            descriptionEditText.setText(animal.getDescription());
                            Glide.with(this).load(animal.getPhoto()).into(imageAnimal);
                            sterilizedSwitch.setChecked(animal.isSterilized());
                            vaccinatedSwitch.setChecked(animal.isVaccinated());

                            if (animal.getGen().equals("Mascul")) {
                                maleButton.setChecked(true);
                            } else if (animal.getGen().equals("Femela")) {
                                femaleButton.setChecked(true);
                            }
                            if (animal.getSpecies().equals("Câine")) {
                                dogButton.setChecked(true);
                            } else if (animal.getSpecies().equals("Pisică")) {
                                catButton.setChecked(true);
                            }
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la obținerea detaliilor animalului:");
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
        return true;
    }


    private void deleteOldImage() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String imageUrl = animal.getPhoto();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Uri uri = Uri.parse(imageUrl);
            String fileName = uri.getLastPathSegment(); // Obținem doar numele fișierului
            StorageReference imageRef = storage.getReference().child(fileName);

            imageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Imaginea a fost ștearsă cu succes.");
                        updateAnimalData();
                    })
                    .addOnFailureListener(e -> Log.w("Firebase", "Eroare la ștergerea imaginii.", e));
        }

    }


    private Timestamp parseDate(String date) {
        try {
            Date parsedDate = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).parse(date);
            return new Timestamp(parsedDate);
        } catch (ParseException e) {
            return null;
        }
    }

    private void updateAnimalData() {
        String name = nameEditText.getText().toString();
        String speciesSelected = getSelectedRadioText(speciesGroup);
        String gender = getSelectedRadioText(genGroup);
        String breed = breedEditText.getText().toString();
//        int age = Integer.parseInt(ageEditText.getText().toString());

        Timestamp arrivalDate = parseDate(dateTextView.getText().toString());
        Log.d("Timestamp", "Arrival date: " + arrivalDate);
        Log.d("Timestamp", "Arrival date: " + dateTextView.getText().toString());
        String color = colorEditText.getText().toString();
        String description = descriptionEditText.getText().toString();
        boolean sterilized = getSelectedSwitchText(sterilizedSwitch);
        boolean vaccinated = getSelectedSwitchText(vaccinatedSwitch);

        if (arrivalDate == null) {
            Toast.makeText(this, "Dată invalidă!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", name);
        updateData.put("species", speciesSelected);
        updateData.put("gen", gender);
        updateData.put("breed", breed);
        updateData.put("years", years);
        updateData.put("months", months);
        updateData.put("arrivalDate", arrivalDate);
        updateData.put("color", color);
        updateData.put("description", description);
        updateData.put("sterilized", sterilized);
        updateData.put("vaccinated", vaccinated);
        updateData.put("photo", animal.getPhoto());

        Log.d("Varsta", "Ani: " + years + " Luni: " + months);

        if (imageUri != null) {
            deleteOldImage();
            // Dacă imaginea a fost schimbată, o încărcăm și actualizăm URL-ul
            uploadImageAndUpdateAnimal(updateData);
        } else {
            // Actualizăm direct în Firestore dacă imaginea nu s-a schimbat
            db.collection("Animals").document(animal.getId())
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Datele animalului au fost actualizate!", Toast.LENGTH_SHORT).show();
                        finish(); // Închide activitatea
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Eroare la actualizare: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void uploadImageAndUpdateAnimal(Map<String, Object> updateData) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("animal_images/" + fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateData.put("photo", uri.toString());
                    db.collection("Animals").document(animal.getId())
                            .update(updateData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Datele animalului au fost actualizate!", Toast.LENGTH_SHORT).show();
                                finish(); // Închide activitatea
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Eroare la actualizare: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare la încărcarea imaginii: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearFields() {
        nameEditText.setText("");
        breedEditText.setText("");
        dateTextView.setText("");
        numberPickerYears.setValue(0);
        numberPickerMonths.setValue(0);
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
