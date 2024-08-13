package com.waqf.bewithme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterGuideActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextPhoneNumber;
    private Button buttonRegister;

    private FirebaseAuth mAuth;
    private DatabaseReference guidesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_guide);

        editTextName = findViewById(R.id.editTextName);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        buttonRegister = findViewById(R.id.buttonRegister);

        mAuth = FirebaseAuth.getInstance();
        guidesRef = FirebaseDatabase.getInstance().getReference("guides");

        buttonRegister.setOnClickListener(v -> registerGuide());
    }

    private void registerGuide() {
        String name = editTextName.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();

        if (name.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show();
            return;
        }

        // الحصول على معرف المستخدم الحالي من Firebase Auth
        String userId = mAuth.getCurrentUser().getUid();

        // إعداد بيانات المرشد
        Guide guide = new Guide(name, phoneNumber, 0.0, 0.0);

        // تخزين بيانات المرشد في قاعدة البيانات
        guidesRef.child(userId).setValue(guide).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(RegisterGuideActivity.this, "تم تسجيل المرشد بنجاح", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterGuideActivity.this,GuideActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(RegisterGuideActivity.this, "فشل في تسجيل المرشد", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
