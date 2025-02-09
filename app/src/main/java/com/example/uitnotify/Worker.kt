package com.example.uitnotify

import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

class ArticleWorker(appContext: Context,
                    workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        var result: Result = Result.failure()
        try {
            kotlinx.coroutines.runBlocking {
                coroutineScope {
                    launch {
                        val articles = downloadArticles()
                        saveArticlesToDatabase(articles)
                        result = Result.success()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("ArticleWorker", "Error downloading articles", e)
            result = Result.retry()
        } catch (e: Exception) {
            Log.e("ArticleWorker", "Error processing articles", e)
            result = Result.failure()
        }
        return result
    }

    private suspend fun downloadArticles(): List<Article> = withContext(Dispatchers.IO) {
        val url = "https://www.uit.edu.vn/thong-bao-chung" // Replace with the actual URL
        val articles = mutableListOf<Article>()
        try {
            val document: Document = Jsoup.connect(url).get()
            val articleElements: List<Element> = document.select("article")
            for (articleElement in articleElements) {
                val header = articleElement.select("h2").text()
                val date = articleElement.select("time").text()
                val content = articleElement.select("p").text()
                val url = articleElement.select("a").attr("href")
                articles.add(Article(header = header, date = date, content = content, url = url))
            }
        } catch (e: IOException) {
            throw e
        }
        articles
    }

    private suspend fun saveArticlesToDatabase(articles: List<Article>) {
        val articleDao = AppDatabase.getDatabase(applicationContext).articleDao()
        articleDao.insertAllArticles(articles)
    }
}

