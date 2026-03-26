package com.example.goprox;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1001;

    Button btnGoogle, btnEmail, btnForgotPassword;
    EditText etEmail, etPassword;
    ProgressBar pbLoading;
    TextView tvGoToRegister;

    FirebaseAuth mAuth;
    GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnGoogle = findViewById(R.id.btnGoogleLogin);
        btnEmail = findViewById(R.id.btnEmailLogin);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        etEmail = findViewById(R.id.etEmailLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        pbLoading = findViewById(R.id.pbLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        btnEmail.setOnClickListener(v -> loginWithEmail());
        btnForgotPassword.setOnClickListener(v -> forgotPassword());
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistrationActivity.class));
            finish();
        });
    }

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    pbLoading.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            goToHome();
                        } else {
                            Toast.makeText(this, "Please verify your email first", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, VerifyEmailActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Wrong email or password", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void forgotPassword() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            pbLoading.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startGoogleSignIn() {
        pbLoading.setVisibility(View.VISIBLE);
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

                if (account == null) {
                    pbLoading.setVisibility(View.GONE);
                    return;
                }

                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(signInTask -> {
                            pbLoading.setVisibility(View.GONE);

                            if (signInTask.isSuccessful()) {
                                goToHome();
                            } else {
                                Toast.makeText(this,
                                        "Login failed: " + signInTask.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });

            } catch (ApiException e) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(this, "Google error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 🔥 ՍԱ Է ՊԱԿԱՑՈՂ ՄԵԹՈԴԸ 🔥
    private void goToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}