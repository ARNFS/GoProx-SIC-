package com.example.goprox;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New token: " + token);
        updateTokenInFirestore(token);
    }

    private void updateTokenInFirestore(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e(TAG, "User not logged in, cannot save token");
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "Saving token for user: " + uid + ", token: " + token);

        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        data.put("updatedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save token", e));
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Message received");

        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");
            if ("call".equals(type)) {
                Intent intent = new Intent(this, CallActivity.class);
                intent.putExtra("SPECIALIST_ID", remoteMessage.getData().get("callerId"));
                intent.putExtra("SPECIALIST_NAME", remoteMessage.getData().get("callerName"));
                intent.putExtra("IS_AUDIO_ONLY", Boolean.parseBoolean(remoteMessage.getData().get("isAudioOnly")));
                intent.putExtra("IS_CALLER", false);
                intent.putExtra("currentUserId", FirebaseAuth.getInstance().getCurrentUser().getUid());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }
}