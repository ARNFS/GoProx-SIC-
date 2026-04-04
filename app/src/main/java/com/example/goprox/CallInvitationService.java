package com.example.goprox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CallInvitationService extends Service {

    private DatabaseReference callsRef;
    private ValueEventListener listener;

    @Override
    public void onCreate() {
        super.onCreate();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId != null) {
            callsRef = FirebaseDatabase.getInstance().getReference("calls").child(userId);
            listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String channelName = snapshot.child("channelName").getValue(String.class);
                        // Open CallActivity
                        Intent intent = new Intent(CallInvitationService.this, CallActivity.class);
                        intent.putExtra("channelName", channelName);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        snapshot.getRef().removeValue();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            callsRef.addValueEventListener(listener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (callsRef != null && listener != null) {
            callsRef.removeEventListener(listener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}