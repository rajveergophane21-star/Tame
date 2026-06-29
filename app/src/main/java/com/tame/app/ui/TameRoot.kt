package com.tame.app.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.tame.app.ui.components.BottomNav
import com.tame.app.ui.components.ToastBubble
import com.tame.app.ui.screens.AddRuleScreen
import com.tame.app.ui.screens.HabitsScreen
import com.tame.app.ui.screens.HomeScreen
import com.tame.app.ui.screens.OnboardingScreen
import com.tame.app.ui.screens.RuleDetailScreen
import com.tame.app.ui.screens.RulesScreen
import com.tame.app.ui.screens.SettingsScreen
import com.tame.app.ui.sheets.FocusSheet
import com.tame.app.ui.sheets.HabitEditorSheet
import com.tame.app.ui.takeover.AlarmTakeover
import com.tame.app.ui.takeover.BlockTakeover
import com.tame.app.ui.takeover.FocusRunTakeover
import com.tame.app.ui.takeover.FrictionTakeover
import com.tame.app.ui.takeover.ReelsTakeover
import com.tame.app.ui.theme.TameColors

private val darkScreens = setOf(Screen.BLOCK, Screen.FRICTION, Screen.FOCUS_RUN, Screen.REELS, Screen.ALARM)
private val navScreens = setOf(Screen.HOME, Screen.RULES, Screen.HABITS, Screen.SETTINGS, Screen.DETAIL)

@Composable
fun TameRoot(vm: AppViewModel) {
    val screen = vm.screen

    // keep system status-bar icon colour in sync with the current screen
    val view = LocalView.current
    val dark = screen in darkScreens
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) TameColors.InkDark else TameColors.Surface)
    ) {
        when (screen) {
            Screen.ONBOARDING -> OnboardingScreen(vm)
            Screen.HOME -> HomeScreen(vm)
            Screen.RULES -> RulesScreen(vm)
            Screen.ADD_RULE -> AddRuleScreen(vm)
            Screen.DETAIL -> RuleDetailScreen(vm)
            Screen.HABITS -> HabitsScreen(vm)
            Screen.SETTINGS -> SettingsScreen(vm)
            Screen.BLOCK -> BlockTakeover(vm)
            Screen.FRICTION -> FrictionTakeover(vm)
            Screen.FOCUS_RUN -> FocusRunTakeover(vm)
            Screen.REELS -> ReelsTakeover(vm)
            Screen.ALARM -> AlarmTakeover(vm)
        }

        if (screen in navScreens) {
            BottomNav(
                current = screen,
                onHome = vm::goHome,
                onRules = vm::goRules,
                onHabits = vm::goHabits,
                onYou = vm::goSettings,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (vm.sheet == "focus") FocusSheet(vm)
        if (vm.editHabit != null) HabitEditorSheet(vm)

        ToastBubble(
            vm.toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 108.dp),
        )
    }
}
