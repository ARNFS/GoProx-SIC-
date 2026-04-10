package com.example.goprox;

import android.app.Application;
import android.util.Log;

import im.zego.uikit.ZegoUIKit;
import im.zego.uikit.call.ZegoUIKitCallInitConfig;

public class ZegoCloudManager {

    private static final String TAG = "ZegoCloudManager";
    private static boolean isInitialized = false;

    /**
     * Инициализирует ZegoUIKit. Вызови этот метод в Application.onCreate().
     *
     * @param app   экземпляр Application
     * @param appID твой AppID из ZegoCloud Console
     */
    public static void init(Application app, long appID) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized");
            return;
        }

        ZegoUIKitCallInitConfig config = new ZegoUIKitCallInitConfig();
        // Устанавливаем логотип и название приложения (опционально)
        config.setAppIcon(R.drawable.ic_launcher);
        config.setAppName(app.getString(R.string.app_name));

        ZegoUIKit.init(app, appID, config);
        isInitialized = true;
        Log.d(TAG, "ZegoUIKit initialized");
    }
}