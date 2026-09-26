package com.example.goprox;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class CallManager {

    private static volatile CallManager instance;

    private ListenerRegistration listener;
    private FirebaseAuth.AuthStateListener authStateListener;

    private volatile boolean isInCall = false;
    private volatile boolean isPaused = false;

    private Context appContext;
    private String listeningUid;

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

    public synchronized void startListening(Context context) {
        if (context == null) return;

        appContext = context.getApplicationContext();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null) return;

        // Եթե արդեն կա AuthStateListener, հինը հանում ենք,
        // որպեսզի duplicate listener չստեղծվի։
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
        }

        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user == null) {
                android.util.Log.d(
                        "CallManager",
                        "🔴 No authenticated user — stopping call listener"
                );

                stopListening();
                return;
            }

            String uid = user.getUid();

            if (!uid.equals(listeningUid)) {
                android.util.Log.d(
                        "CallManager",
                        "🟢 Auth ready — starting call listener for uid=" + uid
                );

                startCallDocumentListener(uid);
            }
        };

        auth.addAuthStateListener(authStateListener);

        // Եթե Auth-ը արդեն պատրաստ է, անմիջապես listener-ը միացնում ենք։
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            startCallDocumentListener(currentUser.getUid());
        }
    }

    private synchronized void startCallDocumentListener(String uid) {
        if (uid == null || uid.isEmpty() || appContext == null) {
            return;
        }

        stopListening();

        listeningUid = uid;

        android.util.Log.d(
                "CallManager",
                "📞 Listening to /calls/" + uid
        );

        listener = FirebaseFirestore.getInstance()
                .collection("calls")
                .document(uid)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        android.util.Log.e(
                                "CallManager",
                                "❌ Call listener error: " + e.getMessage(),
                                e
                        );
                        return;
                    }

                    if (snap == null || !snap.exists()) {
                        return;
                    }

                    if (isPaused) {
                        android.util.Log.d(
                                "CallManager",
                                "⏸ Call listener paused"
                        );
                        return;
                    }

                    String status = snap.getString("status");
                    String callerIdFromDb = snap.getString("callerId");

                    android.util.Log.d(
                            "CallManager",
                            "📞 Call update: uid=" + uid
                                    + ", status=" + status
                                    + ", callerId=" + callerIdFromDb
                    );

                    if ("ringing".equals(status)
                            && !isInCall
                            && !uid.equals(callerIdFromDb)) {

                        handleIncomingCall(appContext, snap);
                    }
                });
    }

    private void handleIncomingCall(
            Context ctx,
            DocumentSnapshot snap
    ) {
        if (ctx == null || snap == null) return;

        try {
            String callId = snap.getString("callId");
            String callerId = snap.getString("callerId");
            String callerName = snap.getString("callerName");
            String callerPhotoUrl = snap.getString("callerPhotoUrl");
            String channelName = snap.getString("channelName");
            String serviceTitle = snap.getString("serviceTitle");

            Intent intent =
                    new Intent(ctx, IncomingCallActivity.class);

            intent.putExtra("callId", callId);
            intent.putExtra("callerId", callerId);
            intent.putExtra(
                    "callerName",
                    callerName != null ? callerName : "Unknown"
            );
            intent.putExtra("callerPhotoUrl", callerPhotoUrl);
            intent.putExtra("channelName", channelName);
            intent.putExtra("serviceTitle", serviceTitle);

            int flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER;
            }

            intent.setFlags(flags);

            android.util.Log.d(
                    "CallManager",
                    "📲 Opening IncomingCallActivity"
            );

            ctx.startActivity(intent);

        } catch (Exception e) {

            android.util.Log.e(
                    "CallManager",
                    "❌ Failed to open IncomingCallActivity",
                    e
            );
        }
    }

    public void pauseListening() {
        isPaused = true;

        android.util.Log.d(
                "CallManager",
                "⏸ Call listening paused"
        );
    }

    public void resumeListening() {
        isPaused = false;

        android.util.Log.d(
                "CallManager",
                "▶️ Call listening resumed"
        );
    }

    public void setInCall(boolean inCall) {
        this.isInCall = inCall;
    }

    public boolean isInCall() {
        return isInCall;
    }

    public synchronized void stopListening() {

        if (listener != null) {
            try {
                listener.remove();
            } catch (Exception e) {
                android.util.Log.e(
                        "CallManager",
                        "❌ Failed to remove call listener",
                        e
                );
            }

            listener = null;
        }

        listeningUid = null;
    }
}