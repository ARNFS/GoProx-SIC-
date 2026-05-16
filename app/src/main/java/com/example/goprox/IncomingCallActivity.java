package com.example.goprox;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class IncomingCallActivity extends AppCompatActivity {

    private static final int PERM_REQ = 33;
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
    };

    private FirebaseFirestore db;
    private String callId, callerId, callerName, callerPhoto, channelName, serviceTitle;
    private String currentUserId;
    private ListenerRegistration statusListener;
    private MediaPlayer ringtonePlayer;
    private volatile boolean callHandled = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            } else {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                );
            }
        } catch (Exception ignored) {}

        setContentView(R.layout.activity_incoming_call);

        handler = new Handler();
        db = FirebaseFirestore.getInstance();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        Intent intent = getIntent();
        callId = intent.getStringExtra("callId");
        callerId = intent.getStringExtra("callerId");
        callerName = intent.getStringExtra("callerName");
        callerPhoto = intent.getStringExtra("callerPhotoUrl");
        if (callerPhoto == null) {
            callerPhoto = intent.getStringExtra("callerPhoto");
        }
        channelName = intent.getStringExtra("channelName");
        serviceTitle = intent.getStringExtra("serviceTitle");

        TextView tvName = findViewById(R.id.tvCallerName);
        TextView tvService = findViewById(R.id.tvServiceTitle);
        ImageView ivPhoto = findViewById(R.id.ivCallerPhoto);

        if (tvName != null) tvName.setText(callerName != null ? callerName : "Unknown");
        if (tvService != null) {
            tvService.setText(serviceTitle != null && !serviceTitle.isEmpty()
                    ? "For: " + serviceTitle : "GoProx Call");
        }

        if (ivPhoto != null) {
            if (callerPhoto != null && !callerPhoto.isEmpty()) {
                Glide.with(this)
                        .load(callerPhoto)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .transform(new CircleCrop())
                        .into(ivPhoto);
            } else {
                ivPhoto.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }

        ImageButton btnAccept = findViewById(R.id.btnAccept);
        ImageButton btnDecline = findViewById(R.id.btnDecline);

        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> {
                if (callHandled) return;
                if (checkPermissions()) acceptCall();
                else ActivityCompat.requestPermissions(this, PERMISSIONS, PERM_REQ);
            });
        }

        if (btnDecline != null) {
            btnDecline.setOnClickListener(v -> {
                if (!callHandled) declineCall();
            });
        }

        startRingtone();
        listenForCancellation();
    }

    private void listenForCancellation() {
        if (currentUserId == null) return;

        statusListener = db.collection("calls").document(currentUserId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || callHandled) return;
                    if (snap == null || !snap.exists()) {
                        callHandled = true;
                        runOnUiThread(() -> {
                            stopRingtone();
                            finish();
                        });
                        return;
                    }
                    String status = snap.getString("status");
                    if ("cancelled".equals(status)) {
                        callHandled = true;
                        runOnUiThread(() -> {
                            stopRingtone();
                            finish();
                        });
                    }
                });
    }

    private void acceptCall() {
        if (callHandled) return;
        callHandled = true;
        stopRingtone();

        if (currentUserId == null || callerId == null) {
            finish();
            return;
        }

        db.collection("calls").document(currentUserId).update("status", "accepted");
        db.collection("calls").document(callerId)
                .update("status", "accepted")
                .addOnSuccessListener(v -> {
                    Intent i = new Intent(this, CallActivity.class);
                    i.putExtra("channelName", channelName);
                    i.putExtra("currentUid", currentUserId);
                    i.putExtra("otherUserId", callerId);
                    i.putExtra("remoteUserName", callerName);
                    i.putExtra("serviceTitle", serviceTitle);
                    i.putExtra("isCaller", false);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to accept call", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void declineCall() {
        if (callHandled) return;
        callHandled = true;
        stopRingtone();

        if (currentUserId != null) {
            db.collection("calls").document(currentUserId).update("status", "declined");
        }
        if (callerId != null) {
            db.collection("calls").document(callerId)
                    .update("status", "declined")
                    .addOnCompleteListener(t -> finish());
        } else {
            finish();
        }
    }

    private void startRingtone() {
        try {
            ringtonePlayer = new MediaPlayer();
            // 🔥 STREAM_RING — զանգի ձայն, ոչ թե մեդիա
            ringtonePlayer.setAudioStreamType(AudioManager.STREAM_RING);
            ringtonePlayer.setDataSource(this, android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.incoming_call));
            ringtonePlayer.setLooping(true);
            ringtonePlayer.prepare();
            ringtonePlayer.start();
        } catch (Exception ignored) {}
    }

    private void stopRingtone() {
        try {
            if (ringtonePlayer != null) {
                if (ringtonePlayer.isPlaying()) ringtonePlayer.stop();
                ringtonePlayer.release();
                ringtonePlayer = null;
            }
        } catch (Exception ignored) {
            ringtonePlayer = null;
        }
    }

    private boolean checkPermissions() {
        for (String p : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int req,
                                           @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PERM_REQ) {
            boolean ok = true;
            for (int r : results) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                    break;
                }
            }
            if (ok) acceptCall();
            else Toast.makeText(this,
                    "Camera and microphone required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
}