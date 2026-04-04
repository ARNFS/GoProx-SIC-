package com.example.goprox;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.List;

public class CallActivity extends AppCompatActivity {

    private static final String TAG = "CallActivity";

    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private com.google.android.material.button.MaterialButton btnSwitchCamera;
    private com.google.android.material.button.MaterialButton btnMute;
    private com.google.android.material.button.MaterialButton btnEndCall;
    private android.widget.TextView tvStatus;

    private PeerConnection peerConnection;
    private PeerConnectionFactory peerConnectionFactory;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private final EglBase eglBase = EglBase.create();

    private FirebaseDatabase database;
    private DatabaseReference signalingRef;

    private String roomId = "";
    private boolean isInitiator = false;
    private boolean isMuted = false;

    private static final int CAMERA_MIC_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        Log.d(TAG, "CallActivity started");

        localVideoView = findViewById(R.id.local_video_view);
        remoteVideoView = findViewById(R.id.remote_video_view);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnMute = findViewById(R.id.btn_mute);
        btnEndCall = findViewById(R.id.btn_end_call);
        tvStatus = findViewById(R.id.tv_status);

        if (localVideoView == null || remoteVideoView == null) {
            Toast.makeText(this, "Էկրանը ճիշտ չի բեռնվել", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String specialistId = getIntent().getStringExtra("SPECIALIST_ID");
        if (specialistId == null || specialistId.isEmpty()) {
            Toast.makeText(this, "Մասնագետի ID-ն բացակայում է", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomId = "call_" + specialistId;
        isInitiator = getIntent().getBooleanExtra("IS_CALLER", false);

        Log.d(TAG, "Room ID: " + roomId + " | Is Caller: " + isInitiator);

        database = FirebaseDatabase.getInstance();
        signalingRef = database.getReference("calls/" + roomId);

        checkPermissions();
        setupButtons();
    }

    private void checkPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_MIC_PERMISSION_CODE);
        } else {
            initWebRTC();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_MIC_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initWebRTC();
        } else {
            Toast.makeText(this, "Թույլտվությունները պետք են տեսազանգի համար", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initWebRTC() {
        Log.d(TAG, "Initializing WebRTC...");
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory();

        localVideoView.init(eglBase.getEglBaseContext(), null);
        remoteVideoView.init(eglBase.getEglBaseContext(), null);
        localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        startLocalStream();
    }

    private void startLocalStream() {
        VideoCapturer videoCapturer = createCameraCapturer(new Camera1Enumerator(false));
        if (videoCapturer == null) {
            Toast.makeText(this, "Camera չի գտնվել", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);

        localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource);
        localVideoTrack.addSink(localVideoView);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource);

        createPeerConnection();

        tvStatus.setText(isInitiator ? "Ստեղծում եմ առաջարկ..." : "Սպասում եմ զանգի...");

        if (isInitiator) {
            createOffer();
        } else {
            listenForSignals();
        }
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return capturer;
            }
        }
        return null;
    }

    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override public void onSignalingChange(PeerConnection.SignalingState signalingState) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}
            @Override public void onIceConnectionReceivingChange(boolean receiving) {}
            @Override public void onIceCandidate(IceCandidate iceCandidate) {
                signalingRef.child("ice_candidates").push().setValue(iceCandidateToJson(iceCandidate));
            }
            @Override public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                // Այս մեթոդը պետք է լինի, նույնիսկ եթե դատարկ է
            }
            @Override public void onAddStream(MediaStream mediaStream) {
                runOnUiThread(() -> {
                    if (!mediaStream.videoTracks.isEmpty()) {
                        mediaStream.videoTracks.get(0).addSink(remoteVideoView);
                    }
                });
            }
            @Override public void onRemoveStream(MediaStream mediaStream) {}
            @Override public void onDataChannel(DataChannel dataChannel) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {}
            @Override public void onTrack(RtpTransceiver rtpTransceiver) {}
        });

        MediaStream localStream = peerConnectionFactory.createLocalMediaStream("local_stream");
        if (localVideoTrack != null) localStream.addTrack(localVideoTrack);
        if (localAudioTrack != null) localStream.addTrack(localAudioTrack);
        peerConnection.addStream(localStream);
    }

    private void createOffer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

        peerConnection.createOffer(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(this, sdp);
                signalingRef.child("offer").setValue(sessionDescriptionToJson(sdp));
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) {}
            @Override public void onSetFailure(String s) {}
        }, constraints);
    }

    private void listenForSignals() {
        signalingRef.child("offer").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String json = snapshot.getValue(String.class);
                if (json == null) return;
                SessionDescription sdp = sessionDescriptionFromJson(json);
                if (sdp != null) {
                    peerConnection.setRemoteDescription(new SdpObserver() {
                        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                        @Override public void onSetSuccess() { createAnswer(); }
                        @Override public void onCreateFailure(String s) {}
                        @Override public void onSetFailure(String s) {}
                    }, sdp);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        signalingRef.child("answer").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String json = snapshot.getValue(String.class);
                if (json == null) return;
                SessionDescription sdp = sessionDescriptionFromJson(json);
                if (sdp != null) {
                    peerConnection.setRemoteDescription(new SdpObserver() {
                        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                        @Override public void onSetSuccess() {}
                        @Override public void onCreateFailure(String s) {}
                        @Override public void onSetFailure(String s) {}
                    }, sdp);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        signalingRef.child("ice_candidates").addChildEventListener(new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String json = snapshot.getValue(String.class);
                if (json == null) return;
                IceCandidate candidate = iceCandidateFromJson(json);
                if (candidate != null) peerConnection.addIceCandidate(candidate);
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createAnswer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

        peerConnection.createAnswer(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(this, sdp);
                signalingRef.child("answer").setValue(sessionDescriptionToJson(sdp));
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) {}
            @Override public void onSetFailure(String s) {}
        }, constraints);
    }

    private void setupButtons() {
        btnEndCall.setOnClickListener(v -> endCall());
        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            if (localAudioTrack != null) localAudioTrack.setEnabled(!isMuted);
            btnMute.setText(isMuted ? "🔊" : "🔇");
        });
    }

    private void endCall() {
        if (peerConnection != null) peerConnection.close();
        if (signalingRef != null) signalingRef.removeValue();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localVideoTrack != null) localVideoTrack.dispose();
        if (localAudioTrack != null) localAudioTrack.dispose();
        if (peerConnection != null) peerConnection.close();
        eglBase.release();
    }

    // ====================== JSON HELPER METHODS ======================
    private String sessionDescriptionToJson(SessionDescription sdp) {
        return sdp.type.canonicalForm() + ":" + sdp.description;
    }

    private SessionDescription sessionDescriptionFromJson(String json) {
        String[] parts = json.split(":", 2);
        if (parts.length != 2) return null;
        SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(parts[0]);
        return type != null ? new SessionDescription(type, parts[1]) : null;
    }

    private String iceCandidateToJson(IceCandidate candidate) {
        return candidate.sdpMid + ":" + candidate.sdpMLineIndex + ":" + candidate.sdp;
    }

    private IceCandidate iceCandidateFromJson(String json) {
        String[] parts = json.split(":", 3);
        if (parts.length != 3) return null;
        return new IceCandidate(parts[0], Integer.parseInt(parts[1]), parts[2]);
    }
}