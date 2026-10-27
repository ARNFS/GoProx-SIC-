package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class RegistrationActivity extends AppCompatActivity {
    private Button btnGoogle, btnEmail;
    private EditText etEmail, etPassword, etConfirmPassword;
    private LinearLayout llPasswordFields;
    private ProgressBar pbLoading;
    private TextView tvGoToLogin;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleLauncher;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnGoogle = findViewById(R.id.btnGoogle);
        btnEmail = findViewById(R.id.btnEmail);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        llPasswordFields = findViewById(R.id.llPasswordFields);
        pbLoading = findViewById(R.id.pbLoading);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        if (btnGoogle == null || btnEmail == null || etEmail == null
                || etPassword == null || etConfirmPassword == null
                || llPasswordFields == null || pbLoading == null) {
            Toast.makeText(this, "Couldn't load the registration screen.", Toast.LENGTH_SHORT).show();
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
                            saveProfileAndContinue(auth.getCurrentUser());
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
        btnEmail.setOnClickListener(v -> {
            hideKeyboard();
            registerWithEmail();
        });
        etConfirmPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                registerWithEmail();
                return true;
            }
            return false;
        });
        if (tvGoToLogin != null) tvGoToLogin.setOnClickListener(v -> goToLogin());
    }

    private void registerWithEmail() {
        if (busy) return;
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);

        String email = AuthInputValidator.normalizeEmail(etEmail.getText().toString());
        String password = etPassword.getText().toString();
        String confirmation = etConfirmPassword.getText().toString();

        if (!AuthInputValidator.isValidEmail(email)) {
            etEmail.setError("Enter a valid email address.");
            etEmail.requestFocus();
            return;
        }
        if (!AuthInputValidator.isValidPassword(password)) {
            etPassword.setError("Use at least 6 characters.");
            etPassword.requestFocus();
            return;
        }
        if (!AuthInputValidator.passwordsMatch(password, confirmation)) {
            etConfirmPassword.setError("Passwords don't match.");
            etConfirmPassword.requestFocus();
            return;
        }

        setBusy(true);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                setBusy(false);
                showMessage(AuthErrorMessages.registration(task.getException()));
                return;
            }
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                setBusy(false);
                showMessage("Account was created, but couldn't be loaded. Sign in to continue.");
                auth.signOut();
                return;
            }

            AuthProfileSync.ensureProfile(user).addOnCompleteListener(this, profileTask -> {
                if (!profileTask.isSuccessful()) {
                    auth.signOut();
                    setBusy(false);
                    showMessage("Account created, but profile setup failed. Sign in again to retry it.");
                    return;
                }
                sendVerificationEmail(user);
            });
        });
    }

    private void saveProfileAndContinue(FirebaseUser user) {
        if (user == null) {
            setBusy(false);
            showMessage("Account couldn't be loaded. Try again.");
            return;
        }
        AuthProfileSync.ensureProfile(user).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                auth.signOut();
                setBusy(false);
                showMessage("Couldn't finish sign-in because profile setup failed. Check your connection and retry.");
                return;
            }
            if (!user.isEmailVerified()) {
                startActivity(new Intent(this, VerifyEmailActivity.class));
                finish();
            } else {
                goHome();
            }
        });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                setBusy(false);
                showMessage("Account created. Couldn't send the verification email; sign in to resend it.");
                auth.signOut();
                return;
            }
            setBusy(false);
            startActivity(new Intent(this, VerifyEmailActivity.class));
            finish();
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
        if (tvGoToLogin != null) tvGoToLogin.setEnabled(!value);
        if (etEmail != null) etEmail.setEnabled(!value);
        if (etPassword != null) etPassword.setEnabled(!value);
        if (etConfirmPassword != null) etConfirmPassword.setEnabled(!value);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void goHome() {
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
