package com.tame.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tame.app.data.model.Habit
import com.tame.app.data.model.TameData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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

    /**
     * Reset today's reel count/time and advance habit history when the calendar day
     * changes — safe to call from anywhere (app or service); no-ops if already today.
     */
    suspend fun rolloverIfNeeded() = update { d ->
        val today = today()
        val last = d.settings.lastReelDay
        if (last == today) return@update d
        val elapsed = daysBetween(last, today)
        val under = last.isNotEmpty() && d.settings.todayReels <= d.settings.reelLimit
        val daysUnder = (if (under) d.settings.daysUnderLimit + 1 else d.settings.daysUnderLimit).coerceIn(0, 7)
        val habits = if (elapsed > 0) d.habits.map { rollGrid(it, elapsed) } else d.habits
        d.copy(
            settings = d.settings.copy(todayReels = 0, reelSeconds = 0, lastReelDay = today, daysUnderLimit = daysUnder),
            habits = habits,
        )
    }

    private fun daysBetween(last: String, today: String): Int {
        if (last.isEmpty()) return 0
        return try {
            ChronoUnit.DAYS.between(LocalDate.parse(last), LocalDate.parse(today)).toInt().coerceIn(0, 28)
        } catch (e: Exception) { 1 }
    }

    private fun rollGrid(h: Habit, shift: Int): Habit {
        if (h.grid.isEmpty()) return h.copy(grid = List(28) { 0 })
        val n = h.grid.size
        val s = shift.coerceIn(0, n)
        if (s == 0) return h
        return h.copy(grid = (h.grid.drop(s) + List(s) { 0 }).takeLast(n))
    }

    companion object {
        fun today(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
