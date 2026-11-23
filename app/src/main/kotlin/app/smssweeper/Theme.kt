package app.smssweeper

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val IranSans = FontFamily(
    Font(R.font.iransans_regular, FontWeight.Normal),
    Font(R.font.iransans_medium, FontWeight.Medium),
    Font(R.font.iransans_bold, FontWeight.Bold),
)

// Splash / brand
val Teal = Color(0xFF19D3B0)
val NavyTop = Color(0xFF0E1330)
val NavyBottom = Color(0xFF1E2760)

// Minimal palette: cool neutrals + one blue accent; red is reserved for destructive actions.
private val Light = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFFF3F6FC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EEF9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFDDE5F2),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F6FC),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF2FA),
    surfaceContainerHighest = Color(0xFFE8EEF9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0B1B3A),
    secondaryContainer = Color(0xFF1D3560),
    onSecondaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6ECF8),
    surface = Color(0xFF121B2E),
    onSurface = Color(0xFFE6ECF8),
    surfaceVariant = Color(0xFF1A2540),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334568),
    outlineVariant = Color(0xFF22304D),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF3B1212),
    onErrorContainer = Color(0xFFFECACA),
    surfaceContainerLowest = Color(0xFF0B1220),
    surfaceContainerLow = Color(0xFF0E1627),
    surfaceContainer = Color(0xFF121B2E),
    surfaceContainerHigh = Color(0xFF18233C),
    surfaceContainerHighest = Color(0xFF1E2B49),
)

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = Typography().withFont(IranSans),
        content = content,
    )
}
