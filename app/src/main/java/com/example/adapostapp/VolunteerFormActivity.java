package com.example.adapostapp;

import android.content.Intent;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.example.adapostapp.utils.UserUtils;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VolunteerFormActivity extends AppCompatActivity {
    private EditText fullName, phoneNumber, motivation, mondayTime, tuesdayTime,
            wednesdayTime, thursdayTime, fridayTime, saturdayTime, sundayTime, experienceDetails;
    private CheckBox monday, tuesday, wednesday, thursday, friday, saturday, sunday, terms;
    private Button submitButton;
    private ImageButton backButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RadioGroup experienceRadioGroup;
    private TextView error_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_form);

        // Inițializare Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inițializare componente UI
        fullName = findViewById(R.id.fullName);
        phoneNumber = findViewById(R.id.phoneNumber);
        motivation = findViewById(R.id.motivation);
        experienceRadioGroup = findViewById(R.id.experienceRadioGroup);
        experienceDetails = findViewById(R.id.experienceDetails);
        error_message = findViewById(R.id.error_message);

        mondayTime = findViewById(R.id.mondayTime);
        tuesdayTime = findViewById(R.id.tuesdayTime);
        wednesdayTime = findViewById(R.id.wednesdayTime);
        thursdayTime = findViewById(R.id.thursdayTime);
        fridayTime = findViewById(R.id.fridayTime);
        saturdayTime = findViewById(R.id.saturdayTime);
        sundayTime = findViewById(R.id.sundayTime);

        monday = findViewById(R.id.monday);
        tuesday = findViewById(R.id.tuesday);
        wednesday = findViewById(R.id.wednesday);
        thursday = findViewById(R.id.thursday);
        friday = findViewById(R.id.friday);
        saturday = findViewById(R.id.saturday);
        sunday = findViewById(R.id.sunday);
        terms = findViewById(R.id.terms);

        submitButton = findViewById(R.id.submitVolunteerForm);
        backButton = findViewById(R.id.buttonBackToMain);

        // Verificare autentificare
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }else {
            isApplicated(user);
            checkUserRole(user);
        }

        experienceRadioGroup.setOnCheckedChangeListener((group, checkedId) ->{
            if (checkedId != -1){
                String selectedOption = getSelectedRadioText(experienceRadioGroup);
                if ("Da".equals(selectedOption)){
                    experienceDetails.setVisibility(View.VISIBLE);
                } else {
                    experienceDetails.setVisibility(View.GONE);
                }
            }
        });


        // Precompletare date utilizator
        fullName.setText(user.getDisplayName());

        // Listener pentru butonul Back
        backButton.setOnClickListener(v -> onBackPressed());

        // Configurare checkbox-uri și TimePicker
        setupDayCheckBox(monday, mondayTime);
        setupDayCheckBox(tuesday, tuesdayTime);
        setupDayCheckBox(wednesday, wednesdayTime);
        setupDayCheckBox(thursday, thursdayTime);
        setupDayCheckBox(friday, fridayTime);
        setupDayCheckBox(saturday, saturdayTime);
        setupDayCheckBox(sunday, sundayTime);

        // Listener pentru butonul Submit
        submitButton.setOnClickListener(v -> {
            if (validateFields(user)){
                submitForm(user);
            }else{
                Toast.makeText(this, "Completati toate campurile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDayCheckBox(CheckBox checkBox, EditText timeEditText) {
        // Activează/dezactivează EditText în funcție de checkbox
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timeEditText.setEnabled(isChecked);
            if (!isChecked) {
                timeEditText.setText(""); // Resetează câmpul dacă checkbox-ul e debifat
            }
        });

        // Deschide TimePicker la click pe EditText
        timeEditText.setOnClickListener(v -> {
            if (timeEditText.isEnabled()) {
                showTimeRangePicker(timeEditText);
            }
        });
    }

    private void showTimeRangePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();

        MaterialTimePicker startTimePicker = new MaterialTimePicker.Builder()
                .setTitleText("Selectează ora de început")
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .build();

        startTimePicker.addOnPositiveButtonClickListener(view -> {
            String startTime = String.format(Locale.getDefault(), "%02d:%02d", startTimePicker.getHour(), startTimePicker.getMinute());

            MaterialTimePicker endTimePicker = new MaterialTimePicker.Builder()
                    .setTitleText("Selectează ora de sfârșit")
                    .setHour(startTimePicker.getHour())
                    .setMinute(startTimePicker.getMinute())
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .build();

            endTimePicker.addOnPositiveButtonClickListener(view2 -> {
                String endTime = String.format(Locale.getDefault(), "%02d:%02d", endTimePicker.getHour(), endTimePicker.getMinute());
                editText.setText(startTime + " - " + endTime);
            });

            endTimePicker.show(((FragmentActivity) editText.getContext()).getSupportFragmentManager(), "END_TIME_PICKER");
        });

        startTimePicker.show(((FragmentActivity) editText.getContext()).getSupportFragmentManager(), "START_TIME_PICKER");
    }

    private boolean validateFields(FirebaseUser user){
        if (user == null) {
            Toast.makeText(this, "Trebuie să fii autentificat!", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validare câmpuri obligatorii
        if (fullName.getText().toString().isEmpty()) {
            fullName.setError("Numele este obligatoriu");
            return false;
        }
        if (phoneNumber.getText().toString().isEmpty()) {
            phoneNumber.setError("Numărul de telefon este obligatoriu");
            return false;
        }
        if (motivation.getText().toString().isEmpty()) {
            motivation.setError("Motivația este obligatorie");
            return false;
        }
        if (!terms.isChecked()) {
            Toast.makeText(this, "Trebuie să accepți termenii și condițiile!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (getSelectedRadioText(experienceRadioGroup).isEmpty()){
            error_message.setVisibility(View.VISIBLE);
            return false;
        } else if ("Da".equals(getSelectedRadioText(experienceRadioGroup))) {
            if (experienceDetails.getText().toString().isEmpty()) {
                experienceDetails.setError("Introduceți detalii despre experienta anterioara!");
                return false;
            }
        }

        return true;
    }

    private void checkDisp(CheckBox dayCheckBox, EditText dayTime, String day, Map<String, String> availability){
        if (dayCheckBox.isChecked() && !dayTime.getText().toString().isEmpty()) {
            availability.put(day, dayTime.getText().toString());
        }
    }

    private void submitForm(FirebaseUser user) {
        // Colectare disponibilitate
        Map<String, String> availability = new HashMap<>();
        checkDisp(monday, mondayTime, "Luni", availability);
        checkDisp(tuesday, tuesdayTime, "Marți", availability);
        checkDisp(wednesday, wednesdayTime, "Miercuri", availability);
        checkDisp(thursday, thursdayTime, "Joi", availability);
        checkDisp(friday, fridayTime, "Vineri", availability);
        checkDisp(saturday, saturdayTime, "Sâmbătă", availability);
        checkDisp(sunday, sundayTime, "Duminică", availability);

        if (availability.isEmpty()) {
            Toast.makeText(this, "Selectează cel puțin o zi disponibilă!", Toast.LENGTH_SHORT).show();
            mondayTime.setError("");
            tuesdayTime.setError("");
            wednesdayTime.setError("");
            thursdayTime.setError("");
            fridayTime.setError("");
            saturdayTime.setError("");
            sundayTime.setError("");
            return;
        }



        VolunteerApplications volunteerData = new VolunteerApplications(phoneNumber.getText().toString(), motivation.getText().toString(),
                user.getUid(), availability, "În așteptare", "Da".equals(getSelectedRadioText(experienceRadioGroup)), experienceDetails.getText().toString(), null, null, null);

        // Trimitere către Firestore
        db.collection("VolunteerRequests")
                .add(volunteerData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Cererea a fost trimisă cu succes!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la trimiterea cererii", e);
                    Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUserRole(FirebaseUser user) {
        UserUtils.checkUserRole(user, new UserUtils.UserRoleCallback() {
            @Override
            public void onRoleRetrieved(String role) {
                if (role.equals("admin")) {
                    startActivity(new Intent(VolunteerFormActivity.this, ApplicationsListActivity.class));
                    finish();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("Firestore", "Eroare la preluarea tipului de utilizator", e);
                Toast.makeText(VolunteerFormActivity.this, "Eroare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void isApplicated(FirebaseUser user){
        db.collection("VolunteerRequests")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshots -> {
                    if (!documentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Deja ai aplicat!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, VolunteerApplicationDetailsActivity.class);
                        intent.putExtra("volunteerApplication", documentSnapshots.getDocuments().get(0).getId());
                        startActivity(intent);
                        finish();
                    }
                }).addOnFailureListener(e -> {
                    Log.e("Firestore", "Eroare la preluarea aplicatiilor", e);
                    Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private String getSelectedRadioText(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";  // Dacă nu a fost selectat niciun radio button
        RadioButton radioButton = findViewById(selectedId);  // Obține RadioButton-ul selectat
        return radioButton.getText().toString();  // Returnează textul RadioButton-ului selectat
    }

}
