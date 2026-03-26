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

import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    EditText etResetEmail;
    Button btnSendReset, btnBackToLogin;
    ProgressBar pbResetLoading;
    TextView tvMessage;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        pbResetLoading = findViewById(R.id.pbResetLoading);
        tvMessage = findViewById(R.id.tvMessage);

        mAuth = FirebaseAuth.getInstance();

        btnSendReset.setOnClickListener(v -> sendResetEmail());
        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void hideKeyboard() {
        View v = this.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void sendResetEmail() {
        hideKeyboard();

        String email = etResetEmail.getText().toString().trim();

        // Validate email
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

        // Show loading
        pbResetLoading.setVisibility(View.VISIBLE);
        btnSendReset.setEnabled(false);
        tvMessage.setVisibility(View.GONE);

        // Send reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    pbResetLoading.setVisibility(View.GONE);
                    btnSendReset.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Success
                        tvMessage.setVisibility(View.VISIBLE);
                        tvMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        tvMessage.setText("✅ Password reset link sent to:\n" + email +
                                "\n\nCheck your email and follow the instructions.");

                        Toast.makeText(this,
                                "Reset link sent to " + email,
                                Toast.LENGTH_LONG).show();

                        // Clear email field
                        etResetEmail.setText("");
                    } else {
                        // Error
                        String errorMsg;
                        if (task.getException() != null) {
                            errorMsg = task.getException().getMessage();
                            // Handle common errors
                            if (errorMsg.contains("no user record")) {
                                errorMsg = "No account found with this email address";
                            } else if (errorMsg.contains("format is invalid")) {
                                errorMsg = "Invalid email format";
                            }
                        } else {
                            errorMsg = "Failed to send reset email";
                        }

                        tvMessage.setVisibility(View.VISIBLE);
                        tvMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        tvMessage.setText("❌ " + errorMsg);

                        Toast.makeText(this,
                                "Error: " + errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}