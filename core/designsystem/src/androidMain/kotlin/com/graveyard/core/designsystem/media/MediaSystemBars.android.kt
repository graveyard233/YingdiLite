package com.graveyard.core.designsystem.media

import android.graphics.Color
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Keeps system bars readable in the window that owns full-screen media.
 *
 * `enableEdgeToEdge` derives the status bar icon appearance from the theme background, which is
 * still the (light) app surface behind a black gallery. The gallery draws the background under
 * transparent bars and owns dimming and animations so dragging can reveal the article underneath.
 * Contrast enforcement is disabled and icons stay light; disposing restores the window settings.
 */
@Composable
internal actual fun MediaSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = rememberMediaWindow() ?: return

    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previousLightIcons = controller.isAppearanceLightStatusBars
        val previousLightNavigation = controller.isAppearanceLightNavigationBars
        val previousContrast = @Suppress("DEPRECATION") window.isStatusBarContrastEnforced
        val previousNavigationContrast = @Suppress("DEPRECATION") window.isNavigationBarContrastEnforced
        @Suppress("DEPRECATION")
        val previousColor = window.statusBarColor
        @Suppress("DEPRECATION")
        val previousNavigationColor = window.navigationBarColor
        val previousDimBehind = window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0
        val previousDimAmount = window.attributes.dimAmount
        val previousAnimations = window.attributes.windowAnimations

        onDispose {
            controller.isAppearanceLightStatusBars = previousLightIcons
            controller.isAppearanceLightNavigationBars = previousLightNavigation
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = previousContrast
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = previousNavigationContrast
            @Suppress("DEPRECATION")
            window.statusBarColor = previousColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = previousNavigationColor
            window.setDimAmount(previousDimAmount)
            if (previousDimBehind) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            window.setWindowAnimations(previousAnimations)
        }
    }
    SideEffect {
        val controller = WindowCompat.getInsetsController(window, view)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        // A user-drawn bar keeps its own color without a system scrim layered on top.
        @Suppress("DEPRECATION")
        window.isStatusBarContrastEnforced = false
        controller.isAppearanceLightStatusBars = false
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.isNavigationBarContrastEnforced = false
        controller.isAppearanceLightNavigationBars = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0f)
        window.setWindowAnimations(0)
    }
}
