package com.example.uitnotify.ui.theme

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.uitnotify.AppDatabase
import com.example.uitnotify.Article
import com.example.uitnotify.ArticleDao
import com.example.uitnotify.ArticleWorker
import com.example.uitnotify.openUrlInBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.work.Data

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val articleDao = AppDatabase.getDatabase(context).articleDao()
    val coroutineScope = rememberCoroutineScope()
    val workManager = WorkManager.getInstance(context)

    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var workId: UUID? by remember { mutableStateOf(null) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        isLoading = true
        hasError = false
        isInstalling = false
        launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "Collecting articles from database")
                articleDao.getAllArticles().collect {
                    Log.d("MainActivity", "Articles updated: ${it.size}")
                    articles = it
                    if (it.isEmpty()) {
                        isInstalling = true
                        val inputData = Data.Builder().putBoolean("oneTime", true).build()
                        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(ArticleWorker::class.java).setInputData(inputData).build()
                        workId = oneTimeWorkRequest.id
                        workManager.enqueue(oneTimeWorkRequest)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error collecting articles", e)
                hasError = true
            }
        }
        isLoading = false
    }

    LaunchedEffect(workId) {
        if (workId != null) {
            workManager.getWorkInfoByIdLiveData(workId!!).observeForever { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            Log.d("MainActivity", "Work succeeded")
                            isInstalling = false
                        }
                        WorkInfo.State.FAILED -> {
                            Log.e("MainActivity", "Work failed")
                            isInstalling = false
                        }
                        WorkInfo.State.CANCELLED -> {
                            Log.e("MainActivity", "Work cancelled")
                            isInstalling = false
                        }
                        WorkInfo.State.RUNNING -> {
                            Log.d("MainActivity", "Work running")
                            progress = workInfo.progress.getFloat("progress", 0f)
                        }
                        else -> {
                            Log.d("MainActivity", "Work state: ${workInfo.state}")
                        }
                    }
                }
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            BannerComposable(
                coroutineScope = coroutineScope,
                articleDao = articleDao,
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        articleDao.deleteAllArticles()
                        Log.d("MainActivity", "Articles deleted")
                    }
                }
            )
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }  else if (hasError) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "An error occurred.")
                }
            } else if (articles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isInstalling) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            DynamicDeterminateProgressBar(
                                modifier = Modifier.padding(16.dp),
                                progress = progress
                            )
                            Text(text = "Fetching articles")
                        }
                    } else {
                        Text(text = "No articles found")
                    }
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

@Composable
fun DynamicDeterminateProgressBar(modifier: Modifier, progress: Float) {

    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = Color.Green,
    )
}

// Banner composable function
@Composable
fun BannerComposable(coroutineScope: CoroutineScope, articleDao: ArticleDao, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor()),
    ) {
        Text(
            text = "UIT Notify",
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterStart),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default,
            letterSpacing = 0.1.sp
        )
        Button(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
        )   {
            Text("Refresh")
        }
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
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
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
        Color(0xFFFFFFFF)
    }
}