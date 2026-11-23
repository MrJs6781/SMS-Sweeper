package app.smssweeper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material "bar chart" glyph (the core icon set has none). */
val BarChartIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(5f, 9f); horizontalLineToRelative(3f); verticalLineToRelative(10f); horizontalLineToRelative(-3f); close()
        moveTo(10.5f, 5f); horizontalLineToRelative(3f); verticalLineToRelative(14f); horizontalLineToRelative(-3f); close()
        moveTo(16f, 12f); horizontalLineToRelative(3f); verticalLineToRelative(7f); horizontalLineToRelative(-3f); close()
    }.build()
}
