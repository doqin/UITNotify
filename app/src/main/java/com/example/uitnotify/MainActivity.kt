package com.example.uitnotify

import android.annotation.SuppressLint
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.uitnotify.ui.theme.UITNotifyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

// Data class to hold article information
data class ArticleData(
    val header: String? = null,
    val date: String? = null,
    val content: String? = null,
    val url: String? = null,
    val error: String? = null
)

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
        schedulePeriodicArticleDownload(this)
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

@Composable
fun MainScreen() {
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val articleDao = AppDatabase.getDatabase(context).articleDao()
        articles = articleDao.getAllArticles()
        withContext(Dispatchers.IO) {
            try {
                articles = articleDao.getAllArticles()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching articles", e)
            }
        }
        isLoading = false
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            BannerComposable()
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (articles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No articles found")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(articles) { article ->
                        ArticleItem(article = article)
                    }
                }
            }
        }
    }
}

// Banner composable function
@Composable
fun BannerComposable(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor()),
    ) {
        Text(
            text = "UIT Notify",
            modifier = Modifier.padding(16.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default,
            letterSpacing = 0.1.sp
        )
    }
}

// Article composable function
@Composable
fun ArticleItem(article: Article) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberRipple(bounded = true)
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = {
                    openUrlInBrowser(context, article.url)
                },
                interactionSource = interactionSource,
                indication = indication,
            ),
        shape = RoundedCornerShape(8.dp),
        color = surfaceColor()
    )   {
        Column(
            modifier = Modifier
                .padding(
                    vertical = 16.dp,
                    horizontal = 16.dp
                )
        ) {
            Text(
                text = article.header,
                color = Color(0xFFB94A48),
                fontSize = 19.sp
            )
            Text(
                text = article.date,
                modifier = Modifier.padding(vertical = 6.dp),
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            Text(
                text = article.content,
                fontSize = 14.sp
            )
        }
    }
}

// Function to determine the surface color based on the current theme
@Composable
fun surfaceColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color(0x0DFFFFFF)
    }
    else {
        Color(0x0D000000)
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

fun schedulePeriodicArticleDownload(context: Context) {
    val periodicWorkRequest = PeriodicWorkRequest.Builder(ArticleWorker::class.java, 15, TimeUnit.MINUTES).build()

    WorkManager.getInstance(context).enqueue(periodicWorkRequest)
}