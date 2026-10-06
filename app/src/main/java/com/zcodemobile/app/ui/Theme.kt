package com.zcodemobile.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

const val THEME_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

private val BrandViolet = Color(0xFF6C5CE7)
private val BrandVioletLight = Color(0xFFA99BF5)
private val BrandInk = Color(0xFF151718)

private val LightColors = lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF),
    onPrimaryContainer = Color(0xFF241A4D),
    secondary = Color(0xFF5B5B6E),
    secondaryContainer = Color(0xFFE2E0F0),
    onSecondaryContainer = Color(0xFF1A1B2E),
    background = Color(0xFFFBFAFE),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFFBFAFE),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFE7E7F0),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
)

private val DarkColors = darkColorScheme(
    primary = BrandVioletLight,
    onPrimary = Color(0xFF241A4D),
    primaryContainer = Color(0xFF3F3570),
    onPrimaryContainer = Color(0xFFE6E1FF),
    secondary = Color(0xFFBFC0D0),
    secondaryContainer = Color(0xFF33343F),
    onSecondaryContainer = Color(0xFFE2E0F0),
    background = BrandInk,
    onBackground = Color(0xFFE4E2E8),
    surface = Color(0xFF1B1D1F),
    onSurface = Color(0xFFE4E2E8),
    surfaceVariant = Color(0xFF2A2C2F),
    onSurfaceVariant = Color(0xFFC0C0CA),
    outline = Color(0xFF8B8B95),
)

@Composable
fun ZcodeTheme(themeMode: Int = THEME_SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        THEME_LIGHT -> false
        THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
