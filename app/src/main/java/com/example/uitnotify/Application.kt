package com.example.uitnotify

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLifecycleTracker(this)
        AppClosedEvent.appClosedEvent.observeForever {
            val serviceIntent = Intent(this,
                ArticleForegroundService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
}