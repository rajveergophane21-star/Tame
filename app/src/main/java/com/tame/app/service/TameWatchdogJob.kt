package com.tame.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.tame.app.R
import com.tame.app.util.AccessibilityUtil

/**
 * Periodic backstop: if the OS or an aggressive battery optimiser turns Tame's
 * accessibility service off, this notices and nudges the user to switch it back on,
 * so blocking keeps working over time.
 */
class TameWatchdogJob : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        if (!AccessibilityUtil.isEnabled(this)) maybeNotify()
        return false // work is synchronous
    }

    override fun onStopJob(params: JobParameters?): Boolean = false

    private fun maybeNotify() {
        val prefs = getSharedPreferences("tame_watchdog", Context.MODE_PRIVATE)
        val last = prefs.getLong("last_notify", 0L)
        val now = System.currentTimeMillis()
        if (now - last < NOTIFY_COOLDOWN_MS) return
        prefs.edit().putLong("last_notify", now).apply()

        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Protection status", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Warns you if APE's blocking gets switched off."
                },
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("APE blocking is off")
            .setContentText("Turn the APE accessibility service back on to keep blocking working.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
        nm.notify(7712, notif)
    }

    companion object {
        private const val JOB_ID = 7711
        private const val CHANNEL_ID = "service_status"
        private const val NOTIFY_COOLDOWN_MS = 6 * 60 * 60 * 1000L // 6h

        fun schedule(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java) ?: return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, TameWatchdogJob::class.java))
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000L) // 15 min (system clamps the minimum)
                .build()
            runCatching { js.schedule(job) }
        }
    }
}
