package com.example.uitnotify.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.uitnotify.workers.ArticleWorker
import com.example.uitnotify.composables.MainScreen
import com.example.uitnotify.data.SettingsRepository
import com.example.uitnotify.notifications.createNotificationChannel
import com.example.uitnotify.data.dataStore
import com.example.uitnotify.ui.theme.UITNotifyTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(dataStore)
        // enableEdgeToEdge()
        createNotificationChannel(this)
        setContent {
            UITNotifyTheme {
                MainScreen()
            }
        }
        requestNotificationPermission()
        observeIntervalChanges()
    }

    private fun observeIntervalChanges() {
        lifecycleScope.launch {
            settingsRepository.getInterval().collectLatest { interval ->
                Log.d("MainActivity",
                    "Observed interval change: interval = $interval")
                schedulePeriodicArticleDownload(interval)
            }
        }
    }

    private fun schedulePeriodicArticleDownload(interval: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            ArticleWorker::class.java,
            interval,
            TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this@MainActivity)
            .enqueueUniquePeriodicWork(
                "ArticleUpdate",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Permission granted")
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