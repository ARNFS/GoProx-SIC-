package com.example.goprox;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class CallActivity extends BaseActivity {

    private static final int PERMISSION_REQ_ID = 22;
    private static final String APP_ID = "a5179791fb8d43cc8ae79020e60e3360";

    private RtcEngine mRtcEngine;
    private FrameLayout mLocalVideoContainer;
    private FrameLayout mRemoteVideoContainer;
    private ImageButton mEndCallButton;
    private ImageButton mToggleMicButton;
    private ImageButton mToggleCameraButton;
    private ImageButton mShareCallButton;

    private String mChannelName;
    private boolean mIsAudioOnly;
    private boolean mIsMicEnabled = true;
    private boolean mIsCameraEnabled = true;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> Toast.makeText(CallActivity.this, "Joined channel", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                SurfaceView remoteView = new SurfaceView(CallActivity.this);
                mRemoteVideoContainer.addView(remoteView);
                mRtcEngine.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                Toast.makeText(CallActivity.this, "User joined", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                mRemoteVideoContainer.removeAllViews();
                Toast.makeText(CallActivity.this, "User left", Toast.LENGTH_SHORT).show();
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        mChannelName = getIntent().getStringExtra("channelName");
        mIsAudioOnly = getIntent().getBooleanExtra("IS_AUDIO_ONLY", false);

        if (mChannelName == null) {
            Toast.makeText(this, "Invalid call data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        checkPermissions();
    }

    private void initViews() {
        mLocalVideoContainer = findViewById(R.id.local_video_container);
        mRemoteVideoContainer = findViewById(R.id.remote_video_container);
        mEndCallButton = findViewById(R.id.btn_end_call);
        mToggleMicButton = findViewById(R.id.btn_toggle_mic);
        mToggleCameraButton = findViewById(R.id.btn_toggle_camera);
        mShareCallButton = findViewById(R.id.btn_share_call);

        mEndCallButton.setOnClickListener(v -> finish());
        mToggleMicButton.setOnClickListener(v -> toggleMic());
        mToggleCameraButton.setOnClickListener(v -> toggleCamera());
        mShareCallButton.setOnClickListener(v -> shareCallLink());

        if (mIsAudioOnly) {
            mToggleCameraButton.setVisibility(View.GONE);
        }
    }

    private void shareCallLink() {
        String callLink = "https://agora.io/call/" + mChannelName;

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("callLink", callLink);
        intent.putExtra("otherUserId", getOtherUserIdFromChannel());
        startActivity(intent);

        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("call link", callLink);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private String getOtherUserIdFromChannel() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String[] parts = mChannelName.split("_");
        if (parts.length == 2) {
            return parts[0].equals(currentUserId) ? parts[1] : parts[0];
        }
        return "";
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            initializeAgoraEngine();
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQ_ID);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAgoraEngine();
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initializeAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = APP_ID;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);

            mRtcEngine.enableVideo();
            mRtcEngine.enableAudio();

            SurfaceView localView = new SurfaceView(this);
            mLocalVideoContainer.addView(localView);
            mRtcEngine.setupLocalVideo(new VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
            mRtcEngine.startPreview();

            joinChannel();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to initialize Agora: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = !mIsAudioOnly;
        options.publishCameraTrack = !mIsAudioOnly;
        options.publishMicrophoneTrack = true;

        mRtcEngine.joinChannel(null, mChannelName, 0, options);
    }

    private void toggleMic() {
        mIsMicEnabled = !mIsMicEnabled;
        mRtcEngine.muteLocalAudioStream(!mIsMicEnabled);
        mToggleMicButton.setImageResource(mIsMicEnabled ? R.drawable.ic_mic_on : R.drawable.ic_mic_off);
    }

    private void toggleCamera() {
        mIsCameraEnabled = !mIsCameraEnabled;
        mRtcEngine.muteLocalVideoStream(!mIsCameraEnabled);
        mToggleCameraButton.setImageResource(mIsCameraEnabled ? R.drawable.ic_camera_on : R.drawable.ic_camera_off);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }
}