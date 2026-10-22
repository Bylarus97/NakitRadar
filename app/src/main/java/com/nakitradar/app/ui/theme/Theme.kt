package com.nakitradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightFintechColors = lightColorScheme(
    primary = Color(0xFF059669),            // Canlı Zümrüt Yeşili (Nakit İade Vurgusu)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),   // Yumuşak Nane Yeşili Rozet Zemini
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF2563EB),          // Modern Fintech Mavisi (Paycell / Cüzdan Vurgusu)
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = Color(0xFFD97706),           // Sıcak Kehribar / Altın (Turkcell & Sinerji Vurgusu)
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    background = Color(0xFFF4F6FA),         // Ferah Apple Wallet / Juzdan Açık Gri-Mavi Zemin
    onBackground = Color(0xFF0F172A),       // Koyu Lacivert-Siyah Net Tipografi
    surface = Color(0xFFFFFFFF),            // Saf Beyaz Kart Zemini
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F6),     // Yumuşak Gri İç Kutu / Sekme Zemini
    onSurfaceVariant = Color(0xFF64748B),   // İkincil Metin Rengi
    outline = Color(0xFFE2E8F0)
)

@Composable
fun NakitRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightFintechColors,
        content = content
    )
}
