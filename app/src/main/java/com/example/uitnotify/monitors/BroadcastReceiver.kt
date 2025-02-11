package com.example.uitnotify.monitors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.uitnotify.service.ArticleForegroundService

class StopServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("StopServiceReceiver",
            "StopServiceReceiver received")
        val serviceIntent = Intent(context, ArticleForegroundService::class.java)
        context?.stopService(serviceIntent)
    }

}