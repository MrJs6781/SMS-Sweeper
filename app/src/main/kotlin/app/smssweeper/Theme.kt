package app.smssweeper

import androidx.compose.foundation.isSystemInDarkTheme
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

val Teal = Color(0xFF19D3B0)
val NavyTop = Color(0xFF0E1330)
val NavyBottom = Color(0xFF1E2760)

private val Light = lightColorScheme(
    primary = Color(0xFF4F5BFF),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFDDE0FF),
    onSecondaryContainer = Color(0xFF14196B),
    background = Color(0xFFF4F5FF),
    surface = Color.White,
    onSurface = Color(0xFF14172E),
    onSurfaceVariant = Color(0xFF555A7A),
    error = Color(0xFFE5384F),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8C97FF),
    onPrimary = NavyTop,
    secondaryContainer = Color(0xFF2A3470),
    onSecondaryContainer = Color(0xFFDDE0FF),
    background = NavyTop,
    surface = Color(0xFF171E48),
    onSurface = Color(0xFFEDEFFF),
    onSurfaceVariant = Color(0xFFA9AFD6),
    error = Color(0xFFFF6B7D),
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
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Typography().withFont(IranSans),
        content = content,
    )
}
