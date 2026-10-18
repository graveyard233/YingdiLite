package com.graveyard.core.designsystem.media

import androidx.compose.ui.window.DialogProperties

actual fun imageGalleryDialogProperties(): DialogProperties = DialogProperties(
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)
