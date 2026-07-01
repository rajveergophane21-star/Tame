package com.tame.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tame.app.TameApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms all habit reminders after a reboot, app update, or a timezone/clock change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val relevant = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED
        if (relevant) {
            AlarmScheduler.ensureChannel(context)
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    TameApp.repo.snapshot().habits.forEach {
                        if (it.remindOn) AlarmScheduler.schedule(context, it)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
