package com.graveyard.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// DESIGN.md's typography applies to both color schemes. Use system sans-serif
// fonts, including platform fallback for Chinese, without bundling font assets.
// Font sizes, line heights and px tracking are expressed in sp for font scaling.
// Roles absent from the document retain the Material 3 defaults.
val AppTypography = Typography(
    displayLarge = appTextStyle(57, FontWeight.Bold, 64, (-0.25).sp),
    headlineLarge = appTextStyle(32, FontWeight.Bold, 40),
    headlineMedium = appTextStyle(24, FontWeight.SemiBold, 32),
    headlineSmall = appTextStyle(20, FontWeight.SemiBold, 28),
    titleLarge = appTextStyle(18, FontWeight.SemiBold, 24),
    titleMedium = appTextStyle(16, FontWeight.SemiBold, 22),
    bodyLarge = appTextStyle(16, FontWeight.Normal, 24, 0.15.sp),
    bodyMedium = appTextStyle(14, FontWeight.Normal, 20, 0.25.sp),
    bodySmall = appTextStyle(12, FontWeight.Normal, 16, 0.4.sp),
    labelLarge = appTextStyle(14, FontWeight.SemiBold, 20, 0.1.sp),
    labelMedium = appTextStyle(12, FontWeight.SemiBold, 16, 0.5.sp),
    labelSmall = appTextStyle(11, FontWeight.SemiBold, 14, 0.5.sp),
)

val AppMobileTypography = AppTypography.copy(
    headlineLarge = appTextStyle(28, FontWeight.Bold, 36),
)

private fun appTextStyle(
    fontSize: Int,
    fontWeight: FontWeight,
    lineHeight: Int,
    letterSpacing: TextUnit = 0.sp,
): TextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = fontSize.sp,
    fontWeight = fontWeight,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing,
)
