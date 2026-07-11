package com.tame.app.util

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** A launchable installed app: package name, display label, and (small) icon. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

/** Loads the user's installed, launchable apps with real labels + icons. */
object InstalledApps {

    @Volatile private var cache: List<AppEntry>? = null
    private val byPkg = HashMap<String, AppEntry>()

    fun load(context: Context): List<AppEntry> {
        cache?.let { return it }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching { pm.queryIntentActivities(intent, 0) }.getOrNull().orEmpty()
        // Never offer the home launcher or dialer as block targets — blocking the home
        // screen would trap the user in a loop (Home button → block → "Back to home" → block).
        val essential = HashSet<String>()
        runCatching {
            pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                ?.activityInfo?.packageName?.let { essential.add(it) }
            pm.resolveActivity(Intent(Intent.ACTION_DIAL), 0)
                ?.activityInfo?.packageName?.let { essential.add(it) }
        }
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || pkg in essential) continue
            if (!seen.add(pkg)) continue
            val label = runCatching { ri.loadLabel(pm).toString() }.getOrNull()?.takeIf { it.isNotBlank() } ?: pkg
            val icon = runCatching { ri.activityInfo.loadIcon(pm).toBitmap(144, 144).asImageBitmap() }.getOrNull()
            out.add(AppEntry(pkg, label, icon))
        }
        out.sortBy { it.label.lowercase() }
        synchronized(byPkg) { byPkg.clear(); out.forEach { byPkg[it.packageName] = it } }
        cache = out
        return out
    }

    fun entry(pkg: String): AppEntry? = synchronized(byPkg) { byPkg[pkg] }

    fun label(context: Context, pkg: String): String {
        entry(pkg)?.let { return it.label }
        val pm = context.packageManager
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull() ?: pkg
    }
}
