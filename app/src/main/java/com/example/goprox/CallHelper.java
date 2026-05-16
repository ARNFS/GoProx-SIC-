package com.example.goprox;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CallHelper {

    public static void startCall(Activity activity, String otherUserId,
                                 String otherUserName, String serviceTitle) {
        if (activity == null || otherUserId == null || otherUserId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();

        if (otherUserId.equals(currentUserId)) {
            Toast.makeText(activity, "You cannot call yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        if (CallManager.getInstance().isInCall()) {
            Toast.makeText(activity, "Already in a call", Toast.LENGTH_SHORT).show();
            return;
        }

        CallManager.getInstance().pauseListening();
        CallManager.getInstance().setInCall(true);

        String channelName = (currentUserId.compareTo(otherUserId) < 0)
                ? currentUserId + "_" + otherUserId
                : otherUserId + "_" + currentUserId;

        String myName = auth.getCurrentUser().getDisplayName();
        if (myName == null) myName = "User";

        String finalServiceTitle = (serviceTitle != null && !serviceTitle.isEmpty()) ? serviceTitle : "";

        Map<String, Object> callData = new HashMap<>();
        callData.put("callId", channelName);
        callData.put("callerId", currentUserId);
        callData.put("callerName", myName);
        callData.put("callerPhotoUrl", "");
        callData.put("channelName", channelName);
        callData.put("serviceTitle", finalServiceTitle);
        callData.put("status", "ringing");

        Map<String, Object> selfData = new HashMap<>();
        selfData.put("callId", channelName);
        selfData.put("otherUserId", otherUserId);
        selfData.put("channelName", channelName);
        selfData.put("status", "calling"); // 🔥 "calling", ՈՉ "ringing"

        db.collection("calls").document(otherUserId).set(callData);
        db.collection("calls").document(currentUserId)
                .set(selfData)
                .addOnSuccessListener(v -> {
                    Intent intent = new Intent(activity, OutgoingCallActivity.class);
                    intent.putExtra("channelName", channelName);
                    intent.putExtra("currentUid", currentUserId);
                    intent.putExtra("otherUserId", otherUserId);
                    intent.putExtra("remoteUserName", otherUserName != null ? otherUserName : "User");
                    intent.putExtra("serviceTitle", finalServiceTitle);
                    activity.startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    CallManager.getInstance().setInCall(false);
                    CallManager.getInstance().resumeListening();
                    Toast.makeText(activity, "Failed to start call", Toast.LENGTH_SHORT).show();
                });
    }
}