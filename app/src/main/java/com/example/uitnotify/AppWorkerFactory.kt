package com.example.uitnotify

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class AppWorkerFactory (private val settingsRepository: SettingsRepository)
    : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ) : ListenableWorker? {
            return when (workerClassName) {
                ArticleWorker::class.java.name ->
                    ArticleWorker(appContext,
                        workerParameters,
                        settingsRepository)
                else -> null
            }
        }
}