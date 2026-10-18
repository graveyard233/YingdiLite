package com.graveyard.core.designsystem.media

import androidx.compose.runtime.Composable

/**
 * Hides or restores the system bars that overlay full-screen media.
 *
 * While the bars are hidden the gallery chrome decides when they come back, like a photo viewer's
 * immersive mode. No-op where the platform has no app-controlled system bars.
 */
@Composable
internal expect fun MediaSystemBarsVisibility(hidden: Boolean)
