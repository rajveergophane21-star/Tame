package com.tame.app.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tame.app.data.model.Habit
import java.util.Calendar

/** Schedules exact, repeating-by-reschedule habit reminders via AlarmManager. */
object AlarmScheduler {

    const val CHANNEL_ID = "habit_reminders"
    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_HABIT_NAME = "habit_name"
    const val EXTRA_HABIT_TIME = "habit_time"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(com.tame.app.R.string.habit_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(com.tame.app.R.string.habit_channel_desc)
                setBypassDnd(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun reqCode(id: String): Int = id.hashCode()

    private fun pendingIntent(context: Context, habit: Habit): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_HABIT_ID, habit.id)
            putExtra(EXTRA_HABIT_NAME, habit.name)
            putExtra(EXTRA_HABIT_TIME, habit.remind)
        }
        return PendingIntent.getBroadcast(
            context, reqCode(habit.id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(context: Context, habit: Habit) {
        if (!habit.remindOn) { cancel(context, habit.id); return }
        val trigger = nextTrigger(habit) ?: return
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, habit)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            else am.setWindow(AlarmManager.RTC_WAKEUP, trigger, 60_000L, pi)
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, habitId: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, reqCode(habitId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
    }

    /** Next epoch-millis matching the habit's time on one of its enabled days. */
    private fun nextTrigger(habit: Habit): Long? {
        val parts = habit.remind.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val now = Calendar.getInstance()
        for (offset in 0..7) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now.timeInMillis) continue
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7   // Mon=0 .. Sun=6
            if (habit.days.getOrElse(dow) { true }) return cal.timeInMillis
        }
        return null
    }
}
