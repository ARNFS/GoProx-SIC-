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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private Button btnGoogle, btnEmail, btnForgotPassword;
    private EditText etEmail, etPassword;
    private ProgressBar pbLoading;
    private TextView tvGoToRegister;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleLauncher;
    private boolean busy;

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

        if (btnGoogle == null || btnEmail == null || btnForgotPassword == null
                || etEmail == null || etPassword == null || pbLoading == null) {
            Toast.makeText(this, "Couldn't load the sign-in screen.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        auth = FirebaseAuth.getInstance();
        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        setBusy(false);
                        return; // User cancelled Google sign-in.
                    }
                    try {
                        GoogleSignInAccount account = GoogleSignIn
                                .getSignedInAccountFromIntent(result.getData())
                                .getResult(ApiException.class);
                        if (account == null || account.getIdToken() == null) {
                            setBusy(false);
                            showMessage("Google couldn't verify this sign-in. Try again.");
                            return;
                        }
                        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
                            if (!task.isSuccessful()) {
                                setBusy(false);
                                showMessage(AuthErrorMessages.signIn(task.getException()));
                                return;
                            }
                            finishSignIn(auth.getCurrentUser());
                        });
                    } catch (ApiException error) {
                        setBusy(false);
                        showMessage("Google sign-in was cancelled or couldn't be completed.");
                    }
                });

        try {
            GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, options);
        } catch (RuntimeException error) {
            googleSignInClient = null;
        }

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        btnEmail.setOnClickListener(v -> loginWithEmail());
        btnForgotPassword.setOnClickListener(v -> forgotPassword());
        if (tvGoToRegister != null) {
            tvGoToRegister.setOnClickListener(v -> {
                startActivity(new Intent(this, RegistrationActivity.class));
                finish();
            });
        }

        etPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                loginWithEmail();
                return true;
            }
            return false;
        });
    }

    private void loginWithEmail() {
        if (busy) return;
        hideKeyboard();
        etEmail.setError(null);
        etPassword.setError(null);

        String email = AuthInputValidator.normalizeEmail(etEmail.getText().toString());
        String password = etPassword.getText().toString(); // Password whitespace is significant.
        if (!AuthInputValidator.isValidEmail(email)) {
            etEmail.setError("Enter a valid email address.");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Enter your password.");
            etPassword.requestFocus();
            return;
        }

        setBusy(true);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                setBusy(false);
                showMessage(AuthErrorMessages.signIn(task.getException()));
                return;
            }
            finishSignIn(auth.getCurrentUser());
        });
    }

    private void finishSignIn(FirebaseUser user) {
        if (user == null) {
            setBusy(false);
            showMessage("Sign-in completed, but the account couldn't be loaded. Try again.");
            return;
        }
        AuthProfileSync.ensureProfile(user).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                auth.signOut();
                setBusy(false);
                showMessage("Couldn't finish sign-in because your profile couldn't sync. Check your connection and retry.");
                return;
            }
            if (!user.isEmailVerified()) {
                startActivity(new Intent(this, VerifyEmailActivity.class));
                finish();
                return;
            }
            goToHome();
        });
    }

    private void forgotPassword() {
        if (busy) return;
        etEmail.setError(null);
        String email = AuthInputValidator.normalizeEmail(etEmail.getText().toString());
        if (!AuthInputValidator.isValidEmail(email)) {
            etEmail.setError("Enter a valid email address first.");
            etEmail.requestFocus();
            return;
        }

        setBusy(true);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(this, task -> {
            setBusy(false);
            if (task.isSuccessful()) {
                showMessage("If an account uses this email, a reset link will arrive shortly.");
            } else {
                showMessage("Couldn't send the reset email. Check your connection and try again.");
            }
        });
    }

    private void startGoogleSignIn() {
        if (busy) return;
        if (googleSignInClient == null) {
            showMessage("Google sign-in isn't available right now.");
            return;
        }
        setBusy(true);
        try {
            googleLauncher.launch(googleSignInClient.getSignInIntent());
        } catch (RuntimeException error) {
            setBusy(false);
            showMessage("Google sign-in couldn't start. Please try again.");
        }
    }

    private void setBusy(boolean value) {
        busy = value;
        if (pbLoading != null) pbLoading.setVisibility(value ? View.VISIBLE : View.GONE);
        if (btnGoogle != null) btnGoogle.setEnabled(!value);
        if (btnEmail != null) btnEmail.setEnabled(!value);
        if (btnForgotPassword != null) btnForgotPassword.setEnabled(!value);
        if (tvGoToRegister != null) tvGoToRegister.setEnabled(!value);
        if (etEmail != null) etEmail.setEnabled(!value);
        if (etPassword != null) etPassword.setEnabled(!value);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) return;
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
