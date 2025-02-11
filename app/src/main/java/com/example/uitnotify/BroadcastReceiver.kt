package com.example.uitnotify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StopServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("StopServiceReceiver",
            "StopServiceReceiver received")
        val serviceIntent = Intent(context, ArticleForegroundService::class.java)
        context?.stopService(serviceIntent)
    }

}