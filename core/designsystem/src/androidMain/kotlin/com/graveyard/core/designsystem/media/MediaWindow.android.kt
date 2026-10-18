package com.graveyard.core.designsystem.media

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * The window that owns the full-screen media surface: the dialog window when the gallery is hosted
 * in one, otherwise the activity window behind the current view.
 *
 * Returns null in the preview editor and for contexts without an activity.
 */
@Composable
internal fun rememberMediaWindow(): Window? {
    val view = LocalView.current
    if (view.isInEditMode) return null
    return (view.parent as? DialogWindowProvider)?.window ?: view.context.findActivity()?.window
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
