package com.tame.app.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.tame.app.ui.theme.AccentPalette
import com.tame.app.ui.theme.TameTheme

/**
 * Hosts the Compose stop screen in a full-screen TYPE_ACCESSIBILITY_OVERLAY window.
 * This is the reliable way to show a blocking screen over other apps from an
 * accessibility service — it floats above everything and is not subject to the
 * background-activity-launch restrictions that silently drop a started Activity.
 *
 * One instance per show (the lifecycle is created and destroyed with the window).
 */
class BlockOverlay(
    private val service: AccessibilityService,
    private val accent: AccentPalette,
    // how to leave: BACK for a feed (stay in the app), HOME for a whole-app block
    private val onLeave: () -> Unit,
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedState = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private val wm = service.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null

    val isShowing: Boolean get() = view != null

    fun showBlock(style: String, name: String, lifts: String) =
        mount { BlockContent(style = style, name = name, lifts = lifts, onBack = ::leave) }

    fun showFriction(name: String, onOpenAnyway: () -> Unit) =
        mount { FrictionContent(name = name, onStay = ::leave, onOpen = { hide(); onOpenAnyway() }) }

    private fun mount(content: @Composable () -> Unit) {
        if (view != null) return
        savedState.performAttach()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val cv = ComposeView(service).apply {
            setViewTreeLifecycleOwner(this@BlockOverlay)
            setViewTreeViewModelStoreOwner(this@BlockOverlay)
            setViewTreeSavedStateRegistryOwner(this@BlockOverlay)
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    leave(); true
                } else false
            }
            setContent { TameTheme(accent = accent) { content() } }
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val types = buildList {
            add(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
            if (android.provider.Settings.canDrawOverlays(service)) {
                add(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            }
        }
        var attached = false
        for (type in types) {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type, flags, PixelFormat.TRANSLUCENT,
            )
            if (runCatching { wm.addView(cv, params); cv.requestFocus() }.isSuccess) { attached = true; break }
        }
        if (attached) {
            view = cv
        } else {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private fun leave() {
        hide()
        onLeave()
    }

    fun hide() {
        val v = view ?: return
        view = null
        runCatching { wm.removeViewImmediate(v) }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        runCatching { store.clear() }
    }
}
