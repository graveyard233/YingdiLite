package com.graveyard.core.designsystem.platform

import androidx.compose.ui.platform.ClipEntry

/**
 * Builds a clipboard entry that holds [text] as plain text.
 *
 * Compose Multiplatform only exposes `ClipEntry.withPlainText` on iOS, so the platform differences
 * (and the Android clip label) stay in the Android/iOS actuals.
 */
expect fun plainTextClipEntry(text: String): ClipEntry
