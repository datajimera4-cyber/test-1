package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Slate900,
    primaryContainer = AmberDark,
    onPrimaryContainer = AmberLight,
    secondary = Color(0xFF38BDF8),
    onSecondary = Slate900,
    background = Color(0xFF070B12), // Ultra-deep luxury obsidian slate
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A),    // Sleek elevated dark slate
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B), // Card container
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = AlertRed,
    onError = Color.White
)

private val LightColorScheme = DarkColorScheme // Default user app to dark theme as requested

@Composable
fun WatchEarnTheme(
    darkTheme: Boolean = true, // Default to deep dark luxury theme for the User App
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = WatchEarnTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
