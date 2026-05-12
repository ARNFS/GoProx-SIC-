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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

public class LoginActivity extends AppCompatActivity {

    private Button btnGoogle, btnEmail, btnForgotPassword;
    private EditText etEmail, etPassword;
    private ProgressBar pbLoading;
    private TextView tvGoToRegister;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleLauncher;

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

        if (btnGoogle == null || btnEmail == null || etEmail == null ||
                etPassword == null || pbLoading == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();

        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Toast.makeText(this, "Google Sign-In init failed", Toast.LENGTH_SHORT).show();
        }

        // Modern Google sign-in
        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (pbLoading == null) return;
                    Intent data = result.getData();
                    if (data == null) {
                        pbLoading.setVisibility(View.GONE);
                        return;
                    }

                    try {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(data);
                        GoogleSignInAccount account = task.getResult(ApiException.class);

                        if (account == null || account.getIdToken() == null) {
                            pbLoading.setVisibility(View.GONE);
                            return;
                        }

                        AuthCredential credential =
                                GoogleAuthProvider.getCredential(account.getIdToken(), null);

                        mAuth.signInWithCredential(credential)
                                .addOnCompleteListener(t -> {
                                    pbLoading.setVisibility(View.GONE);
                                    if (t.isSuccessful()) {
                                        goToHome();
                                    } else {
                                        Toast.makeText(this,
                                                "Login failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } catch (ApiException e) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Google error", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        btnEmail.setOnClickListener(v -> loginWithEmail());
        btnForgotPassword.setOnClickListener(v -> forgotPassword());

        if (tvGoToRegister != null) {
            tvGoToRegister.setOnClickListener(v -> {
                startActivity(new Intent(this, RegistrationActivity.class));
                finish();
            });
        }
    }

    private void loginWithEmail() {
        hideKeyboard();

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            goToHome();
                        } else {
                            Toast.makeText(this,
                                    "Verify email first", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, VerifyEmailActivity.class));
                            finish();
                        }
                    } else {
                        String error = "Wrong credentials";
                        if (task.getException() != null) {
                            String msg = task.getException().getMessage();
                            if (msg != null && msg.contains("no user record")) {
                                error = "No account found with this email";
                            }
                        }
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void forgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter email first", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        String error = "Failed request";
                        if (task.getException() != null) {
                            String msg = task.getException().getMessage();
                            if (msg != null && msg.contains("no user record")) {
                                error = "No account found with this email";
                            }
                        }
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startGoogleSignIn() {
        if (googleSignInClient == null) {
            Toast.makeText(this, "Google Sign-In unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        try {
            Intent intent = googleSignInClient.getSignInIntent();
            googleLauncher.launch(intent);
        } catch (Exception e) {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Google Sign-In error", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHome() {
        try {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        try {
            View v = getCurrentFocus();
            if (v != null) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }
}