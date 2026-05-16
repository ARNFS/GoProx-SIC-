package com.example.goprox;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class CallManager {

    private static volatile CallManager instance;
    private ListenerRegistration listener;
    private volatile boolean isInCall = false;
    private volatile boolean isPaused = false;

    private CallManager() {}

    public static CallManager getInstance() {
        if (instance == null) {
            synchronized (CallManager.class) {
                if (instance == null) {
                    instance = new CallManager();
                }
            }
        }
        return instance;
    }

    public void startListening(Context appContext) {
        if (appContext == null) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null) return;

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        stopListening();

        try {
            listener = FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(uid)
                    .addSnapshotListener((snap, e) -> {
                        if (e != null || snap == null || !snap.exists()) return;
                        if (isPaused) return; // 🔥 PAUSED — ոչինչ չենք անում

                        String status = snap.getString("status");
                        String callerIdFromDb = snap.getString("callerId");

                        if ("ringing".equals(status) && !isInCall && !uid.equals(callerIdFromDb)) {
                            handleIncomingCall(appContext, snap);
                        }
                    });
        } catch (Exception ignored) {}
    }

    private void handleIncomingCall(Context ctx, DocumentSnapshot snap) {
        if (ctx == null || snap == null) return;

        try {
            Intent intent = new Intent(ctx, IncomingCallActivity.class);
            intent.putExtra("callId", snap.getString("callId"));
            intent.putExtra("callerId", snap.getString("callerId"));
            intent.putExtra("callerName", snap.getString("callerName") != null
                    ? snap.getString("callerName") : "Unknown");
            intent.putExtra("callerPhotoUrl", snap.getString("callerPhotoUrl"));
            intent.putExtra("channelName", snap.getString("channelName"));
            intent.putExtra("serviceTitle", snap.getString("serviceTitle"));

            int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER;
            }
            intent.setFlags(flags);

            ctx.startActivity(intent);
        } catch (Exception ignored) {}
    }

    // 🔥 ՆՈՐ ՄԵԹՈԴՆԵՐ
    public void pauseListening() {
        isPaused = true;
    }

    public void resumeListening() {
        isPaused = false;
    }

    public void setInCall(boolean inCall) {
        this.isInCall = inCall;
    }

    public boolean isInCall() {
        return isInCall;
    }

    public void stopListening() {
        if (listener != null) {
            try {
                listener.remove();
            } catch (Exception ignored) {}
            listener = null;
        }
    }
}