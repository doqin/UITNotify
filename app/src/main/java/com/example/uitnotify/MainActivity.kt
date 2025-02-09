package com.example.uitnotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.uitnotify.ui.theme.UITNotifyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

// Data class to hold article information
data class ArticleData(
    val header: String? = null,
    val date: String? = null,
    val content: String? = null,
    val error: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UITNotifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ArticleComposable(url = "https://student.uit.edu.vn/thong-bao-chung", index = 0, modifier = Modifier.padding(innerPadding))
                }
            }
        }
        runBlocking {
            launch {

            }
        }
    }
}

@Composable
fun ArticleComposable(url: String, index: Int, modifier: Modifier = Modifier) {
    var articleData by remember { mutableStateOf(ArticleData()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(url, index) {
        isLoading = true
        val result = getArticleText(url, index)
        articleData = result
        isLoading = false
    }

    Column(modifier = Modifier.padding(24.dp)) {
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
            Text(
                text = "Article ${index + 1} header: ${articleData.header}",
                modifier = modifier,
                )
            Text(
                text = "Date: ${articleData.date}",
                modifier = modifier,
                )
            Text(
                text = "Content: ${articleData.content}",
                modifier = modifier,
                )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArticleComposablePreview() {
    UITNotifyTheme {
        ArticleComposable(url = "https://student.uit.edu.vn/thong-bao-chung", index = 0)
    }
}

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
            content = paragraph
        )
    } catch (e: Exception) {
        ArticleData(error = e.message)
    }
}