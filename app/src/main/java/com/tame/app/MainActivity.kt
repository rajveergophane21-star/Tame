package com.tame.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.CrashScreen
import com.tame.app.ui.SystemActions
import com.tame.app.ui.TameRoot
import com.tame.app.ui.theme.Accents
import com.tame.app.ui.theme.TameTheme
import com.tame.app.util.AccessibilityUtil
import com.tame.app.util.CrashReporter

class MainActivity : ComponentActivity(), SystemActions {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Apply the saved theme before the first frame (bootPrefs mirrors the darkMode setting).
        com.tame.app.ui.theme.TameColors.applyDark(
            getSharedPreferences("tame_boot", MODE_PRIVATE).getBoolean("dark", false)
        )
        vm.systemActions = this
        maybeRequestNotifications()
        val initialCrash = CrashReporter.consume(this)
        setContent {
            var crash by remember { mutableStateOf(initialCrash) }
            val palette = Accents.byKey(vm.settings.accentKey)
            TameTheme(accent = palette) {
                val current = crash
                if (current != null) {
                    CrashScreen(current) { CrashReporter.clear(this@MainActivity); crash = null }
                } else {
                    TameRoot(vm)
                }
            }
        }
        vm.rescheduleAllAlarms()
    }

    override fun onResume() {
        super.onResume()
        vm.refreshPerms()
        vm.ensureDay()
    }

    private fun maybeRequestNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(perm), 1001)
            }
        }
    }

    // ── SystemActions ──
    override fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    override fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }

    override fun isAccessibilityOn(): Boolean = AccessibilityUtil.isEnabled(this)

    override fun isOverlayOn(): Boolean = Settings.canDrawOverlays(this)

    override fun isBatteryUnrestricted(): Boolean {
        val pm = getSystemService(android.os.PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    override fun pinHomeWidget() {
        val mgr = getSystemService(android.appwidget.AppWidgetManager::class.java) ?: return
        val provider = android.content.ComponentName(this, com.tame.app.widget.ReelWidgetProvider::class.java)
        if (mgr.isRequestPinAppWidgetSupported) {
            runCatching { mgr.requestPinAppWidget(provider, null, null) }
        }
    }

    override fun openBatterySettings() {
        // Use the unrestricted-list settings screen (no special permission needed, so it
        // won't complicate the Play review). Falls back to this app's details page.
        val opened = runCatching {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }.isSuccess
        if (!opened) runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        }
    }
}
