package com.example.adapostapp;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MessagesActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private LinearLayout messageInputLayout;
    private ImageButton buttonBackToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) {
                // Tastatura este afișată
                bottomNavigationView.setVisibility(View.GONE);
                // Modifici layout_above pentru messageInputLayout
                messageInputLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT));
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messageInputLayout.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                messageInputLayout.setLayoutParams(params);
            } else {
                // Tastatura este ascunsă
                bottomNavigationView.setVisibility(View.VISIBLE);
                // Revii la layout-ul inițial
                messageInputLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT));
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messageInputLayout.getLayoutParams();
                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ABOVE, R.id.bottomNavigationView);
                messageInputLayout.setLayoutParams(params);
            }
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        buttonBackToMain = findViewById(R.id.buttonBackToMain);

        buttonBackToMain.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        bottomNavigationView.setSelectedItemId(R.id.navigation_messages);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(MessagesActivity.this, MainActivity.class));
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                startActivity(new Intent(MessagesActivity.this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                return true;
            } else if (itemId == R.id.navigation_animals) {
                startActivity(new Intent(MessagesActivity.this, ListAnimalActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(MessagesActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });

    }
}