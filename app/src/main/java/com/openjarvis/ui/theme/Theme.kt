package com.openjarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object VoidColor {
    val Void950 = Color(0xFF06060A)
    val Void900 = Color(0xFF0A0A0F)
    val Void800 = Color(0xFF12121B)
    val Void700 = Color(0xFF1A1A26)
    val Void600 = Color(0xFF2A2A3B)
    val BorderSubtle = Color(0xFF2E2E42)
    val BorderGlow = Color(0xFF8B5CF6)
    val Violet = Color(0xFF8B5CF6)
    val VioletDim = Color(0xFF4C1D95)
    val Cyan = Color(0xFF22D3EE)
    val Green = Color(0xFF22C55E)
    val Amber = Color(0xFFF59E0B)
    val Red = Color(0xFFEF4444)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextDisabled = Color(0xFF64748B)
}

private val MahiruDarkColorScheme = darkColorScheme(
    primary = VoidColor.Violet,
    secondary = VoidColor.Cyan,
    background = VoidColor.Void900,
    surface = VoidColor.Void900,
    surfaceVariant = VoidColor.Void700,
    onPrimary = Color.White,
    onBackground = VoidColor.TextPrimary,
    onSurface = VoidColor.TextPrimary,
    onSurfaceVariant = VoidColor.TextSecondary
)

@Composable
fun OpenJarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MahiruDarkColorScheme,
        content = content
    )
}
