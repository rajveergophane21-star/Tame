package com.tame.app.data

import com.tame.app.data.model.Habit
import com.tame.app.data.model.Rule
import com.tame.app.data.model.RuleKind
import com.tame.app.data.model.RuleMode
import com.tame.app.data.model.SchedMode
import com.tame.app.data.model.Settings
import com.tame.app.data.model.TameData

/** First-run content that mirrors the Tame design so the app looks populated out of the box. */
object SampleData {
    fun seed(): TameData = TameData(
        settings = Settings(
            onboarded = false,
            accentKey = "grove",
            counterStyle = "bubble",
            blockStyle = "frank",
            reelLimit = 40,
            todayReels = 0,
        ),
        rules = listOf(
            Rule(
                id = "r1", kind = RuleKind.FEED, targets = listOf("ig", "yt"),
                mode = RuleMode.FRICTION, schedMode = SchedMode.ALL_DAY, limit = 40,
            ),
            Rule(
                id = "r2", kind = RuleKind.APP, targets = listOf("tt"),
                mode = RuleMode.BLOCK, schedMode = SchedMode.CUSTOM,
                days = listOf(true, true, true, true, true, false, false),
                fromHour = 9, toHour = 17, committed = true,
            ),
            Rule(
                id = "r3", kind = RuleKind.FEED, targets = listOf("tt", "sc"),
                mode = RuleMode.BLOCK, schedMode = SchedMode.CUSTOM,
                days = List(7) { true }, fromHour = 22, toHour = 7, limit = 0,
            ),
        ),
        habits = listOf(
            Habit(
                id = "h1", name = "Read 10 min", remind = "21:30",
                grid = listOf(1,0,1,1,1,1,0,1,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1,1,1,1,1,1),
            ),
            Habit(
                id = "h2", name = "No phone in bed", remind = "22:45",
                grid = listOf(1,1,0,1,0,1,1,0,1,1,0,0,1,1,0,1,0,1,1,0,1,0,1,1,0,1,1,0),
            ),
            Habit(
                id = "h3", name = "Walk", remind = "08:00",
                grid = listOf(1,1,0,1,1,1,1,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1),
            ),
        ),
    )
}
