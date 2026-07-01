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

/**
 * Minimal home-screen widget: today's reel count and time spent on feeds. Values are pushed
 * from the accessibility service as they change (see ReelWidgetProvider.update) and refreshed
 * from disk on the system's periodic update / when the widget is first placed.
 */
class ReelWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val d = runCatching { TameApp.repo.snapshot() }.getOrNull()
        val reels = d?.settings?.todayReels ?: 0
        val secs = d?.settings?.reelSeconds ?: 0
        val views = buildViews(context, reels, secs)
        for (id in ids) mgr.updateAppWidget(id, views)
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
