package com.nushhack26.app.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Placeholder palette. The plan job maps the design's colours here.
val Navy = Color(0xFF1F4E79)
val NavyLight = Color(0xFFD3E4F5)
val Teal = Color(0xFF2E7D73)
val TealLight = Color(0xFFCDEAE5)
val Amber = Color(0xFFB26A00)
val AmberLight = Color(0xFFFFE3B8)
val Red = Color(0xFFB3261E)
val RedLight = Color(0xFFF9DEDC)
val Paper = Color(0xFFFAFBFD)
val PaperShade = Color(0xFFE6EAF0)
val Ink = Color(0xFF1A1C1E)
val InkMuted = Color(0xFF44474E)
val Outline = Color(0xFF74777F)

val AppColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Navy,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealLight,
    onSecondaryContainer = Teal,
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = AmberLight,
    onTertiaryContainer = Color(0xFF4A2800),
    error = Red,
    onError = Color.White,
    errorContainer = RedLight,
    onErrorContainer = Color(0xFF410E0B),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperShade,
    onSurfaceVariant = InkMuted,
    outline = Outline,
)
