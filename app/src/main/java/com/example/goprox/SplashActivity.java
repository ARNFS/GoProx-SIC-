package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        // 1 վայրկյան հետո ստուգենք user-ի վիճակը
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserStatus();
            }
        }, 1000); // 1000 milliseconds = 1 second
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User signed in - check if email verified
            if (currentUser.isEmailVerified()) {
                // Email verified - go to Home
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                // Email not verified - go to VerifyEmail
                startActivity(new Intent(SplashActivity.this, VerifyEmailActivity.class));
            }
        } else {
            // No user - go to Login
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }

        // Close SplashActivity
        finish();
    }
}