package com.tame.app.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves uncaught exceptions to a file so the next app launch can show the user the
 * stack trace (no developer tools / adb needed to report a crash).
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                File(app.filesDir, FILE).writeText("Tame crash · $stamp\n\n$sw")
            }
            prev?.uncaughtException(thread, throwable)
        }
    }

    fun consume(context: Context): String? {
        val f = File(context.applicationContext.filesDir, FILE)
        if (!f.exists()) return null
        return runCatching { f.readText() }.getOrNull()
    }

    fun clear(context: Context) {
        runCatching { File(context.applicationContext.filesDir, FILE).delete() }
    }
}
