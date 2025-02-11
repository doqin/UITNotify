package com.example.uitnotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.uitnotify.options.IntervalOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private companion object {
        val INTERVAL_KEY = longPreferencesKey("interval")
    }

    suspend fun saveInterval(interval: Long) {
        dataStore.edit { preferences ->
            preferences[INTERVAL_KEY] = interval
        }
    }

    fun getInterval(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[INTERVAL_KEY] ?: IntervalOption.FIFTEEN_MINUTES.minutes
    }
}