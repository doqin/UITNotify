package com.example.uitnotify.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.uitnotify.SettingsScreen
import com.example.uitnotify.ui.theme.UITNotifyTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UITNotifyTheme {
                SettingsScreen()
            }
        }
    }
}