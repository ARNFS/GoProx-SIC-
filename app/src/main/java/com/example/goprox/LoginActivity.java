package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    Button btnGoogle, btnEmail, btnPhone;
    EditText etEmail, etPhone;
    ProgressBar pbLoading;

    FirebaseAuth mAuth;
    GoogleSignInClient googleSignInClient;

    int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnGoogle = findViewById(R.id.btnGoogle);
        btnEmail = findViewById(R.id.btnEmail);
        btnPhone = findViewById(R.id.btnPhone);

        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        pbLoading = findViewById(R.id.pbLoading);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> signInGoogle());

        btnEmail.setOnClickListener(v -> {
            if (etEmail.getVisibility() == View.GONE) {
                etEmail.setVisibility(View.VISIBLE);
            } else {
                loginEmail();
            }
        });

        btnPhone.setOnClickListener(v -> {
            if (etPhone.getVisibility() == View.GONE) {
                etPhone.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this,"Phone login coming soon",Toast.LENGTH_SHORT).show();
            }
        });
    }

    void signInGoogle() {
        pbLoading.setVisibility(View.VISIBLE);

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    void loginEmail() {

        String email = etEmail.getText().toString().trim();

        if(email.isEmpty()){
            Toast.makeText(this,"Enter email",Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email,"123456")
                .addOnCompleteListener(task -> {

                    pbLoading.setVisibility(View.GONE);

                    if(task.isSuccessful()){
                        openHome();
                    } else {
                        Toast.makeText(this,"Login failed",Toast.LENGTH_SHORT).show();
                    }

                });

    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode==RC_SIGN_IN){

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {

                GoogleSignInAccount account = task.getResult(ApiException.class);

                AuthCredential credential = GoogleAuthProvider
                        .getCredential(account.getIdToken(),null);

                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task1 -> {

                            pbLoading.setVisibility(View.GONE);

                            if(task1.isSuccessful()){
                                openHome();
                            }else{
                                Toast.makeText(this,"Google login failed",Toast.LENGTH_SHORT).show();
                            }

                        });

            } catch (ApiException e) {

                pbLoading.setVisibility(View.GONE);
                Toast.makeText(this,"Google error",Toast.LENGTH_SHORT).show();

            }
        }
    }

    void openHome(){

        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();

    }

    @Override
    protected void onStart(){

        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();

        if(user!=null){
            openHome();
        }

    }
}