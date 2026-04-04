package com.example.goprox;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import androidx.annotation.NonNull;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference callsRef;
    private ValueEventListener callListener;

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
        }, 1000);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // ✅ 6-րդ քայլ – Start listening for incoming calls
            startCallInvitationListener(currentUser.getUid());

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

    // ============================================================
    // 6-րդ ՔԱՅԼ – Listen for incoming call invitations
    // ============================================================
    private void startCallInvitationListener(String userId) {
        callsRef = FirebaseDatabase.getInstance().getReference("calls").child(userId);
        callListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String channelName = snapshot.child("channelName").getValue(String.class);
                    String callerName = snapshot.child("callerName").getValue(String.class);

                    // Open CallActivity when an invitation is received
                    Intent intent = new Intent(SplashActivity.this, CallActivity.class);
                    intent.putExtra("channelName", channelName);
                    intent.putExtra("callerName", callerName);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    // Remove invitation after reading
                    snapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error (optional)
            }
        };
        callsRef.addValueEventListener(callListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener to avoid memory leaks
        if (callsRef != null && callListener != null) {
            callsRef.removeEventListener(callListener);
        }
    }
}