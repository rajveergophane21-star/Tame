package com.tame.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.tame.app.R

/**
 * Manages the floating "reels watched" counter drawn over the feed app using the
 * display-over-other-apps permission. Built with classic views for reliability.
 */
@SuppressLint("StaticFieldLeak") // single global overlay; always removed in hide()/onUnbind()
object OverlayManager {

    private var root: View? = null
    private var countText: TextView? = null
    private var frank: ImageView? = null
    private var bar: View? = null

    // last values rendered — skip the view work when nothing changed
    private var lastReels = Int.MIN_VALUE
    private var lastLimit = Int.MIN_VALUE
    private var lastRatio = -1f

    private fun dp(ctx: Context, v: Float): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun showOrUpdate(ctx: Context, reels: Int, limit: Int, ratio: Float) {
        // already showing the same numbers — nothing to redraw
        if (root != null && reels == lastReels && limit == lastLimit && ratio == lastRatio) return
        val wm = ctx.getSystemService(WindowManager::class.java)
        val color = when {
            ratio >= 1f -> Color.parseColor("#E1574C")
            ratio >= 0.82f -> Color.parseColor("#F2B705")
            else -> Color.parseColor("#2F7A5A")
        }
        val mood = when {
            ratio >= 1f -> R.drawable.frank_panic
            ratio >= 0.82f -> R.drawable.frank_sad
            ratio >= 0.5f -> R.drawable.frank_normal
            else -> R.drawable.frank_calm
        }
        if (root == null) {
            val pill = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 8f), dp(ctx, 8f), dp(ctx, 13f), dp(ctx, 8f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx, 20f).toFloat()
                    setColor(Color.parseColor("#9E10120F")) // rgba(16,18,15,.62)
                }
            }
            frank = ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 34f), dp(ctx, 34f))
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(ctx, 9f), 0, 0, 0)
            }
            countText = TextView(ctx).apply {
                setTextColor(Color.WHITE)
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val track = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 62f), dp(ctx, 4f)).apply { topMargin = dp(ctx, 4f) }
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx, 3f).toFloat(); setColor(Color.parseColor("#2EFFFFFF"))
                }
            }
            bar = View(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(dp(ctx, 31f), dp(ctx, 4f))
                background = GradientDrawable().apply { cornerRadius = dp(ctx, 3f).toFloat(); setColor(color) }
            }
            track.addView(bar)
            col.addView(countText); col.addView(track)
            pill.addView(frank); pill.addView(col)

            // only cache the view if it actually attaches; otherwise retry next event
            if (!addCounter(ctx, wm, pill)) return
            root = pill
        }
        lastReels = reels; lastLimit = limit; lastRatio = ratio
        frank?.setImageResource(mood)
        countText?.text = "$reels/$limit"
        bar?.let {
            val pct = ratio.coerceIn(0f, 1f)
            val lp = it.layoutParams
            lp.width = (dp(ctx, 62f) * pct).toInt().coerceAtLeast(dp(ctx, 2f))
            it.layoutParams = lp
            (it.background as? GradientDrawable)?.setColor(color)
        }
    }

    /** Try to attach the counter, preferring the standard overlay type, then the a11y type. */
    private fun addCounter(ctx: Context, wm: WindowManager, pill: View): Boolean {
        val types = buildList {
            if (Settings.canDrawOverlays(ctx)) add(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            add(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        }
        for (type in types) {
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = dp(ctx, 14f); y = dp(ctx, 56f)
            }
            if (runCatching { wm.addView(pill, lp) }.isSuccess) return true
        }
        return false
    }

    fun hide(ctx: Context) {
        val r = root ?: return
        runCatching { ctx.getSystemService(WindowManager::class.java).removeView(r) }
        root = null; countText = null; frank = null; bar = null
        lastReels = Int.MIN_VALUE; lastLimit = Int.MIN_VALUE; lastRatio = -1f
    }
}
