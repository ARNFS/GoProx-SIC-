package com.example.goprox;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.Map;

public class CallActivity extends AppCompatActivity implements WebRTCManager.SignalingListener {

    private static final String TAG = "CallActivity";

    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private FrameLayout remoteVideoContainer;
    private ImageButton btnEndCall, btnToggleMic, btnToggleCamera, btnToggleSpeaker, btnFlipCamera;
    private TextView tvCallStatus;

    private WebRTCManager webRTCManager;
    private FirebaseFirestore firestore;
    private ListenerRegistration callListener;
    private String currentUserId, targetUserId, roomName;
    private boolean isCaller;
    private boolean isAudioOnly;
    private boolean micEnabled = true;
    private boolean cameraEnabled = true;
    private boolean speakerEnabled = false;
    private AudioManager audioManager;
    private boolean isCallEnded = false;
    private final Handler statusHandler = new Handler();

    private MediaPlayer ringtonePlayer;   // для гудков

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        targetUserId = getIntent().getStringExtra("SPECIALIST_ID");
        isAudioOnly = getIntent().getBooleanExtra("IS_AUDIO_ONLY", false);
        isCaller = getIntent().getBooleanExtra("IS_CALLER", true);

        if (targetUserId == null) {
            Toast.makeText(this, "Invalid call data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomName = (currentUserId.compareTo(targetUserId) < 0) ?
                currentUserId + "_" + targetUserId : targetUserId + "_" + currentUserId;

        firestore = FirebaseFirestore.getInstance();

        initViews();
        setupWebRTC();

        if (isCaller) {
            startOutgoingCall();
        } else {
            showIncomingCallDialog();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                endCall();
            }
        });
    }

    private void initViews() {
        localVideoView = findViewById(R.id.localVideoView);
        remoteVideoView = findViewById(R.id.remoteVideoView);
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer);
        btnEndCall = findViewById(R.id.btnEndCall);
        btnToggleMic = findViewById(R.id.btnToggleMic);
        btnToggleCamera = findViewById(R.id.btnToggleCamera);
        btnToggleSpeaker = findViewById(R.id.btnToggleSpeaker);
        btnFlipCamera = findViewById(R.id.btnFlipCamera);
        tvCallStatus = findViewById(R.id.tvCallStatus);

        btnEndCall.setOnClickListener(v -> endCall());
        btnToggleMic.setOnClickListener(v -> toggleMic());
        btnToggleCamera.setOnClickListener(v -> toggleCamera());
        btnToggleSpeaker.setOnClickListener(v -> toggleSpeaker());
        btnFlipCamera.setOnClickListener(v -> flipCamera());

        // Для аудиозвонка полностью скрываем видео
        if (isAudioOnly) {
            localVideoView.setVisibility(View.GONE);
            remoteVideoContainer.setVisibility(View.GONE);
            btnToggleCamera.setVisibility(View.GONE);
            btnFlipCamera.setVisibility(View.GONE);
        }
    }

    private void setupWebRTC() {
        webRTCManager = new WebRTCManager(this, localVideoView, remoteVideoView, this);
        webRTCManager.startLocalStream(isAudioOnly);
        webRTCManager.createPeerConnection();
    }

    private void startOutgoingCall() {
        tvCallStatus.setText("Calling...");
        tvCallStatus.setVisibility(View.VISIBLE);
        webRTCManager.createOffer();

        // --- Запускаем гудки ---
        startRingtone();

        Map<String, Object> callData = new HashMap<>();
        callData.put("callerId", currentUserId);
        callData.put("calleeId", targetUserId);
        callData.put("status", "ringing");
        callData.put("audioOnly", isAudioOnly);
        callData.put("timestamp", FieldValue.serverTimestamp());

        firestore.collection("calls").document(roomName)
                .set(callData, SetOptions.merge())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to initiate call", Toast.LENGTH_SHORT).show();
                    stopRingtone();
                    finish();
                });

        listenForCallUpdates();
    }

    private void startRingtone() {
        try {
            ringtonePlayer = MediaPlayer.create(this, R.raw.ringtone);
            if (ringtonePlayer != null) {
                ringtonePlayer.setLooping(true);
                ringtonePlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ringtone error: " + e.getMessage());
        }
    }

    private void stopRingtone() {
        if (ringtonePlayer != null) {
            try {
                ringtonePlayer.stop();
                ringtonePlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping ringtone: " + e.getMessage());
            }
            ringtonePlayer = null;
        }
    }

    private void showIncomingCallDialog() {
        // Загружаем имя звонящего
        firestore.collection("users").document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    String callerName = doc.getString("name");
                    if (callerName == null) callerName = "User";

                    new AlertDialog.Builder(this)
                            .setTitle("Incoming " + (isAudioOnly ? "Audio" : "Video") + " Call")
                            .setMessage(callerName + " is calling you...")
                            .setPositiveButton("Accept", (dialog, which) -> acceptCall())
                            .setNegativeButton("Decline", (dialog, which) -> rejectCall())
                            .setCancelable(false)
                            .show();
                });
    }

    private void acceptCall() {
        tvCallStatus.setVisibility(View.GONE);
        firestore.collection("calls").document(roomName)
                .update("status", "accepted");
        listenForCallUpdates();
    }

    private void rejectCall() {
        firestore.collection("calls").document(roomName)
                .update("status", "rejected")
                .addOnCompleteListener(t -> finish());
    }

    private void listenForCallUpdates() {
        DocumentReference callRef = firestore.collection("calls").document(roomName);
        callListener = callRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                return;
            }

            String status = snapshot.getString("status");

            if ("ended".equals(status) || "rejected".equals(status)) {
                if (!isCallEnded) {
                    isCallEnded = true;
                    Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }

            if ("accepted".equals(status)) {
                stopRingtone();
                tvCallStatus.setVisibility(View.GONE);
            }

            // Обработка SDP
            Map<String, Object> offer = (Map<String, Object>) snapshot.get("offer");
            Map<String, Object> answer = (Map<String, Object>) snapshot.get("answer");

            if (!isCaller && offer != null) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.OFFER,
                        (String) offer.get("sdp")
                );
                webRTCManager.onRemoteSdpReceived(sdp);
                callRef.update("offer", FieldValue.delete());
            }

            if (isCaller && answer != null) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.ANSWER,
                        (String) answer.get("sdp")
                );
                webRTCManager.onRemoteSdpReceived(sdp);
                callRef.update("answer", FieldValue.delete());
            }

            // Обработка ICE
            Map<String, Object> callerIce = (Map<String, Object>) snapshot.get("callerIce");
            Map<String, Object> calleeIce = (Map<String, Object>) snapshot.get("calleeIce");

            if (!isCaller && callerIce != null) {
                IceCandidate candidate = new IceCandidate(
                        (String) callerIce.get("sdpMid"),
                        ((Long) callerIce.get("sdpMLineIndex")).intValue(),
                        (String) callerIce.get("sdp")
                );
                webRTCManager.onRemoteIceCandidateReceived(candidate);
            }

            if (isCaller && calleeIce != null) {
                IceCandidate candidate = new IceCandidate(
                        (String) calleeIce.get("sdpMid"),
                        ((Long) calleeIce.get("sdpMLineIndex")).intValue(),
                        (String) calleeIce.get("sdp")
                );
                webRTCManager.onRemoteIceCandidateReceived(candidate);
            }
        });
    }

    @Override
    public void onSendSdp(SessionDescription sdp) {
        Map<String, Object> sdpMap = new HashMap<>();
        sdpMap.put("type", sdp.type.canonicalForm());
        sdpMap.put("sdp", sdp.description);

        DocumentReference callRef = firestore.collection("calls").document(roomName);
        if (sdp.type == SessionDescription.Type.OFFER) {
            callRef.update("offer", sdpMap);
        } else {
            callRef.update("answer", sdpMap);
        }
    }

    @Override
    public void onSendIceCandidate(IceCandidate candidate) {
        Map<String, Object> iceMap = new HashMap<>();
        iceMap.put("sdpMid", candidate.sdpMid);
        iceMap.put("sdpMLineIndex", candidate.sdpMLineIndex);
        iceMap.put("sdp", candidate.sdp);

        DocumentReference callRef = firestore.collection("calls").document(roomName);
        if (isCaller) {
            callRef.update("callerIce", iceMap);
        } else {
            callRef.update("calleeIce", iceMap);
        }
    }

    private void toggleMic() {
        micEnabled = !micEnabled;
        webRTCManager.setMicEnabled(micEnabled);
        btnToggleMic.setImageResource(micEnabled ? R.drawable.ic_mic_on : R.drawable.ic_mic_off);
    }

    private void toggleCamera() {
        cameraEnabled = !cameraEnabled;
        webRTCManager.setCameraEnabled(cameraEnabled);
        btnToggleCamera.setImageResource(cameraEnabled ? R.drawable.ic_camera_on : R.drawable.ic_camera_off);
    }

    private void toggleSpeaker() {
        speakerEnabled = !speakerEnabled;
        audioManager.setSpeakerphoneOn(speakerEnabled);
        btnToggleSpeaker.setImageResource(speakerEnabled ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
    }

    private void flipCamera() {
        webRTCManager.flipCamera();
    }

    private void endCall() {
        isCallEnded = true;
        stopRingtone();
        firestore.collection("calls").document(roomName)
                .update("status", "ended")
                .addOnCompleteListener(task -> {
                    if (webRTCManager != null) {
                        webRTCManager.endCall();
                    }
                    finish();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
        if (callListener != null) {
            callListener.remove();
        }
        if (webRTCManager != null) {
            webRTCManager.endCall();
        }
        statusHandler.removeCallbacksAndMessages(null);
    }
}