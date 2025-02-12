package com.example.uitnotify.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.uitnotify.R
import com.example.uitnotify.activities.MainActivity

fun createNotificationChannel(context: Context) {
    val name = "Article Channel"
    val descriptionText = "Channel for articles"
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel("article_channel", name, importance)
        .apply {
            description = descriptionText
        }

    val notificationManager: NotificationManager = context
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    notificationManager.createNotificationChannel(channel)
}

fun sendNotification(context: Context, title: String, message: String, url: String) {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    val pendingIntent: PendingIntent = PendingIntent
        .getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val fullScreenIntent = Intent(context, MainActivity::class.java)
        .apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        }

    val fullScreenPendingIntent = PendingIntent.getActivity(
        context,
        0,
        fullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Builds the notification
    val builder = NotificationCompat.Builder(context, "article_channel")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .setAutoCancel(true)

    // Shows the notification
    with (NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        notify(0, builder.build())
    }
}