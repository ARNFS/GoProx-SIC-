package com.example.goprox;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class CallActivity extends AppCompatActivity {

    private static final String APP_ID = "fc7f50959be9423aa7e6e37d6723f287";
    private static final String TOKEN = null; // null for testing
    private static final int PERMISSION_REQ_ID = 22;

    private RtcEngine mRtcEngine;
    private boolean isMuted = false;
    private boolean isCameraOn = true;
    private String channelName;
    private boolean isCaller;

    private FrameLayout localVideoContainer;
    private FrameLayout remoteVideoContainer;
    private ImageButton btnToggleMic, btnToggleCamera, btnEndCall;

    // Sound
    private MediaPlayer ringtonePlayer;
    private MediaPlayer callingTonePlayer;
    private Handler soundHandler = new Handler();
    private boolean isSoundPlaying = false;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                Toast.makeText(CallActivity.this, "Remote user joined: " + uid, Toast.LENGTH_SHORT).show();
                setupRemoteVideo(uid);
                stopAllSounds();
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                Toast.makeText(CallActivity.this, "Remote user left", Toast.LENGTH_SHORT).show();
                if (remoteVideoContainer != null) remoteVideoContainer.removeAllViews();
                playDisconnectSound();
                new Handler().postDelayed(() -> finish(), 1000);
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                Toast.makeText(CallActivity.this, "Joined channel: " + channel, Toast.LENGTH_SHORT).show();
                startCallSounds();
            });
        }

        @Override
        public void onError(int err) {
            runOnUiThread(() -> {
                Toast.makeText(CallActivity.this, "Agora error: " + err, Toast.LENGTH_LONG).show();
                stopAllSounds();
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_call);

        channelName = getIntent().getStringExtra("channel_name");
        isCaller = getIntent().getBooleanExtra("is_caller", true);

        if (channelName == null) channelName = "test_channel";

        localVideoContainer = findViewById(R.id.local_video_container);
        remoteVideoContainer = findViewById(R.id.remote_video_container);
        btnToggleMic = findViewById(R.id.btn_toggle_mic);
        btnToggleCamera = findViewById(R.id.btn_toggle_camera);
        btnEndCall = findViewById(R.id.btn_end_call);

        if (checkPermissions()) {
            initializeAgora();
        }

        btnToggleMic.setOnClickListener(v -> toggleMic());
        btnToggleCamera.setOnClickListener(v -> toggleCamera());
        btnEndCall.setOnClickListener(v -> endCall());
    }

    // ==================== SOUND METHODS ====================
    private void startCallSounds() {
        if (isCaller) {
            playCallingTone();
        } else {
            playRingtone();
        }
    }

    private void playRingtone() {
        try {
            if (ringtonePlayer != null) ringtonePlayer.release();
            ringtonePlayer = MediaPlayer.create(this, R.raw.ringtone);
            if (ringtonePlayer != null) {
                ringtonePlayer.setLooping(true);
                ringtonePlayer.start();
                isSoundPlaying = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playCallingTone() {
        try {
            if (callingTonePlayer != null) callingTonePlayer.release();
            callingTonePlayer = MediaPlayer.create(this, R.raw.calling_tone);
            if (callingTonePlayer != null) {
                callingTonePlayer.setLooping(true);
                callingTonePlayer.start();
                isSoundPlaying = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playConnectSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.call_connect);
            if (mp != null) {
                mp.start();
                mp.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playDisconnectSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.call_end_sound);
            if (mp != null) {
                mp.start();
                mp.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAllSounds() {
        isSoundPlaying = false;
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
        if (callingTonePlayer != null) {
            callingTonePlayer.stop();
            callingTonePlayer.release();
            callingTonePlayer = null;
        }
        playConnectSound();
    }
    // ======================================================

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQ_ID);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                initializeAgora();
            } else {
                Toast.makeText(this, "Camera & microphone permissions are required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = APP_ID;
            config.mEventHandler = mRtcEventHandler;

            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.enableVideo();

            VideoEncoderConfiguration encoderConfig = new VideoEncoderConfiguration();
            encoderConfig.dimensions = new VideoEncoderConfiguration.VideoDimensions(640, 360);
            encoderConfig.frameRate = 15;
            encoderConfig.bitrate = VideoEncoderConfiguration.STANDARD_BITRATE;
            encoderConfig.orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;
            mRtcEngine.setVideoEncoderConfiguration(encoderConfig);

            setupLocalVideo();
            joinChannel();

        } catch (Exception e) {
            Toast.makeText(this, "Agora init failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setupLocalVideo() {
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        localVideoContainer.addView(surfaceView);

        VideoCanvas localCanvas = new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
        mRtcEngine.setupLocalVideo(localCanvas);
        mRtcEngine.startPreview();
    }

    private void setupRemoteVideo(int uid) {
        remoteVideoContainer.removeAllViews();

        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        remoteVideoContainer.addView(surfaceView);

        VideoCanvas remoteCanvas = new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid);
        mRtcEngine.setupRemoteVideo(remoteCanvas);
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;

        mRtcEngine.joinChannel(TOKEN, channelName, 0, options);
    }

    private void toggleMic() {
        isMuted = !isMuted;
        mRtcEngine.muteLocalAudioStream(isMuted);
        btnToggleMic.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
    }

    private void toggleCamera() {
        isCameraOn = !isCameraOn;
        mRtcEngine.muteLocalVideoStream(!isCameraOn);
        btnToggleCamera.setImageResource(isCameraOn ? R.drawable.ic_camera_on : R.drawable.ic_camera_off);
    }

    private void endCall() {
        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
        playDisconnectSound();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAllSounds();
        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }
}