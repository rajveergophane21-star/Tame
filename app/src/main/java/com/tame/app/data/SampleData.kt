package com.tame.app.data

import com.tame.app.data.model.Settings
import com.tame.app.data.model.TameData

/** First-run state: empty — the user adds their own rules and habits. */
object SampleData {
    fun seed(): TameData = TameData(settings = Settings(onboarded = false))
}
