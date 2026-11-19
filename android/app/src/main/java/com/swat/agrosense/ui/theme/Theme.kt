package com.swat.agrosense.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Green = Color(0xFF176B45)
val GreenDark = Color(0xFF145B3B)
val GreenSoft = Color(0xFFE8F4ED)
val Ink = Color(0xFF1C2B23)
val Muted = Color(0xFF68776E)
val Line = Color(0xFFDCE6DF)
val Bg = Color(0xFFF4F7F5)
val Danger = Color(0xFFA83434)
val Warn = Color(0xFF8A5B08)

/** Pares de color RGB para los botones (degradado inicio → fin). */
object Rgb {
    val Red = Color(0xFFFF1744) to Color(0xFFFF8A00)
    val Green = Color(0xFF00C853) to Color(0xFF00B8D4)
    val Blue = Color(0xFF2979FF) to Color(0xFFD500F9)
    val Purple = Color(0xFF7C4DFF) to Color(0xFFFF4081)
    val Orange = Color(0xFFFF6D00) to Color(0xFFFFD600)
    val Teal = Color(0xFF00BFA5) to Color(0xFF2962FF)
    val spectrum = listOf(Color(0xFFFF0040), Color(0xFFFFA000), Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFFD500F9), Color(0xFFFF0040))
}

private val scheme = lightColorScheme(
    primary = Green, onPrimary = Color.White, primaryContainer = GreenSoft, onPrimaryContainer = GreenDark,
    secondary = Color(0xFF2979FF), background = Bg, surface = Color.White, onSurface = Ink, onBackground = Ink,
    surfaceVariant = Color(0xFFEDF4EF), outline = Line, error = Danger
)

private val type = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
)

@Composable
fun AgroSenseTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, typography = type, content = content)
