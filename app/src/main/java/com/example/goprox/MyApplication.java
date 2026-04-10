package com.example.goprox;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;

import im.zego.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService;
import im.zego.uikit.prebuilt.call.invitation.ZegoUIKitPrebuiltCallInvitationConfig;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Твой AppID и AppSign из ZegoCloud
        long appID = 613010579L;
        String appSign = "8b8819cfbf4a32265f3449f69d6a83b7542d298e4b08d0c66ce2a235787e7faa";

        // Данные текущего пользователя (если уже залогинен)
        String currentUserId = "default_user";
        String currentUserName = "Me";

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (currentUserName == null) currentUserName = "Me";
        }

        ZegoUIKitPrebuiltCallInvitationConfig config = new ZegoUIKitPrebuiltCallInvitationConfig();
        ZegoUIKitPrebuiltCallService.init(getApplication(), appID, appSign, currentUserId, currentUserName, config);
    }
}