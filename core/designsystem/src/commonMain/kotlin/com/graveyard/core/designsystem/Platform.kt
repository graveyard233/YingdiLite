package com.graveyard.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

expect fun supportsDynamicTheming(): Boolean

@Composable
expect fun dynamicAppColorScheme(darkTheme: Boolean): ColorScheme