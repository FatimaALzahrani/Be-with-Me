package com.waqf.bewithme;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private TextView loginTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        loginTextView = findViewById(R.id.loginTextView);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users"); // مسار قاعدة البيانات

        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                nameEditText.setError("يجب إدخال الاسم");
                return;
            }

            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("يجب إدخال البريد الإلكتروني");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("يجب إدخال كلمة المرور");
                return;
            }

            if (password.length() < 6) {
                passwordEditText.setError("يجب أن تكون كلمة المرور مكونة من 6 أحرف على الأقل");
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordEditText.setError("كلمات المرور غير متطابقة");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            // إنشاء حساب مستخدم جديد
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // إضافة بيانات المستخدم إلى قاعدة البيانات
                                String userId = user.getUid();
                                User userInfo = new User(name, email);
                                databaseReference.child(userId).setValue(userInfo)
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "تم إنشاء الحساب بنجاح.", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "فشل في إضافة بيانات المستخدم: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "فشل في إنشاء الحساب: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }

    // تعريف كلاس User لتمثيل بيانات المستخدم
    public static class User {
        public String name;
        public String email;

        public User() {
            // مطلوب فارغ لتسهيل التحويل
        }

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
