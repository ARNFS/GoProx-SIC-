package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnSendReset, btnBackToLogin;
    private ProgressBar pbResetLoading;
    private TextView tvMessage;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        pbResetLoading = findViewById(R.id.pbResetLoading);
        tvMessage = findViewById(R.id.tvMessage);

        if (etResetEmail == null || btnSendReset == null || pbResetLoading == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();

        btnSendReset.setOnClickListener(v -> sendResetEmail());
        if (btnBackToLogin != null) {
            btnBackToLogin.setOnClickListener(v -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        }
    }

    private void hideKeyboard() {
        try {
            View v = getCurrentFocus();
            if (v != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    private void sendResetEmail() {
        hideKeyboard();

        String email = etResetEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etResetEmail.setError("Email required");
            Toast.makeText(this, "Enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etResetEmail.setError("Invalid email format");
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        pbResetLoading.setVisibility(View.VISIBLE);
        btnSendReset.setEnabled(false);
        if (tvMessage != null) tvMessage.setVisibility(View.GONE);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    pbResetLoading.setVisibility(View.GONE);
                    btnSendReset.setEnabled(true);

                    if (task.isSuccessful()) {
                        if (tvMessage != null) {
                            tvMessage.setVisibility(View.VISIBLE);
                            tvMessage.setTextColor(ContextCompat.getColor(this,
                                    android.R.color.holo_green_dark));
                            tvMessage.setText("✅ Password reset link sent to:\n" + email
                                    + "\n\nCheck your email and follow the instructions.");
                        }
                        Toast.makeText(this,
                                "Reset link sent to " + email, Toast.LENGTH_LONG).show();
                        etResetEmail.setText("");
                    } else {
                        String errorMsg = "Failed to send reset email";
                        if (task.getException() != null
                                && task.getException().getMessage() != null) {
                            errorMsg = task.getException().getMessage();
                            if (errorMsg.contains("no user record")) {
                                errorMsg = "No account found with this email address";
                            } else if (errorMsg.contains("format is invalid")) {
                                errorMsg = "Invalid email format";
                            }
                        }

                        if (tvMessage != null) {
                            tvMessage.setVisibility(View.VISIBLE);
                            tvMessage.setTextColor(ContextCompat.getColor(this,
                                    android.R.color.holo_red_dark));
                            tvMessage.setText("❌ " + errorMsg);
                        }
                        Toast.makeText(this, "Error: " + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}