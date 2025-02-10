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
import com.example.uitnotify.ui.theme.UITNotifyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

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
        requestNotificationPermission()
        setContent {
            UITNotifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column {
                        BannerComposable(modifier = Modifier.padding(innerPadding))
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            ArticleComposable(
                                url = "https://student.uit.edu.vn/thong-bao-chung",
                                index = 0,
                                modifier = Modifier.padding(innerPadding)
                            )
                            for (i in 1 until 5) {
                                ArticleComposable(
                                    url = "https://student.uit.edu.vn/thong-bao-chung",
                                    index = i,
                                )
                            }
                        }
                    }
                }
            }
        }
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

// Banner composable function
@Composable
fun BannerComposable(modifier: Modifier = Modifier) {
    var articleData by remember { mutableStateOf(ArticleData()) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val result = getArticleText("https://student.uit.edu.vn/thong-bao-chung", 0)
        articleData = result
    }

    Box(
        modifier = Modifier.fillMaxWidth().background(surfaceColor()),
    ) {
        Text(
            text = "UIT Notify",
            modifier = Modifier.padding(16.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default,
            letterSpacing = 0.1.sp
        )
        Button(
            onClick = {
                sendNotification(context, "${articleData.header}", "${articleData.date}\n${articleData.content}")
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(text = "Test")
        }
    }
}

// Article composable function
@Composable
fun ArticleComposable(url: String, index: Int, modifier: Modifier = Modifier) {
    var articleData by remember { mutableStateOf(ArticleData()) }
    var isLoading by remember { mutableStateOf(true) }
    var clickCount by remember { mutableStateOf(0) }

    val interactionSource = remember { MutableInteractionSource() }
    val indication = rememberRipple(bounded = true)
    val context = LocalContext.current

    LaunchedEffect(url, index) {
        isLoading = true
        val result = getArticleText(url, index)
        articleData = result
        isLoading = false
    }

    Box {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (articleData.error != null) {
            Text(
                text = "Error: ${articleData.error}",
                modifier = modifier,
            )
        } else {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        onClick = {
                            clickCount++
                            Log.d("ArticleComposable", "URL to open: ${articleData.url}")
                            articleData.url?.let { openUrlInBrowser(context, it) }
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
                    // Header text
                    Text(
                        text = "${articleData.header}",
                        color = Color(0xFFB94A48),
                        fontSize = 19.sp
                    )
                    // Date text
                    Text(
                        text = "${articleData.date}",
                        modifier = Modifier.padding(vertical = 6.dp),
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    // Content text
                    Text(
                        text = "${articleData.content}",
                        fontSize = 14.sp
                    )
                }
            }
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

// Preview function for the ArticleComposable
@Preview(showBackground = true)
@Composable
fun ArticleComposablePreview() {
    UITNotifyTheme {
        ArticleComposable(url = "https://student.uit.edu.vn/thong-bao-chung", index = 0)
    }
}

// Function to fetch article text from a URL
suspend fun getArticleText(url: String, index: Int): ArticleData = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(url).get()
        val articles = doc.select("article")
        var article: Element? = null
        if (index < articles.size) {
            article = articles[index]
        }
        val subheader = article?.selectFirst("h2")
        val articleAbout = article?.attr("about")
        val articleUrl = "https://student.uit.edu.vn$articleAbout"
        val articleDoc = Jsoup.connect(articleUrl).get()
        val contentBody = articleDoc.getElementsByClass("content-body")
        val dateSpan = articleDoc.selectFirst("span[property='dc:date dc:created']")
        val dateContent = dateSpan?.text()
        val content = contentBody.select("p")
        val paragraph = content.joinToString("\n") { it.text() }

        // Return the article data
        ArticleData(
            header = subheader?.text(),
            date = dateContent,
            url = articleUrl,
            content = paragraph
        )
    } catch (e: Exception) {
        ArticleData(error = e.message)
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