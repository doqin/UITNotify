package com.example.uitnotify

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

class AppLifecycleTracker(private val application: Application) :
    Application.ActivityLifecycleCallbacks {
    private var activityCount = 0
    private var isAppInForeground = false
    private val sharedPreferences = application.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d("AppLifecycleTracker", "onActivityCreated:")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d("AppLifecycleTracker", "onActivityStarted")
        activityCount++
        if (isAppInForeground) {
            isAppInForeground = false
            Log.d("AppLifecycleTracker", "App is back in foreground")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d("AppLifecycleTracker", "onActivityResumed")
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d("AppLifecycleTracker", "onActivityPaused")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d("AppLifecycleTracker", "onActivityStopped")
        activityCount--
        if (activityCount == 0) {
            isAppInForeground = true
            Log.d("AppLifecycleTracker", "App is in the background")
            if (!sharedPreferences.getBoolean("isAppInForeground", false)) {
                Log.d("AppLifecycleTracker", "Starting ArticleForegroundService")
                AppClosedEvent.appClosedEvent.setValue(true)
                sharedPreferences.edit().putBoolean("serviceStarted", true).apply()
                val serviceIntent = Intent(application, ArticleForegroundService::class.java)
                application.startService(serviceIntent)
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Log.d("AppLifecycleTracker", "onActivitySaveInstanceState")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d("AppLifecycleTracker", "onActivityDestroyed")
    }
}

class AppClosedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("AppClosedReceiver", "onReceive")
    }
}