package com.waqf.bewithme;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class translate extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);
    }

    public void openMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.support) {
                // Handle Support action
                Toast.makeText(translate.this, "الدعم", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.logout) {
                // Handle Logout action
                logout();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void logout() {
        Intent intent = new Intent(translate.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void Umrah(View view) {
        Intent intent = new Intent(translate.this, Umrah.class);
        startActivity(intent);
    }

    public void Guide(View view) {
        Intent intent = new Intent(translate.this, RequestGuide.class);
        startActivity(intent);
    }

    public void translate(View view) {
        Intent intent = new Intent(translate.this, translate.class);
        startActivity(intent);
    }

    public void services(View view) {
        Intent intent = new Intent(translate.this, services.class);
        startActivity(intent);
    }
}