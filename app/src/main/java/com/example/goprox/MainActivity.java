package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseActivity {

    private Button btnLogin, btnRegister, btnTestUser;
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
        btnTestUser = findViewById(R.id.btnTestUser);

        if (btnLogin == null || btnRegister == null || btnTestUser == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setOnClickListener(v -> goToActivity(LoginActivity.class));
        btnRegister.setOnClickListener(v -> goToActivity(RegistrationActivity.class));

        // 🔥 USER TESTING — Anonymous Auth
        btnTestUser.setOnClickListener(v -> loginAsTestUser());
    }

    private void loginAsTestUser() {
        btnTestUser.setEnabled(false);
        btnTestUser.setText("Logging in...");

        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    btnTestUser.setEnabled(true);
                    btnTestUser.setText("User Testing");
                    if (task.isSuccessful()) {
                        // Save test user name to Realtime Database
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            com.google.firebase.database.FirebaseDatabase
                                    .getInstance("https://myappproject-442cf-default-rtdb.europe-west1.firebasedatabase.app/")
                                    .getReference("users").child(user.getUid()).child("name")
                                    .setValue("Test User");
                        }
                        goToActivity(HomeActivity.class);
                    } else {
                        Toast.makeText(this, "Test login failed", Toast.LENGTH_SHORT).show();
                    }
                });
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