package com.graveyard.core.designsystem.media

import androidx.compose.runtime.Composable

/** iOS owns the system bars; the gallery only recolors their surroundings. */
@Composable
internal actual fun MediaSystemBarsVisibility(hidden: Boolean) = Unit
