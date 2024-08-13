package com.waqf.bewithme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    public void Umrah(View view) {
        Intent intent = new Intent(Home.this , Umrah.class);
        startActivity(intent);
    }

    public void Haj(View view) {
        Intent intent = new Intent(Home.this , Haj.class);
        startActivity(intent);
    }
}