package com.graveyard.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.graveyard.core.designsystem.theme.darkDefaultScheme
import com.graveyard.core.designsystem.theme.lightDefaultScheme


actual fun supportsDynamicTheming(): Boolean = false

@Composable
actual fun dynamicAppColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) darkDefaultScheme else lightDefaultScheme
}

