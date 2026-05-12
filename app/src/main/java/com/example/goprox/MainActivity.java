package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

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

        // If user exists and email verified → go to Home
        if (user != null && user.isEmailVerified()) {
            goToActivity(HomeActivity.class);
            return;
        }

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegistration);

        if (btnLogin == null || btnRegister == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setOnClickListener(v -> goToActivity(LoginActivity.class));
        btnRegister.setOnClickListener(v -> goToActivity(RegistrationActivity.class));
    }

    private void goToActivity(Class<?> target) {
        try {
            Intent intent = new Intent(this, target);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show();
        }
    }
}