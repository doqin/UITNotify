package com.example.uitnotify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.uitnotify.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

class ArticleForegroundService : Service() {
    private lateinit var appContext: Context
    private lateinit var sharedPreferences: SharedPreferences
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var interval: Long = 15
    private lateinit var articleDao: ArticleDao

    companion object {
        const val CHANNEL_ID = "ArticleServiceChannel"
        const val NOTIFICATION_ID = 1
        const val STOP_SERVICE_ACTION = "com.example.uitnotify.STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        createNotificationChannel()
        articleDao = AppDatabase.getDatabase(applicationContext).articleDao()
        sharedPreferences = appContext
            .getSharedPreferences("AppPrefs",
                Context.MODE_PRIVATE)
        loadIntervalFromPreferences()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            try {
                while (true) {
                    Log.d("ArticleForegroundService", "Service is running...")

                    try {
                        runBlocking {
                            Log.d("ArticleService", "runBlocking started")
                            coroutineScope {
                                Log.d("ArticleService", "coroutineScope started")
                                launch {
                                    Log.d("ArticleService", "launch started")
                                    val articles = downloadArticles()
                                    Log.d("ArticleService", "downloadArticle() completed, articles size: ${articles.size}")
                                    val lastArticleUrl = sharedPreferences.getString("lastArticleUrl", null)
                                    if (articles.isNotEmpty()) {
                                        val firstArticle = articles[0]
                                        if (!lastArticleUrl.equals(firstArticle.url)) {
                                            sendNotification(
                                                applicationContext,
                                                firstArticle.header,
                                                firstArticle.content,
                                                firstArticle.url
                                            )
                                            sharedPreferences.edit().putString("lastArticleUrl", firstArticle.url).apply()
                                        }
                                    }
                                    saveArticlesToDatabase(articles)
                                    Log.d("ArticleService", "saveArticlesToDatabase() completed")
                                    Log.d("ArticleService", "Result.success()")
                                }
                                Log.d("ArticleService", "launch completed")
                            }
                            Log.d("ArticleService", "coroutineScope completed")
                        }
                        Log.d("ArticleService", "runBlocking completed")
                    } catch (e: IOException) {
                        Log.e("ArticleService", "Error downloading articles", e)
                    } catch (e: Exception) {
                        Log.e("ArticleService", "Error processing articles", e)
                    }
                    Log.d("ArticleService", "doWork() finished")
                    Log.d("ArticleService", "Before delay()")
                    delay(interval * 60 * 1000)
                    Log.d("ArticleService", "After delay()")
                }
            }   catch (e: Exception) {
                Log.e("ArticleService", "Error in onStartCommand", e)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d("ArticleForegroundService", "Service is destroyed")
        sharedPreferences.edit().putBoolean("serviceStarted", false).apply()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Article Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopServiceIntent = Intent(this,
            StopServiceReceiver::class.java).apply() {
                action = STOP_SERVICE_ACTION
        }

        val stopServicePendingIntent = PendingIntent
            .getBroadcast(
                this,
                0,
                stopServiceIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        val stopAction = NotificationCompat.Action
            .Builder(
            R.drawable.ic_launcher_foreground,
            "Stop",
            stopServicePendingIntent
        ).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Article Service")
            .setContentText("Fetching articles in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(stopAction)
            .build()
    }

    private fun loadIntervalFromPreferences() {
        val intervalString = sharedPreferences.getString("interval_preference", "15")
        interval = intervalString?.toLongOrNull() ?: 15
        Log.d("ArticleForeGroundService", "Interval loaded from preferences: $interval")
    }

    private suspend fun downloadArticles(): List<Article> = withContext(Dispatchers.IO) {
        Log.d("Schedule", "Scheduling periodic article download")
        Log.d("ArticleService", "downloadArticles() started")
        val url = "https://student.uit.edu.vn/thong-bao-chung"
        val articles = mutableListOf<Article>()
        try {
            val document: Document = Jsoup.connect(url).get()
            Log.d("ArticleService", "Jsoup connected to $url")
            val articleElements: List<Element> = document.select("article")
            val totalArticles = articleElements.size
            Log.d("ArticleService", "Found $totalArticles article elements")
            for (articleElement in articleElements) {
                val header = articleElement.select("h2").text()
                val articleAbout = articleElement.attr("about")
                val articleUrl = "https://student.uit.edu.vn$articleAbout"
                Log.d("ArticleService", "Jsoup connected to $articleUrl, \"$header\"")
                val articleDocument: Document = Jsoup.connect(articleUrl).get()
                val dateSpan = articleDocument.selectFirst("span[property='dc:date dc:created']")
                val date = dateSpan?.text() ?: ""
                val content = articleDocument.select("p")
                val paragraph = content.joinToString("\n") { it.text() }
                articles.add(Article(header = header, date = date, content = paragraph, url = articleUrl))
            }
        } catch (e: IOException) {
            Log.e("ArticleService", "IOException in downloadArticles()", e)
            throw e
        }
        Log.d("ArticleService", "downloadArticles() completed, articles size: ${articles.size}")
        articles
    }

    private suspend fun saveArticlesToDatabase(articles: List<Article>) = withContext(Dispatchers.IO) {
        Log.d("ArticleService", "saveArticlesToDatabase() started, articles size: ${articles.size}")
        try {
            Log.d("ArticleService", "ArticleDao obtained")
            articleDao.insertAllArticles(articles)
            Log.d("ArticleService", "Inserted ${articles.size} articles into database")
        } catch (e: Exception) {
            Log.e("ArticleService", "Error saving articles to database", e)
            throw e
        }
        Log.d("ArticleService", "saveArticlesToDatabase() completed")
    }
}