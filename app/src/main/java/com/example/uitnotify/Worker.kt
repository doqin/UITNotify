package com.example.uitnotify

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ArticleWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val articleDao = AppDatabase.getDatabase(applicationContext).articleDao()
    private val sharedPreferences: SharedPreferences = appContext
        .getSharedPreferences("ArticleWorkerPrefs", Context.MODE_PRIVATE)
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun doWork(): Result {
        Log.d("ArticleWorker", "doWork() started")
        val lastDownloadTime = getLastDownloadTime()
        val now = LocalDateTime.now()
        val timeSinceLastDownload = ChronoUnit.MINUTES.between(lastDownloadTime, now)
        if (timeSinceLastDownload < 1) {
            Log.d("ArticleWorker", "doWork: Not enough time has passed since last download. Skipping.")
            return Result.success()
        }
        var result: Result = Result.failure()
        try {
            runBlocking {
                Log.d("ArticleWorker", "runBlocking started")
                coroutineScope {
                    Log.d("ArticleWorker", "coroutineScope started")
                    launch {
                        Log.d("ArticleWorker", "launch started")
                        // articleDao.deleteAllArticles()
                        // Log.d("ArticleWorker", "deleteAllArticles() completed")
                        val articles = downloadArticles()
                        Log.d("ArticleWorker", "downloadArticle() completed, articles size: ${articles.size}")
                        sendNotification(applicationContext, "UIT Notify", "Downloaded ${articles.size} articles")
                        saveArticlesToDatabase(articles)
                        Log.d("ArticleWorker", "saveArticlesToDatabase() completed")
                        saveLastDownloadTime(now)
                        result = Result.success()
                        Log.d("ArticleWorker", "Result.success()")
                    }
                    Log.d("ArticleWorker", "launch completed")
                }
                Log.d("ArticleWorker", "coroutineScope completed")
            }
            Log.d("ArticleWorker", "runBlocking completed")
        } catch (e: IOException) {
            Log.e("ArticleWorker", "Error downloading articles", e)
            result = Result.retry()
        } catch (e: Exception) {
            Log.e("ArticleWorker", "Error processing articles", e)
            result = Result.failure()
        }
        Log.d("ArticleWorker", "doWork() finished, result: $result")
        return result
    }

    private suspend fun downloadArticles(): List<Article> = withContext(Dispatchers.IO) {
        Log.d("Schedule", "Scheduling periodic article download")
        Log.d("ArticleWorker", "downloadArticles() started")
        val url = "https://student.uit.edu.vn/thong-bao-chung"
        val articles = mutableListOf<Article>()
        try {
            val document: Document = Jsoup.connect(url).get()
            Log.d("ArticleWorker", "Jsoup connected to $url")
            val articleElements: List<Element> = document.select("article")
            Log.d("ArticleWorker", "Found ${articleElements.size} article elements")
            for (articleElement in articleElements) {
                val header = articleElement.select("h2").text()
                val articleAbout = articleElement.attr("about")
                val articleUrl = "https://student.uit.edu.vn$articleAbout"
                Log.d("ArticleWorker", "Jsoup connected to $articleUrl, \"$header\"")
                val articleDocument: Document = Jsoup.connect(articleUrl).get()
                val dateSpan = articleDocument.selectFirst("span[property='dc:date dc:created']")
                val date = dateSpan?.text() ?: ""
                val content = articleDocument.select("p")
                val paragraph = content.joinToString("\n") { it.text() }
                articles.add(Article(header = header, date = date, content = paragraph, url = articleUrl))
            }
        } catch (e: IOException) {
            Log.e("ArticleWorker", "IOException in downloadArticles()", e)
            throw e
        }
        Log.d("ArticleWorker", "downloadArticles() completed, articles size: ${articles.size}")
        articles
    }

    private suspend fun saveArticlesToDatabase(articles: List<Article>) {
        Log.d("ArticleWorker", "saveArticlesToDatabase() started, articles size: ${articles.size}")
        try {
            val articleDao = AppDatabase.getDatabase(applicationContext).articleDao()
            Log.d("ArticleWorker", "ArticleDao obtained")
            articleDao.insertAllArticles(articles)
            Log.d("ArticleWorker", "Inserted ${articles.size} articles into database")
        } catch (e: Exception) {
            Log.e("ArticleWorker", "Error saving articles to database", e)
            throw e
        }
        Log.d("ArticleWorker", "saveArticlesToDatabase() completed")
    }

    private fun getLastDownloadTime(): LocalDateTime {
        val lastDownloadTimeString = sharedPreferences.getString("lastDownloadTime", null)
        return if (lastDownloadTimeString != null) {
            LocalDateTime.parse(lastDownloadTimeString, dateTimeFormatter)
        } else {
            LocalDateTime.now().minusYears(1)
        }
    }

    private fun saveLastDownloadTime(time: LocalDateTime) {
        val timeString = time.format(dateTimeFormatter)
        sharedPreferences.edit().putString("lastDownloadTime", timeString).apply()
    }
}

