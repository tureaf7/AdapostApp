package com.example.adapostapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditAnimalActivity extends AppCompatActivity {

    private static final int PICK_MAIN_IMAGE_REQUEST = 1;
    private static final int PICK_MORE_IMAGES_REQUEST = 2;
    private EditText nameEditText, breedEditText, colorEditText, descriptionEditText;
    private ProgressBar progressBar;
    private RadioGroup speciesGroup, genGroup;
    private RadioButton dogButton, catButton, maleButton, femaleButton;
    private FirebaseFirestore db;
    private TextView dateTextView;
    private Button submitButton, buttonPickDate, buttonSelectMainImage, buttonAddMorePhotos;
    private ImageView imageAnimal;
    private ImageButton buttonBackToMain;
    private SwitchCompat sterilizedSwitch, vaccinatedSwitch;
    private Uri mainImageUri;
    private List<Uri> moreImageUris = new ArrayList<>();
    private StorageReference storageReference;
    private Animal animal;
    private LinearLayout additionalPhotosLayout;
    private NumberPicker numberPickerYears, numberPickerMonths;
    private int years, months;
    private String animalSize;
    private ActivityResultLauncher<String> pickMainImage;
    private ActivityResultLauncher<String> pickMoreImages;
    private ArrayAdapter<String> spinnerAdapter;
    private Spinner spinnerFilter;

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
        buttonSelectMainImage = findViewById(R.id.buttonSelectMainImage);
        buttonAddMorePhotos = findViewById(R.id.buttonAddMorePhotos);
        imageAnimal = findViewById(R.id.imageProfile);
        buttonBackToMain = findViewById(R.id.buttonBackToMain);
        dateTextView = findViewById(R.id.dateTextView);
        speciesGroup = findViewById(R.id.speciesGroup);
        genGroup = findViewById(R.id.genGroup);
        sterilizedSwitch = findViewById(R.id.sterilizedSwitch);
        vaccinatedSwitch = findViewById(R.id.vaccinatedSwitch);
        submitButton = findViewById(R.id.submitButton);
        additionalPhotosLayout = findViewById(R.id.additionalPhotosLayout);

        catButton = findViewById(R.id.radioButtonPisica);
        dogButton = findViewById(R.id.radioButtonCaine);
        maleButton = findViewById(R.id.radioButtonMale);
        femaleButton = findViewById(R.id.radioButtonFemale);

        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);

        // Launcher pentru poza principală
        pickMainImage = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        mainImageUri = uri;
                        imageAnimal.setImageURI(uri);
                    }
                });

        // Launcher pentru poze suplimentare
        pickMoreImages = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        moreImageUris.addAll(uris);
                        displayAdditionalPhotos();
                    }
                });

        buttonSelectMainImage.setOnClickListener(v -> pickMainImage.launch("image/*"));
        imageAnimal.setOnClickListener(v -> pickMainImage.launch("image/*"));
        buttonPickDate.setOnClickListener(v -> showDatePicker());
        buttonBackToMain.setOnClickListener(v -> onBackPressed());
        buttonAddMorePhotos.setOnClickListener(v -> pickMoreImages.launch("image/*"));

        numberPickerYears.setMinValue(0);
        numberPickerYears.setMaxValue(30);
        numberPickerYears.setWrapSelectorWheel(true);
        numberPickerMonths.setMinValue(0);
        numberPickerMonths.setMaxValue(11);
        numberPickerMonths.setWrapSelectorWheel(true);

        numberPickerYears.setOnValueChangedListener((picker, oldVal, newVal) -> {
            years = newVal;
            Log.d("Animal", "Ani: " + years + " Luni: " + months);
        });
        numberPickerMonths.setOnValueChangedListener((picker, oldVal, newVal) -> {
            months = newVal;
            Log.d("Animal", "Ani: " + years + " Luni: " + months);
        });

        spinnerFilter = findViewById(R.id.spinner);

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("Mică");
        filterOptions.add("Medie");
        filterOptions.add("Mare");

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterOptions);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                animalSize = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                animalSize = "Medie"; // Valoare implicită dacă nu se selectează nimic
            }
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
                            Log.d("Animal", "Animalul a fost găsit: " + animal.getName());
                            nameEditText.setText(animal.getName());
                            breedEditText.setText(animal.getBreed());
                            years = animal.getYears();
                            months = animal.getMonths();
                            numberPickerYears.setValue(years);
                            numberPickerMonths.setValue(months);

                            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                            dateTextView.setText(dateFormat.format(animal.getArrivalDate().toDate()));

                            colorEditText.setText(animal.getColor());
                            descriptionEditText.setText(animal.getDescription());
                            Glide.with(this).load(animal.getPhoto()).into(imageAnimal);
                            sterilizedSwitch.setChecked(animal.isSterilized());
                            vaccinatedSwitch.setChecked(animal.isVaccinated());

                            if ("Mascul".equals(animal.getGen())) {
                                maleButton.setChecked(true);
                            } else if ("Femela".equals(animal.getGen())) {
                                femaleButton.setChecked(true);
                            }
                            if ("Câine".equals(animal.getSpecies())) {
                                dogButton.setChecked(true);
                            } else if ("Pisică".equals(animal.getSpecies())) {
                                catButton.setChecked(true);
                            }

                            // Populează Spinner-ul cu dimensiunea animalului
                            String size = animal.getAnimalSize(); // Asumăm că Animal are un getter getSize()
                            if (size != null) {
                                int position = spinnerAdapter.getPosition(size);
                                if (position >= 0) {
                                    spinnerFilter.setSelection(position); // Setează poziția în Spinner
                                } else {
                                    spinnerFilter.setSelection(1); // Valoare implicită "Medie" dacă nu se găsește
                                    animalSize = "Medie"; // Setează valoarea implicită
                                }
                            } else {
                                spinnerFilter.setSelection(1); // Valoare implicită "Medie" dacă size este null
                                animalSize = "Medie";
                            }

                            // Afișează pozele suplimentare existente
                            if (animal.getMorePhotos() != null && !animal.getMorePhotos().isEmpty()) {
                                for (String photoUrl : animal.getMorePhotos()) {
                                    addPhotoPreview(photoUrl);
                                }
                            }
                        }
                    }
                }).addOnFailureListener(e -> Log.e("Firestore", "Eroare la obținerea detaliilor animalului: " + e.getMessage()));
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

    private void displayAdditionalPhotos() {
        additionalPhotosLayout.removeAllViews();
        for (Uri uri : moreImageUris) {
            addPhotoPreview(uri);
        }
    }

    private void addPhotoPreview(Uri uri) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(uri);
        additionalPhotosLayout.addView(imageView);
    }

    private void addPhotoPreview(String url) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(url).into(imageView);
        additionalPhotosLayout.addView(imageView);
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
        if (animal != null && animal.getPhoto() != null && !animal.getPhoto().isEmpty()) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(animal.getPhoto());
            imageRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Imaginea a fost ștearsă cu succes."))
                    .addOnFailureListener(e -> Log.w("Firebase", "Eroare la ștergerea imaginii: " + e.getMessage()));
        }
    }

    private Timestamp parseDate(String date) {
        try {
            Date parsedDate = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).parse(date);
            return new Timestamp(parsedDate);
        } catch (ParseException e) {
            Log.e("DateParse", "Eroare la parsarea datei: " + e.getMessage());
            return null;
        }
    }

    private void updateAnimalData() {
        if (animal == null) return;

        String name = nameEditText.getText().toString();
        String speciesSelected = getSelectedRadioText(speciesGroup);
        String gender = getSelectedRadioText(genGroup);
        String breed = breedEditText.getText().toString();
        Timestamp arrivalDate = parseDate(dateTextView.getText().toString());
        String color = colorEditText.getText().toString();
        String description = descriptionEditText.getText().toString();
        boolean sterilized = sterilizedSwitch.isChecked();
        boolean vaccinated = vaccinatedSwitch.isChecked();

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
        updateData.put("animalSize", animalSize);

        if (mainImageUri != null) {
            deleteOldImage();
            uploadMainImageAndUpdateAnimal(updateData);
        } else {
            updateData.put("photo", animal.getPhoto());
            uploadMorePhotosAndUpdateAnimal(updateData);
        }
    }

    private void uploadMainImageAndUpdateAnimal(Map<String, Object> updateData) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storageReference.child(fileName);

        storageRef.putFile(mainImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateData.put("photo", uri.toString());
                    uploadMorePhotosAndUpdateAnimal(updateData);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la încărcarea pozei principale: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                });
    }

    private void uploadMorePhotosAndUpdateAnimal(Map<String, Object> updateData) {
        if (moreImageUris.isEmpty()) {
            updateAnimalInFirestore(updateData);
            return;
        }

        List<String> newPhotoUrls = new ArrayList<>();
        for (int i = 0; i < moreImageUris.size(); i++) {
            String fileName = UUID.randomUUID().toString() + ".jpg";
            StorageReference photoRef = storageReference.child(fileName);

            int finalI = i;
            photoRef.putFile(moreImageUris.get(i))
                    .addOnSuccessListener(taskSnapshot -> photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        newPhotoUrls.add(uri.toString());
                        if (finalI == moreImageUris.size() - 1) {
                            updateData.put("morePhotos", newPhotoUrls);
                            updateAnimalInFirestore(updateData);
                        }
                    }))
                    .addOnFailureListener(e -> Log.e("Firebase", "Eroare la încărcarea pozei suplimentare: " + e.getMessage()));
        }
    }

    private void updateAnimalInFirestore(Map<String, Object> updateData) {
        db.collection("Animals").document(animal.getId())
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Datele animalului au fost actualizate!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la actualizare: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                });
    }

    private String getSelectedRadioText(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        RadioButton radioButton = findViewById(selectedId);
        return radioButton.getText().toString();
    }

    private boolean getSelectedSwitchText(SwitchCompat switchCompat) {
        return switchCompat.isChecked();
    }
}