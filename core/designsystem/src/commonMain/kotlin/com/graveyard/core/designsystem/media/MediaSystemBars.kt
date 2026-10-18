package com.graveyard.core.designsystem.media

import androidx.compose.runtime.Composable

/**
 * Configures the media window for gallery-owned backgrounds and animations, restoring it on disposal.
 *
 * No-op where the platform has no app-controlled bar color.
 */
@Composable
internal expect fun MediaSystemBars()
