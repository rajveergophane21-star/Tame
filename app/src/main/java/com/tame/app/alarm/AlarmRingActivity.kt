package com.tame.app.alarm

import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tame.app.TameApp
import com.tame.app.ui.components.Frank
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Accents
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.LocalAccent
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.TameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Full-screen, ring-until-stopped habit alarm (shows over the lock screen). */
class AlarmRingActivity : ComponentActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var habitId: String? = null
    private var nameState by mutableStateOf("Habit")
    private var timeState by mutableStateOf("")

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        }
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )

        readExtras(intent)
        startRinging()

        val palette = Accents.byKey(intent.getStringExtra(AlarmScheduler.EXTRA_ACCENT))
        setContent {
            TameTheme(accent = palette) {
                AlarmContent(
                    name = nameState,
                    time = timeState,
                    onDone = { markDoneAndFinish() },
                    onDismiss = { stopAndFinish() },
                )
            }
        }
    }

    // singleInstance: a second alarm reuses this instance — refresh its content.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readExtras(intent)
        stopRinging()
        startRinging()
    }

    private fun readExtras(intent: Intent) {
        habitId = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_ID)
        nameState = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_NAME) ?: "Habit"
        timeState = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_TIME) ?: ""
    }

    private fun startRinging() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(this@AlarmRingActivity, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                isLooping = true
                prepare()
                start()
            }
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VibratorManager::class.java)).defaultVibrator
        } else {
            @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as Vibrator)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 600), 0))
    }

    private fun stopRinging() {
        runCatching { player?.stop(); player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        habitId?.let { getSystemService(NotificationManager::class.java).cancel(it.hashCode()) }
    }

    private fun markDoneAndFinish() {
        val id = habitId
        if (id != null) {
            CoroutineScope(Dispatchers.IO).launch {
                TameApp.repo.update { d ->
                    d.copy(habits = d.habits.map { h ->
                        if (h.id != id || h.grid.isEmpty()) h
                        else h.copy(grid = h.grid.toMutableList().also { it[it.size - 1] = 1 })
                    })
                }
            }
        }
        stopAndFinish()
    }

    private fun stopAndFinish() { stopRinging(); finish() }

    override fun onStop() {
        // Only silence the alarm when the user actually dismissed it (Done/Not now → finish()).
        // A transient stop (lockscreen transition, another window briefly covering us) must NOT
        // kill the ring — an alarm should keep ringing until it's acknowledged.
        if (isFinishing) stopRinging()
        super.onStop()
    }

    override fun onDestroy() { stopRinging(); super.onDestroy() }
}

@Composable
private fun AlarmContent(name: String, time: String, onDone: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(TameColors.InkDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Frank(mood = "neutral", size = 108.dp, idle = false, shake = true)
            Text(time, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 46.sp, color = Color.White))
            Text(name, style = TextStyle(fontFamily = Hanken, fontSize = 17.sp, color = TameColors.OnDarkSub))
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 36.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(LocalAccent.current.pop)
                    .tap { onDone() },
                contentAlignment = Alignment.Center,
            ) {
                Text("Done — mark it off", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = TameColors.Ink))
            }
            Box(modifier = Modifier.height(44.dp).tap { onDismiss() }, contentAlignment = Alignment.Center) {
                Text("Not now", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TameColors.OnDarkSub))
            }
        }
    }
}
