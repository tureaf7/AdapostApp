package com.example.adapostapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdoptionActivity extends AppCompatActivity {
    private EditText numar_telefon, adresa, animale_precedente_detalii, animal_curent_specie, animal_curent_varsta,
            animal_curent_temperament, grija_animal, grija_animal_vacanta, animal_probleme_sanatate, mesaj_adapost;
    private RadioGroup animale_precedente, animal_curent, animale_adoptate, locuinta, chirie, permisiune_proprietare_chirie, alergie;
    private ImageButton button_back_to_main;
    private Button button_submit;
    private CheckBox termeni_conditii;
    private TextView error_message, error_message2, error_message3, error_message4, error_message5,
            error_message6, error_message7;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adoption);

        numar_telefon = findViewById(R.id.phone_number);
        adresa = findViewById(R.id.address);
        animale_precedente_detalii = findViewById(R.id.previous_pets_details);
        animal_curent_specie = findViewById(R.id.pet_specie);
        animal_curent_varsta = findViewById(R.id.pet_age);
        animal_curent_temperament = findViewById(R.id.pet_temperament);
        grija_animal = findViewById(R.id.care_of_animal);
        grija_animal_vacanta = findViewById(R.id.vacation_plan);
        animal_probleme_sanatate = findViewById(R.id.health_behavior_issues);
        mesaj_adapost = findViewById(R.id.message_to_shelter);
        termeni_conditii = findViewById(R.id.accept_terms);

        animale_precedente = findViewById(R.id.have_pets_before_group);
        animal_curent = findViewById(R.id.have_other_pets_group);
        animale_adoptate = findViewById(R.id.adopted_before_group);
        locuinta = findViewById(R.id.living_environment_group);
        chirie = findViewById(R.id.rent_or_own_group);
        permisiune_proprietare_chirie = findViewById(R.id.owner_permission_group);
        alergie = findViewById(R.id.allergic_family_member_group);

        error_message = findViewById(R.id.error_message);
        error_message2 = findViewById(R.id.error_message2);
        error_message3 = findViewById(R.id.error_message3);
        error_message4 = findViewById(R.id.error_message4);
        error_message5 = findViewById(R.id.error_message5);
        error_message6 = findViewById(R.id.error_message6);
        error_message7 = findViewById(R.id.error_message7);

        button_back_to_main = findViewById(R.id.buttonBackToMain);
        button_submit = findViewById(R.id.submit_button);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        button_back_to_main.setOnClickListener(v -> onBackPressed());
        button_submit.setOnClickListener(v -> {
            if (validateFields()){
                saveFormData();
            }else{
                Toast.makeText(this, "Completati toate campurile", Toast.LENGTH_SHORT).show();
            }
        });

        animale_precedente.setOnCheckedChangeListener((group, checkedId) ->{
            if (checkedId != -1){
                String selectedOption = getSelectedRadioText(animale_precedente);
                if ("Da".equals(selectedOption)){
                    animale_precedente_detalii.setVisibility(View.VISIBLE);
                } else {
                    animale_precedente_detalii.setVisibility(View.GONE);
                }
            }
        });

        // Listener pentru radio button-ul „Are animale curente”
        animal_curent.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                String selectedOption = getSelectedRadioText(animal_curent);
                if ("Da".equals(selectedOption)) {
                    // Afișează câmpurile pentru animalul curent
                    animal_curent_specie.setVisibility(View.VISIBLE);
                    animal_curent_varsta.setVisibility(View.VISIBLE);
                    animal_curent_temperament.setVisibility(View.VISIBLE);
                } else {
                    // Ascunde câmpurile pentru animalul curent
                    animal_curent_specie.setVisibility(View.GONE);
                    animal_curent_varsta.setVisibility(View.GONE);
                    animal_curent_temperament.setVisibility(View.GONE);
                }
            }
        });
    }

    private boolean validateFields() {
        if (getSelectedRadioText(animale_precedente).isEmpty()){
            error_message.setVisibility(View.VISIBLE);
            return false;
        } else if ("Da".equals(getSelectedRadioText(animale_precedente))) {
            if (animale_precedente_detalii.getText().toString().isEmpty()) {
                animale_precedente_detalii.setError("Introduceți detalii despre animale anterioare");
                return false;
            }
        }

        if(getSelectedRadioText(animal_curent).isEmpty()){
            error_message2.setVisibility(View.VISIBLE);
            return false;
        } else if ("Da".equals(getSelectedRadioText(animal_curent))) {
            if (animal_curent_specie.getText().toString().isEmpty()) {
                animal_curent_specie.setError("Introduceți specie animal curent");
                return false;
            }
            if (animal_curent_varsta.getText().toString().isEmpty()) {
                animal_curent_varsta.setError("Introduceți varsta animal curent");
                return false;
            }
            if (animal_curent_temperament.getText().toString().isEmpty()) {
                animal_curent_temperament.setError("Introduceți temperament animal curent");
                return false;
            }
        }

        if (getSelectedRadioText(animale_adoptate).isEmpty()){
            error_message3.setVisibility(View.VISIBLE);
            return false;
        }
        if (getSelectedRadioText(locuinta).isEmpty()){
            error_message4.setVisibility(View.VISIBLE);
            return false;
        }
        if (getSelectedRadioText(chirie).isEmpty()){
            error_message5.setVisibility(View.VISIBLE);
            return false;
        }
        if (getSelectedRadioText(permisiune_proprietare_chirie).isEmpty()){
            error_message6.setVisibility(View.VISIBLE);
            return false;
        }
        if (getSelectedRadioText(alergie).isEmpty()){
            error_message7.setVisibility(View.VISIBLE);
            return false;
        }
        if (!termeni_conditii.isChecked()) {
            termeni_conditii.setError("Trebuie să accepți termenii și condițiile!");
            return false;
        } else {
            termeni_conditii.setError(null); // Elimină eroarea dacă a fost corectată
        }


        if (numar_telefon.getText().toString().isEmpty()) {
            numar_telefon.setError("Introduceți numărul de telefon");
            return false;
        }
        if (adresa.getText().toString().isEmpty()) {
            adresa.setError("Introduceți adresa");
            return false;
        }
        if (grija_animal.getText().toString().isEmpty()) {
            grija_animal.setError("Introduceți grija animalului");
            return false;
        }
        if (grija_animal_vacanta.getText().toString().isEmpty()) {
            grija_animal_vacanta.setError("Introduceți planul de vacanție pentru animal");
            return false;
        }
        if (animal_probleme_sanatate.getText().toString().isEmpty()) {
            animal_probleme_sanatate.setError("Introduceți probleme desanatate ale animalului");
            return false;
        }

        return true;
    }

    private void saveFormData() {
        String animalId = getIntent().getStringExtra("animal");
        String userId = firebaseUser.getUid();
        String numar_telefon = this.numar_telefon.getText().toString();
        String adresa = this.adresa.getText().toString();
        String animale_precedente_detalii = this.animale_precedente_detalii.getText().toString();
        String animal_curent_specie = this.animal_curent_specie.getText().toString();
        String animal_curent_varsta = this.animal_curent_varsta.getText().toString();
        String animal_curent_temperament = this.animal_curent_temperament.getText().toString();
        String grija_animal = this.grija_animal.getText().toString();
        String grija_animal_vacanta = this.grija_animal_vacanta.getText().toString();
        String animal_probleme_sanatate = this.animal_probleme_sanatate.getText().toString();
        String mesaj_adapost = this.mesaj_adapost.getText().toString();
        String animale_precedente = getSelectedRadioText(this.animale_precedente);
        String animal_curent = getSelectedRadioText(this.animal_curent);
        String animale_adoptate = getSelectedRadioText(this.animale_adoptate);
        String locuinta = getSelectedRadioText(this.locuinta);
        String chirie_proprietate = getSelectedRadioText(this.chirie);
        String permisiune_proprietare_chirie = getSelectedRadioText(this.permisiune_proprietare_chirie);
        String alergie = getSelectedRadioText(this.alergie);


        Map<String, Object> formData = new HashMap<>();
        formData.put("user_id", userId);
        formData.put("animal_id", animalId);
        formData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        formData.put("phone_number", numar_telefon);
        formData.put("address", adresa);
        formData.put("previous_pets_details", animale_precedente_detalii);
        formData.put("pet_specie", animal_curent_specie);
        formData.put("pet_age", animal_curent_varsta);
        formData.put("pet_temperament", animal_curent_temperament);
        formData.put("care_of_animal", grija_animal);
        formData.put("vacation_plan", grija_animal_vacanta);
        formData.put("health_behavior_issues", animal_probleme_sanatate);
        formData.put("message_to_shelter", mesaj_adapost);
        formData.put("have_pets_before", animale_precedente);
        formData.put("have_other_pets", animal_curent);
        formData.put("adopted_before", animale_adoptate);
        formData.put("living_environment", locuinta);
        formData.put("rent_or_own", chirie_proprietate);
        formData.put("owner_permission", permisiune_proprietare_chirie);
        formData.put("allergic_family_member", alergie);


        db.collection("AdoptionForms").add(formData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Formularul a fost trimis cu succes!", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la trimiterea formularului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedRadioText(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";  // Dacă nu a fost selectat niciun radio button
        RadioButton radioButton = findViewById(selectedId);  // Obține RadioButton-ul selectat
        return radioButton.getText().toString();  // Returnează textul RadioButton-ului selectat
    }

}
