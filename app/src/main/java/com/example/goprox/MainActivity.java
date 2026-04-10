package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseActivity {

    private Button btnLogin, btnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // Եթե օգտատերը արդեն կա և verify արած է → ուղղակի HomeActivity
        if (user != null && user.isEmailVerified()) {
            startActivity(new Intent(MainActivity.this, HomeActivity.class)); // 🔥 ՓՈԽԵՑԻ
            finish();
            return;
        }

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegistration);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegistrationActivity.class));
            finish();
        });
    }
}