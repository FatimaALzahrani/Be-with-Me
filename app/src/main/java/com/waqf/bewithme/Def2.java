package com.waqf.bewithme;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Def2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_def2);
    }

    public void toDef3(View view) {
        Intent intent = new Intent(Def2.this , Def3.class);
        startActivity(intent);
    }

    public void toLogin(View view) {
        Intent intent = new Intent(Def2.this , LoginActivity.class);
        startActivity(intent);
    }

    public void toDef1(View view) {
        Intent intent = new Intent(Def2.this , Def3.class);
        startActivity(intent);
    }
}