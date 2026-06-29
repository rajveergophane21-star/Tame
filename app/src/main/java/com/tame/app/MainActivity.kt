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

    override fun requestExactAlarmIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val am = getSystemService(android.app.AlarmManager::class.java)
            if (am != null && !am.canScheduleExactAlarms()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                    )
                }
            }
        }
    }
}
