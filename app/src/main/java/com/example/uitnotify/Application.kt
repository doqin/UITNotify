package com.example.uitnotify

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import com.example.uitnotify.data.SettingsRepository
import com.example.uitnotify.data.dataStore
import com.example.uitnotify.monitors.AppClosedEvent
import com.example.uitnotify.monitors.AppLifecycleTracker
import com.example.uitnotify.service.ArticleForegroundService
import com.example.uitnotify.workers.AppWorkerFactory

class App : Application(), Configuration.Provider {
    private lateinit var settingsRepository: SettingsRepository
    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(dataStore)
        AppLifecycleTracker(this)
        AppClosedEvent.appClosedEvent.observeForever {
            val serviceIntent = Intent(this,
                ArticleForegroundService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(AppWorkerFactory(settingsRepository))
            .build()
}