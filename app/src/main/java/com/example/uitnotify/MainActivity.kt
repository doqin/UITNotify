package com.example.uitnotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.startActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.uitnotify.ui.theme.MainScreen
import com.example.uitnotify.ui.theme.UITNotifyTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        createNotificationChannel(this)

        setContent {
            UITNotifyTheme {
                MainScreen()
            }
        }
        requestNotificationPermission()
        schedulePeriodicArticleDownload()
    }

    private fun schedulePeriodicArticleDownload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequest.Builder(ArticleWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueue(periodicWorkRequest)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            sendNotification(this, "Permission Granted", "Message")
        } else {
            Log.d("MainActivity", "Permission denied")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

// Function to open a URL in the default browser
fun openUrlInBrowser(context: Context, url: String) {
    Log.d("OpenUrl", "Attempting to open URL: $url")
    val webpage: Uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, webpage)
    if (intent.resolveActivity(context.packageManager) != null) {
        Log.d("OpenUrl", "Starting activity for URL: $url")
        startActivity(context, intent, null)
    } else {
        Log.e("OpenUrl", "No activity found to handle URL: $url")

    }
}