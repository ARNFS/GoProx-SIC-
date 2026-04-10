package com.example.goprox;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import im.zego.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig;
import im.zego.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment;

public class CallActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        addCallFragment();
    }

    public void addCallFragment() {
        long appID = 613010579L;
        String appSign = "8b8819cfbf4a32265f3449f69d6a83b7542d298e4b08d0c66ce2a235787e7faa";

        String specialistId = getIntent().getStringExtra("SPECIALIST_ID");
        String callID = specialistId;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (currentUserName == null) currentUserName = "Me";

        ZegoUIKitPrebuiltCallConfig config = ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall();

        ZegoUIKitPrebuiltCallFragment fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                appID, appSign, currentUserId, currentUserName, callID, config);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitNow();
    }
}