package com.example.goprox;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class CallActivity extends AppCompatActivity {

    private static final String APP_ID = "7284362f08214193a948fa9ea9a54c90";
    private static final int PERM_REQ = 22;
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
    };

    private FrameLayout flRemote, flLocal;
    private ImageButton btnMute, btnCamera, btnEndCall, btnSpeaker;
    private TextView tvRemoteName, tvCallTimer;

    private RtcEngine rtcEngine;
    private boolean isMuted = false;
    private boolean isCameraOn = false;
    private boolean isSpeaker = true;
    private volatile boolean isFinishing = false;
    private boolean callConnected = false;
    private int localUid;

    private String channelName, currentUserId, otherUserId, remoteUserName, serviceTitle;
    private FirebaseFirestore db;
    private ListenerRegistration callEndListener;
    private Handler mainHandler;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private long callStartTime = 0;

    private MediaPlayer connectTonePlayer;
    private MediaPlayer disconnectTonePlayer;

    private float dX, dY;

    private final IRtcEngineEventHandler eventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                if (!isFinishing && !callConnected) {
                    callConnected = true;
                    playConnectTone();
                    setupRemoteVideo(uid);
                    startCallTimer();
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                if (!isFinishing) {
                    // 🔥 ՄԻԱՅՆ onUserOffline-ից, ոչ թե նաև endCall-ից
                    playDisconnectTone();
                    if (mainHandler != null) {
                        mainHandler.postDelayed(() -> {
                            if (!isFinishing) cleanupAndFinish();
                        }, 1200);
                    }
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(() -> {
                if (isFinishing) return;
                if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
                    setupRemoteVideo(uid);
                } else if (state == Constants.REMOTE_VIDEO_STATE_STOPPED) {
                    flRemote.removeAllViews();
                    flRemote.setBackgroundColor(Color.BLACK);
                }
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                if (!isFinishing && !callConnected) {
                    if (tvCallTimer != null) tvCallTimer.setVisibility(View.GONE);
                    updateNameLabel();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        mainHandler = new Handler(Looper.getMainLooper());
        timerHandler = new Handler(Looper.getMainLooper());

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

        db = FirebaseFirestore.getInstance();
        localUid = Math.abs(currentUserId.hashCode()) % 100000 + 1;

        initViews();
        setupViews();
        listenForCallEnd();

        if (checkPermissions()) {
            initAgora();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERM_REQ);
        }
    }

    private void initViews() {
        flRemote = findViewById(R.id.flRemoteVideo);
        flLocal = findViewById(R.id.flLocalVideo);
        btnMute = findViewById(R.id.btnMute);
        btnCamera = findViewById(R.id.btnCamera);
        btnEndCall = findViewById(R.id.btnEndCall);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        tvRemoteName = findViewById(R.id.tvRemoteName);
        tvCallTimer = findViewById(R.id.tvCallTimer);

        if (flRemote == null || flLocal == null || btnEndCall == null) {
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        flRemote.setBackgroundColor(Color.BLACK);
        if (tvCallTimer != null) tvCallTimer.setVisibility(View.GONE);
    }

    private void setupViews() {
        flLocal.setOnTouchListener((v, event) -> {
            if (v == null || event == null) return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.animate()
                            .x(event.getRawX() + dX)
                            .y(event.getRawY() + dY)
                            .setDuration(0).start();
                    break;
            }
            return true;
        });

        btnEndCall.setOnClickListener(v -> {
            if (!isFinishing) endCall();
        });

        if (btnMute != null) {
            btnMute.setOnClickListener(v -> {
                if (isFinishing || rtcEngine == null) return;
                isMuted = !isMuted;
                rtcEngine.muteLocalAudioStream(isMuted);
                btnMute.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
            });
        }

        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                if (isFinishing || rtcEngine == null) return;
                isCameraOn = !isCameraOn;
                rtcEngine.muteLocalVideoStream(!isCameraOn);
                flLocal.setVisibility(isCameraOn ? View.VISIBLE : View.INVISIBLE);
                btnCamera.setImageResource(isCameraOn ? R.drawable.ic_camera_on : R.drawable.ic_camera_off);
            });
        }

        if (btnSpeaker != null) {
            btnSpeaker.setOnClickListener(v -> {
                if (isFinishing || rtcEngine == null) return;
                isSpeaker = !isSpeaker;
                rtcEngine.setEnableSpeakerphone(isSpeaker);
                btnSpeaker.setImageResource(isSpeaker ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
            });
        }
    }

    private void updateNameLabel() {
        if (tvRemoteName != null) {
            String display = remoteUserName != null ? remoteUserName : "User";
            if (serviceTitle != null && !serviceTitle.isEmpty()) {
                display += " • " + serviceTitle;
            }
            tvRemoteName.setText(display);
        }
    }

    private void listenForCallEnd() {
        if (currentUserId == null) return;
        callEndListener = db.collection("calls").document(currentUserId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || isFinishing) return;
                    if (snap == null || !snap.exists()) {
                        runOnUiThread(() -> {
                            if (!isFinishing) {
                                // 🔥 ՄԻԱՅՆ listener-ից, ոչ թե նաև endCall-ից
                                playDisconnectTone();
                                mainHandler.postDelayed(this::cleanupAndFinish, 1200);
                            }
                        });
                    }
                });
    }

    private void endCall() {
        if (isFinishing) return;
        isFinishing = true;

        // 🔥 ԱՌԱՆՑ disconnect tone-ի — այն կհնչի onUserOffline-ից կամ listener-ից
        if (currentUserId != null) db.collection("calls").document(currentUserId).delete();
        if (otherUserId != null) db.collection("calls").document(otherUserId).delete();

        if (mainHandler != null) {
            mainHandler.postDelayed(this::cleanupAndFinish, 1200);
        } else {
            cleanupAndFinish();
        }
    }

    private void cleanupAndFinish() {
        CallManager.getInstance().setInCall(false);
        CallManager.getInstance().resumeListening();

        stopAllMedia();
        if (callEndListener != null) { callEndListener.remove(); callEndListener = null; }
        if (rtcEngine != null) {
            try { rtcEngine.leaveChannel(); } catch (Exception ignored) {}
            RtcEngine.destroy(); rtcEngine = null;
        }
        stopCallTimer();
        if (mainHandler != null) { mainHandler.removeCallbacksAndMessages(null); mainHandler = null; }
        if (timerHandler != null) { timerHandler.removeCallbacksAndMessages(null); timerHandler = null; }
        try { finish(); } catch (Exception ignored) {}
    }

    private void startCallTimer() {
        callStartTime = System.currentTimeMillis();
        timerRunnable = () -> {
            if (!isFinishing && tvCallTimer != null) {
                long elapsed = System.currentTimeMillis() - callStartTime;
                long seconds = elapsed / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;
                tvCallTimer.setText(String.format("%02d:%02d", minutes, seconds));
                tvCallTimer.setVisibility(View.VISIBLE);
                if (timerHandler != null) timerHandler.postDelayed(timerRunnable, 1000);
            }
        };
        if (timerHandler != null) timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopCallTimer() {
        if (timerHandler != null && timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    private void playConnectTone() {
        try {
            connectTonePlayer = MediaPlayer.create(this, R.raw.call_connect);
            if (connectTonePlayer != null) {
                connectTonePlayer.start();
                connectTonePlayer.setOnCompletionListener(mp -> { mp.release(); connectTonePlayer = null; });
            }
        } catch (Exception ignored) {}
    }

    private void playDisconnectTone() {
        try {
            if (disconnectTonePlayer != null) return; // 🔥 Արդեն նվագում ա, չկրկնել
            disconnectTonePlayer = MediaPlayer.create(this, R.raw.call_disconnect);
            if (disconnectTonePlayer != null) {
                disconnectTonePlayer.start();
                disconnectTonePlayer.setOnCompletionListener(mp -> { mp.release(); disconnectTonePlayer = null; });
            }
        } catch (Exception ignored) {}
    }

    private void stopAllMedia() {
        try {
            if (connectTonePlayer != null) {
                if (connectTonePlayer.isPlaying()) connectTonePlayer.stop();
                connectTonePlayer.release(); connectTonePlayer = null;
            }
        } catch (Exception ignored) {}
        try {
            if (disconnectTonePlayer != null) {
                if (disconnectTonePlayer.isPlaying()) disconnectTonePlayer.stop();
                disconnectTonePlayer.release(); disconnectTonePlayer = null;
            }
        } catch (Exception ignored) {}
    }

    private void initAgora() {
        if (isFinishing) return;

        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = APP_ID;
            config.mEventHandler = eventHandler;
            rtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            Toast.makeText(this, "Agora init failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            cleanupAndFinish();
            return;
        }

        if (rtcEngine == null) {
            Toast.makeText(this, "Agora init failed", Toast.LENGTH_SHORT).show();
            cleanupAndFinish();
            return;
        }

        try {
            rtcEngine.enableVideo();
            rtcEngine.enableAudio();

            // 🔥 MAX ՁԱՅՆ — բարձր որակ + game streaming
            rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO, Constants.AUDIO_SCENARIO_GAME_STREAMING);
            rtcEngine.setEnableSpeakerphone(true);
            rtcEngine.adjustRecordingSignalVolume(400);
            rtcEngine.adjustPlaybackSignalVolume(400);
            rtcEngine.setInEarMonitoringVolume(400);

            rtcEngine.muteLocalVideoStream(true);
            rtcEngine.muteLocalAudioStream(false);
            if (flLocal != null) flLocal.setVisibility(View.INVISIBLE);
            if (btnCamera != null) btnCamera.setImageResource(R.drawable.ic_camera_off);
            if (btnMute != null) btnMute.setImageResource(R.drawable.ic_mic_on);

            setupLocalVideo();
            joinChannel();
        } catch (Exception e) {
            Toast.makeText(this, "Agora setup failed", Toast.LENGTH_SHORT).show();
            cleanupAndFinish();
        }
    }

    private void setupLocalVideo() {
        if (isFinishing || rtcEngine == null || flLocal == null) return;
        try {
            SurfaceView view = new SurfaceView(this);
            view.setZOrderMediaOverlay(true);
            flLocal.removeAllViews();
            flLocal.addView(view);
            rtcEngine.setupLocalVideo(new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, localUid));
            rtcEngine.startPreview();
        } catch (Exception e) {
            Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRemoteVideo(int uid) {
        if (isFinishing || rtcEngine == null || flRemote == null || uid == localUid) return;
        try {
            SurfaceView view = new SurfaceView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            view.setLayoutParams(params);
            view.setZOrderMediaOverlay(false);
            flRemote.removeAllViews();
            flRemote.setBackgroundColor(Color.TRANSPARENT);
            flRemote.addView(view);
            rtcEngine.setupRemoteVideo(new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid));
            updateNameLabel();
        } catch (Exception e) {
            flRemote.setBackgroundColor(Color.BLACK);
        }
    }

    private void joinChannel() {
        if (isFinishing || rtcEngine == null || channelName == null) return;
        try {
            ChannelMediaOptions opts = new ChannelMediaOptions();
            opts.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            opts.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            opts.publishCameraTrack = true;
            opts.publishMicrophoneTrack = true;
            opts.autoSubscribeAudio = true;
            opts.autoSubscribeVideo = true;
            rtcEngine.joinChannel(null, channelName, localUid, opts);
        } catch (Exception e) {
            Toast.makeText(this, "Channel join failed", Toast.LENGTH_SHORT).show();
            cleanupAndFinish();
        }
    }

    private boolean checkPermissions() {
        for (String p : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQ) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
            }
            if (allGranted) initAgora();
            else {
                Toast.makeText(this, "Camera and microphone are required for calls", Toast.LENGTH_SHORT).show();
                cleanupAndFinish();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupAndFinish();
    }
}