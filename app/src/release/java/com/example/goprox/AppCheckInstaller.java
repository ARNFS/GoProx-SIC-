package com.example.goprox;

import android.content.Context;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

final class AppCheckInstaller {
    private AppCheckInstaller() {}

    static void install(Context context) {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        );
    }
}
