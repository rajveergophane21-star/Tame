package com.tame.app.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tame.app.R
import com.tame.app.TameApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires at a habit's reminder time: shows a full-screen alarm and re-arms the next one. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_ID) ?: return
        val name = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_NAME) ?: "Habit"
        val time = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_TIME) ?: ""

        AlarmScheduler.ensureChannel(context)

        // Do the DataStore read, notification post and reschedule off the main thread.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val data = TameApp.repo.snapshot()
                val accent = data.settings.accentKey

                val fullScreen = Intent(context, AlarmRingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(AlarmScheduler.EXTRA_HABIT_ID, id)
                    putExtra(AlarmScheduler.EXTRA_HABIT_NAME, name)
                    putExtra(AlarmScheduler.EXTRA_HABIT_TIME, time)
                    putExtra(AlarmScheduler.EXTRA_ACCENT, accent)
                }
                val fsPi = PendingIntent.getActivity(
                    context, id.hashCode(), fullScreen,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val notif = NotificationCompat.Builder(context, AlarmScheduler.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(name)
                    .setContentText("Time for your habit")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setOngoing(true)
                    .setFullScreenIntent(fsPi, true)
                    .setContentIntent(fsPi)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()

                context.getSystemService(NotificationManager::class.java).notify(id.hashCode(), notif)

                // Android 14+ can downgrade full-screen intents for non-alarm apps, so the
                // ring screen may not pop up on its own. We hold the draw-over-apps
                // permission, which also permits launching an activity from the background —
                // use it as a fallback so the reminder still rings when the app is closed.
                if (android.provider.Settings.canDrawOverlays(context)) {
                    runCatching { context.startActivity(fullScreen) }
                }

                // Re-arm the next occurrence (AlarmManager exact alarms are one-shot).
                val habit = data.habits.firstOrNull { it.id == id }
                if (habit != null && habit.remindOn) AlarmScheduler.schedule(context, habit)
            } finally {
                pending.finish()
            }
        }
    }
}
