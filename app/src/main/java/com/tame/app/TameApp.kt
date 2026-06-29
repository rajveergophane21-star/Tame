package com.tame.app

import android.app.Application
import com.tame.app.alarm.AlarmScheduler
import com.tame.app.data.TameRepository
import com.tame.app.service.TameWatchdogJob
import com.tame.app.util.CrashReporter

class TameApp : Application() {

    lateinit var repo: TameRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appInstance = this
        CrashReporter.install(this)
        repo = TameRepository(this)
        AlarmScheduler.ensureChannel(this)
        TameWatchdogJob.schedule(this)
    }

    companion object {
        private lateinit var appInstance: TameApp
        val repo: TameRepository get() = appInstance.repo
    }
}
