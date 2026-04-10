package com.example.goprox;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;

public class RegistrationActivity extends AppCompatActivity {

    private Button btnGoogle, btnEmail;
    private EditText etEmail, etPassword, etConfirmPassword;
    private LinearLayout llPasswordFields;
    private ProgressBar pbLoading;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        btnGoogle = findViewById(R.id.btnGoogle);
        btnEmail = findViewById(R.id.btnEmail);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        llPasswordFields = findViewById(R.id.llPasswordFields);
        pbLoading = findViewById(R.id.pbLoading);

        // Check if views exist (prevent crash)
        if (btnGoogle == null || btnEmail == null || etEmail == null || pbLoading == null) {
            Toast.makeText(this, "Layout error: missing views", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();

        // Google Sign-In config
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());

        btnEmail.setOnClickListener(v -> {
            if (llPasswordFields != null && llPasswordFields.getVisibility() == View.GONE) {
                llPasswordFields.setVisibility(View.VISIBLE);
                btnEmail.setText("Register");
                etEmail.requestFocus();
            } else {
                hideKeyboard();
                registerWithEmail();
            }
        });
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void registerWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            List<String> methods = task.getResult().getSignInMethods();

            if (methods != null && !methods.isEmpty()) {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                if (methods.contains("google.com")) {
                    Toast.makeText(this, "This email is registered with Google. Please sign in with Google.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "This email is already registered", Toast.LENGTH_LONG).show();
                }
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(createTask -> {
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                        if (createTask.isSuccessful()) {
                            sendVerificationEmail();
                        } else {
                            Toast.makeText(this, "Registration failed: " + createTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Verification email sent", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, VerifyEmailActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startGoogleSignIn() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                if (account == null || account.getEmail() == null) {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                String email = account.getEmail();

                mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(fetchTask -> {
                    if (!fetchTask.isSuccessful() || fetchTask.getResult() == null) {
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Error checking account", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> methods = fetchTask.getResult().getSignInMethods();

                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

                    if (methods == null || methods.isEmpty()) {
                        mAuth.signInWithCredential(credential).addOnCompleteListener(signInTask -> {
                            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                            if (signInTask.isSuccessful()) {
                                goHome();
                            } else {
                                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        mAuth.signInWithCredential(credential).addOnCompleteListener(signInTask -> {
                            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                            if (signInTask.isSuccessful()) {
                                goHome();
                            } else {
                                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

            } catch (Exception e) {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                Toast.makeText(this, "Google error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}