package com.nakitradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkFintechColors = darkColorScheme(
    primary = Color(0xFF10B981),       // Zümrüt Yeşili (Nakit İade Vurgusu)
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFF59E0B),     // Altın Sarısı (Turkcell / Kombo Sinerji Vurgusu)
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF38BDF8),      // Paycell / Dijital Cüzdan Mavisi
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun NakitRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkFintechColors,
        content = content
    )
}
