package com.tame.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.tame.app.MainActivity
import com.tame.app.R
import com.tame.app.TameApp
import com.tame.app.data.TameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Minimal home-screen widget: today's reel count and time spent on feeds. Values are pushed
 * from the accessibility service as they change (see ReelWidgetProvider.update) and refreshed
 * from disk on the system's periodic update / when the widget is first placed.
 */
class ReelWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        // Read off the main thread (onReceive runs on main; a blocking DataStore read there
        // risks jank/ANR) and finish the broadcast via goAsync once views are pushed.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val s = runCatching { TameApp.repo.snapshot() }.getOrNull()?.settings
                // Counts reset when the app/service performs the daily rollover. If neither has
                // run since midnight, yesterday's totals are still on disk — show 0 instead of
                // presenting stale numbers as "REELS TODAY".
                val fresh = s != null && s.lastReelDay == TameRepository.today()
                val reels = if (fresh) s?.todayReels ?: 0 else 0
                val secs = if (fresh) s?.reelSeconds ?: 0 else 0
                val views = buildViews(context, reels, secs)
                for (id in ids) mgr.updateAppWidget(id, views)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private fun timeText(seconds: Int): String {
            val mins = seconds / 60
            return if (mins >= 60) "${mins / 60}h ${mins % 60}m watched" else "${mins}m watched"
        }

        private fun buildViews(context: Context, reels: Int, seconds: Int): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_reels).apply {
                setTextViewText(R.id.widget_reels, reels.toString())
                setTextViewText(R.id.widget_time, timeText(seconds))
                val pi = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                setOnClickPendingIntent(R.id.widget_root, pi)
            }

        /** Push fresh values to every placed widget (no-op if none are on the home screen). */
        fun update(context: Context, reels: Int, seconds: Int) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, ReelWidgetProvider::class.java))
            if (ids == null || ids.isEmpty()) return
            mgr.updateAppWidget(ids, buildViews(context, reels, seconds))
        }
    }
}
