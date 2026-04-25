package com.example.goprox;

import android.app.Application;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Agora не требует глобальной инициализации здесь.
        // Приложение готово к работе.
    }
}