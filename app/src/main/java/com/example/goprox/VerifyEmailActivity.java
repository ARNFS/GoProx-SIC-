package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends BaseActivity{

    Button btnResend, btnCheck, btnLogout;
    TextView tvInfo, tvEmail;
    ProgressBar pbLoading;

    FirebaseAuth mAuth;
    FirebaseUser user;

    private Handler handler = new Handler();
    private Runnable autoCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        btnResend = findViewById(R.id.btnResend);
        btnCheck = findViewById(R.id.btnCheck);
        btnLogout = findViewById(R.id.btnLogout);
        tvInfo = findViewById(R.id.tvInfo);
        tvEmail = findViewById(R.id.tvEmail);
        pbLoading = findViewById(R.id.pbLoading);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user == null) {
            // No user, go to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Show user email
        tvEmail.setText(user.getEmail());

        // Auto-check every 5 seconds
        startAutoCheck();

        btnResend.setOnClickListener(v -> resendVerificationEmail());
        btnCheck.setOnClickListener(v -> checkEmailVerified());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void startAutoCheck() {
        autoCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (user != null) {
                    user.reload().addOnCompleteListener(task -> {
                        if (user.isEmailVerified()) {
                            // Email verified - go to home
                            Toast.makeText(VerifyEmailActivity.this,
                                    "Email verified! Redirecting...", Toast.LENGTH_SHORT).show();
                            goToHome();
                        } else {
                            // Not verified yet - check again after 5 seconds
                            handler.postDelayed(this, 5000);
                        }
                    });
                }
            }
        };
        handler.postDelayed(autoCheckRunnable, 5000);
    }

    private void resendVerificationEmail() {
        if (user == null) return;

        pbLoading.setVisibility(android.view.View.VISIBLE);
        user.sendEmailVerification().addOnCompleteListener(task -> {
            pbLoading.setVisibility(android.view.View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(this,
                        "Verification email sent to " + user.getEmail(),
                        Toast.LENGTH_LONG).show();
                tvInfo.setText("Check your email and click the verification link");
            } else {
                String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                Toast.makeText(this,
                        "Failed to send: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkEmailVerified() {
        if (user == null) return;

        pbLoading.setVisibility(android.view.View.VISIBLE);
        user.reload().addOnCompleteListener(task -> {
            pbLoading.setVisibility(android.view.View.GONE);
            if (user.isEmailVerified()) {
                Toast.makeText(this, "Email verified! Redirecting...", Toast.LENGTH_SHORT).show();
                goToHome();
            } else {
                Toast.makeText(this,
                        "Email not verified yet. Please check your inbox.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop auto-check when activity is destroyed
        if (handler != null && autoCheckRunnable != null) {
            handler.removeCallbacks(autoCheckRunnable);
        }
    }
}