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
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue
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
