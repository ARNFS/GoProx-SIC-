package com.example.goprox;

import android.content.Context;
import android.util.Log;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.List;

public class WebRTCManager {

    private static final String TAG = "WebRTCManager";
    private static final String STUN_SERVER = "stun:stun.l.google.com:19302";

    private Context context;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoCapturer videoCapturer;
    private VideoSource videoSource;
    private EglBase eglBase;
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private SignalingListener signalingListener;
    private boolean isCameraEnabled = true;
    private boolean isFrontCamera = true;

    public interface SignalingListener {
        void onSendSdp(SessionDescription sdp);
        void onSendIceCandidate(IceCandidate candidate);
    }

    public WebRTCManager(Context context, SurfaceViewRenderer localView, SurfaceViewRenderer remoteView, SignalingListener listener) {
        this.context = context;
        this.localVideoView = localView;
        this.remoteVideoView = remoteView;
        this.signalingListener = listener;
        initialize();
    }

    private void initialize() {
        eglBase = EglBase.create();

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setOptions(options)
                .createPeerConnectionFactory();

        localVideoView.setMirror(true);
        localVideoView.init(eglBase.getEglBaseContext(), null);
        remoteVideoView.init(eglBase.getEglBaseContext(), null);
    }

    public void startLocalStream(boolean isAudioOnly) {
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio", peerConnectionFactory.createAudioSource(new MediaConstraints()));

        if (!isAudioOnly) {
            videoCapturer = createVideoCapturer(isFrontCamera);
            if (videoCapturer != null) {
                videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
                videoCapturer.initialize(SurfaceTextureHelper.create(Thread.currentThread().getName(), eglBase.getEglBaseContext()), context, videoSource.getCapturerObserver());
                videoCapturer.startCapture(1280, 720, 30);
                localVideoTrack = peerConnectionFactory.createVideoTrack("video", videoSource);
                localVideoTrack.addSink(localVideoView);
                localVideoTrack.setEnabled(isCameraEnabled);
            }
        }
    }

    private VideoCapturer createVideoCapturer(boolean frontFacing) {
        Camera2Enumerator enumerator = new Camera2Enumerator(context);
        String[] deviceNames = enumerator.getDeviceNames();
        for (String name : deviceNames) {
            if (frontFacing && enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null);
            }
            if (!frontFacing && enumerator.isBackFacing(name)) {
                return enumerator.createCapturer(name, null);
            }
        }
        if (deviceNames.length > 0) {
            return enumerator.createCapturer(deviceNames[0], null);
        }
        return null;
    }

    public void flipCamera() {
        if (videoCapturer == null || videoSource == null || localVideoTrack == null) return;
        // Проверка, что трек ещё не disposed
        try {
            localVideoTrack.id(); // если disposed, выбросит IllegalStateException
        } catch (IllegalStateException e) {
            Log.w(TAG, "Video track disposed, cannot flip camera");
            return;
        }

        try {
            videoCapturer.stopCapture();
            videoCapturer.dispose();
        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping capture: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        isFrontCamera = !isFrontCamera;
        videoCapturer = createVideoCapturer(isFrontCamera);

        if (videoCapturer != null) {
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(SurfaceTextureHelper.create(Thread.currentThread().getName(), eglBase.getEglBaseContext()), context, videoSource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);

            VideoTrack newVideoTrack = peerConnectionFactory.createVideoTrack("video", videoSource);
            newVideoTrack.addSink(localVideoView);
            newVideoTrack.setEnabled(isCameraEnabled);

            if (peerConnection != null && localVideoTrack != null) {
                for (RtpSender sender : peerConnection.getSenders()) {
                    if (sender.track() != null && sender.track().id().equals(localVideoTrack.id())) {
                        sender.setTrack(newVideoTrack, true);
                        break;
                    }
                }
            }

            localVideoTrack.dispose();
            localVideoTrack = newVideoTrack;
        }

        localVideoView.setMirror(isFrontCamera);
    }

    public void setMicEnabled(boolean enabled) {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(enabled);
        }
    }

    public void setCameraEnabled(boolean enabled) {
        isCameraEnabled = enabled;
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(enabled);
        }
    }

    public void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder(STUN_SERVER).createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {}
            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "ICE state: " + iceConnectionState);
            }
            @Override
            public void onIceConnectionReceivingChange(boolean b) {}
            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                signalingListener.onSendIceCandidate(iceCandidate);
            }
            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}
            @Override
            public void onAddStream(MediaStream mediaStream) {}
            @Override
            public void onRemoveStream(MediaStream mediaStream) {}
            @Override
            public void onDataChannel(DataChannel dataChannel) {}
            @Override
            public void onRenegotiationNeeded() {}
            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                MediaStreamTrack track = rtpReceiver.track();
                if (track instanceof VideoTrack) {
                    VideoTrack remoteVideoTrack = (VideoTrack) track;
                    remoteVideoTrack.addSink(remoteVideoView);
                }
            }
        });

        if (peerConnection != null) {
            peerConnection.addTrack(localAudioTrack);
            if (localVideoTrack != null) {
                peerConnection.addTrack(localVideoTrack);
            }
        }
    }

    public void createOffer() {
        peerConnection.createOffer(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override public void onSetSuccess() {}
                    @Override public void onSetFailure(String s) {}
                    @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                    @Override public void onCreateFailure(String s) {}
                }, sdp);
                signalingListener.onSendSdp(sdp);
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) { Log.e(TAG, "createOffer failed: " + s); }
            @Override public void onSetFailure(String s) {}
        }, new MediaConstraints());
    }

    public void onRemoteSdpReceived(SessionDescription sdp) {
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override public void onSetSuccess() {
                if (sdp.type == SessionDescription.Type.OFFER) {
                    peerConnection.createAnswer(new SdpObserver() {
                        @Override public void onCreateSuccess(SessionDescription answer) {
                            peerConnection.setLocalDescription(new SdpObserver() {
                                @Override public void onSetSuccess() {}
                                @Override public void onSetFailure(String s) {}
                                @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                                @Override public void onCreateFailure(String s) {}
                            }, answer);
                            signalingListener.onSendSdp(answer);
                        }
                        @Override public void onCreateFailure(String s) { Log.e(TAG, "createAnswer failed: " + s); }
                        @Override public void onSetSuccess() {}
                        @Override public void onSetFailure(String s) {}
                    }, new MediaConstraints());
                }
            }
            @Override public void onSetFailure(String s) { Log.e(TAG, "setRemoteDescription failed: " + s); }
            @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
            @Override public void onCreateFailure(String s) {}
        }, sdp);
    }

    public void onRemoteIceCandidateReceived(IceCandidate candidate) {
        peerConnection.addIceCandidate(candidate);
    }

    public void endCall() {
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(TAG, "stopCapture interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        if (localVideoTrack != null) {
            localVideoTrack.dispose();
            localVideoTrack = null;
        }
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
    }
}