package com.tame.app.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.tame.app.service.TameAccessibilityService

object AccessibilityUtil {
    /** Is Tame's accessibility service currently enabled in system settings? */
    fun isEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${TameAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
