package com.waqf.bewithme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Def1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_def1);
    }
    public void toLogin(View view) {
        Intent intent = new Intent(Def1.this , LoginActivity.class);
        startActivity(intent);
    }
    public void toDef2(View view) {
        Intent intent = new Intent(Def1.this , Def2.class);
        startActivity(intent);
    }
}