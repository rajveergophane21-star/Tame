package com.tame.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tame.app.data.model.TameData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tame")

/**
 * Single source of truth for all on-device state. Backed by DataStore, storing the
 * whole [TameData] as one JSON blob. Fully offline — nothing leaves the device.
 */
class TameRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val KEY = stringPreferencesKey("data")

    val data: Flow<TameData> = store.data.map { prefs ->
        prefs[KEY]?.let { runCatching { json.decodeFromString<TameData>(it) }.getOrNull() }
            ?: SampleData.seed()
    }

    /** Read the latest state synchronously (for services running off the main thread). */
    fun snapshot(): TameData = runBlocking { data.first() }

    /** Atomically transform and persist the whole state. */
    suspend fun update(transform: (TameData) -> TameData) {
        store.edit { prefs ->
            val current = prefs[KEY]?.let { runCatching { json.decodeFromString<TameData>(it) }.getOrNull() }
                ?: SampleData.seed()
            prefs[KEY] = json.encodeToString(transform(current))
        }
    }

    /** Blocking variant of [update] for non-coroutine callers (services/receivers). */
    fun updateBlocking(transform: (TameData) -> TameData) = runBlocking { update(transform) }

    companion object {
        fun today(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
