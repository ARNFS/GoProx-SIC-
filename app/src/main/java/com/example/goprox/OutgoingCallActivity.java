package com.example.goprox;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class OutgoingCallActivity extends AppCompatActivity {

    private static final int TIMEOUT_MS = 30_000;

    private FirebaseFirestore db;
    private String currentUserId, otherUserId, channelName, remoteUserName, serviceTitle;
    private ListenerRegistration statusListener;
    private volatile boolean callEnded = false;

    private MediaPlayer dialTonePlayer;
    private Handler timeoutHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        timeoutHandler = new Handler();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        channelName = intent.getStringExtra("channelName");
        currentUserId = intent.getStringExtra("currentUid");
        otherUserId = intent.getStringExtra("otherUserId");
        remoteUserName = intent.getStringExtra("remoteUserName");
        serviceTitle = intent.getStringExtra("serviceTitle");

        if (channelName == null || currentUserId == null || otherUserId == null) {
            Toast.makeText(this, "Call data error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvName = findViewById(R.id.tvCalleeName);
        TextView tvService = findViewById(R.id.tvServiceTitle);
        ImageView ivPhoto = findViewById(R.id.ivCalleePhoto);
        ImageButton btnCancel = findViewById(R.id.btnCancelCall);

        if (tvName != null) tvName.setText(remoteUserName != null ? remoteUserName : "User");
        if (tvService != null) {
            tvService.setText(serviceTitle != null && !serviceTitle.isEmpty()
                    ? serviceTitle : "");
        }
        if (ivPhoto != null) {
            ivPhoto.setImageResource(R.drawable.ic_profile_placeholder);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> cancelCall());
        }

        startDialTone();
        listenForStatus();

        timeoutHandler.postDelayed(() -> {
            if (!callEnded) cancelCall();
        }, TIMEOUT_MS);
    }

    private void listenForStatus() {
        if (currentUserId == null) return;

        statusListener = db.collection("calls").document(currentUserId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || callEnded) return;

                    if (snap == null || !snap.exists()) {
                        callEnded = true;
                        CallManager.getInstance().setInCall(false);
                        CallManager.getInstance().resumeListening();
                        runOnUiThread(this::finishActivitySafe);
                        return;
                    }

                    String status = snap.getString("status");

                    if ("accepted".equals(status)) {
                        callEnded = true;
                        stopDialTone();
                        if (timeoutHandler != null) {
                            timeoutHandler.removeCallbacksAndMessages(null);
                        }
                        if (statusListener != null) {
                            statusListener.remove();
                            statusListener = null;
                        }
                        runOnUiThread(this::openCallActivity);

                    } else if ("declined".equals(status) || "cancelled".equals(status)) {
                        callEnded = true;
                        stopDialTone();
                        CallManager.getInstance().setInCall(false);
                        CallManager.getInstance().resumeListening();
                        if (timeoutHandler != null) {
                            timeoutHandler.removeCallbacksAndMessages(null);
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(OutgoingCallActivity.this,
                                    "Call declined", Toast.LENGTH_SHORT).show();
                            finishActivitySafe();
                        });
                    }
                });
    }

    private void openCallActivity() {
        if (isFinishing() || isDestroyed()) return;
        try {
            Intent i = new Intent(this, CallActivity.class);
            i.putExtra("channelName", channelName);
            i.putExtra("currentUid", currentUserId);
            i.putExtra("otherUserId", otherUserId);
            i.putExtra("remoteUserName", remoteUserName);
            i.putExtra("serviceTitle", serviceTitle);
            i.putExtra("isCaller", true);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open call", Toast.LENGTH_SHORT).show();
        }
        finishActivitySafe();
    }

    private void cancelCall() {
        if (callEnded) return;
        callEnded = true;

        stopDialTone();
        CallManager.getInstance().setInCall(false);
        CallManager.getInstance().resumeListening();

        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }

        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }

        try {
            if (otherUserId != null) {
                db.collection("calls").document(otherUserId).update("status", "cancelled");
                new Handler().postDelayed(() -> {
                    db.collection("calls").document(otherUserId).delete();
                }, 500);
            }
            if (currentUserId != null) {
                db.collection("calls").document(currentUserId).delete();
            }
        } catch (Exception ignored) {}

        finishActivitySafe();
    }

    private void startDialTone() {
        try {
            dialTonePlayer = MediaPlayer.create(this, R.raw.outgoing_call);
            if (dialTonePlayer != null) {
                dialTonePlayer.setLooping(true);
                dialTonePlayer.start();
            }
        } catch (Exception ignored) {}
    }

    private void stopDialTone() {
        try {
            if (dialTonePlayer != null) {
                if (dialTonePlayer.isPlaying()) dialTonePlayer.stop();
                dialTonePlayer.release();
                dialTonePlayer = null;
            }
        } catch (Exception ignored) {
            dialTonePlayer = null;
        }
    }

    private void finishActivitySafe() {
        try {
            if (!isFinishing() && !isDestroyed()) {
                finish();
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDialTone();
        CallManager.getInstance().setInCall(false);
        CallManager.getInstance().resumeListening();
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }
    }
}