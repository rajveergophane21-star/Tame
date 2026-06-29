package com.tame.app.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tame.app.R
import com.tame.app.TameApp

/** Fires at a habit's reminder time: shows a full-screen alarm and re-arms the next one. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_ID) ?: return
        val name = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_NAME) ?: "Habit"
        val time = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_TIME) ?: ""

        AlarmScheduler.ensureChannel(context)

        val fullScreen = Intent(context, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(AlarmScheduler.EXTRA_HABIT_ID, id)
            putExtra(AlarmScheduler.EXTRA_HABIT_NAME, name)
            putExtra(AlarmScheduler.EXTRA_HABIT_TIME, time)
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
            .setAutoCancel(true)
            .setFullScreenIntent(fsPi, true)
            .setContentIntent(fsPi)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(id.hashCode(), notif)

        // Re-arm the next occurrence (AlarmManager exact alarms are one-shot).
        val pending = goAsync()
        try {
            val habit = TameApp.repo.snapshot().habits.firstOrNull { it.id == id }
            if (habit != null && habit.remindOn) AlarmScheduler.schedule(context, habit)
        } finally {
            pending.finish()
        }
    }
}
