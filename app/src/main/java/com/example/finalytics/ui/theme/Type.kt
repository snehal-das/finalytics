package com.example.finalytics.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
private val defaultTypography = Typography()
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = FontFamily.Monospace),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = FontFamily.Monospace),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = FontFamily.Monospace),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = FontFamily.Monospace),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = FontFamily.Monospace),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = FontFamily.Monospace),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = FontFamily.Monospace),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = FontFamily.Monospace),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = FontFamily.Monospace),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = FontFamily.Monospace)
)