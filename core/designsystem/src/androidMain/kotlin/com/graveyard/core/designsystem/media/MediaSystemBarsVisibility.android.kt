package com.graveyard.core.designsystem.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Immersive gallery mode: hides both the status and navigation bars while [hidden].
 *
 * The bars stay hidden until this effect asks for them again; an edge swipe only reveals them
 * transiently. Disposing restores the previous bar behavior, so leaving the gallery never leaves
 * the app in an unrecoverable immersive state.
 */
@Composable
internal actual fun MediaSystemBarsVisibility(hidden: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = rememberMediaWindow() ?: return

    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previousBehavior = controller.systemBarsBehavior
        // Transient bars by swipe is handled relative to the edge the gesture starts from.
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            // The bars themselves are restored by the next composition's SideEffect before this
            // scene is torn down, so only the behavior has to be handed back here.
            controller.systemBarsBehavior = previousBehavior
        }
    }
    SideEffect {
        val controller = WindowCompat.getInsetsController(window, view)
        val bars = WindowInsetsCompat.Type.systemBars()
        if (hidden) controller.hide(bars) else controller.show(bars)
    }
}
