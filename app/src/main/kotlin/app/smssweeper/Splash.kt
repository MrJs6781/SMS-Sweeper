package app.smssweeper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

// Same geometry as res/drawable/ic_launcher_foreground.xml (108x108 grid)
private fun p(d: String) = PathParser().parsePathString(d).toPath()
private val bubble = p("M38,32H70A10,10 0 0 1 80,42V60A10,10 0 0 1 70,70H50L36,80L38,70A10,10 0 0 1 28,60V42A10,10 0 0 1 38,32Z")
private val checkPath = p("M37,52L44,59L58,44")
private val broomHead = p("M68.5,32.5L75.5,39.5L70.6,50.2L57.8,37.4Z")
private val star = p("M0,-1Q0,0 1,0Q0,0 0,1Q0,0 -1,0Q0,0 0,-1Z")
private val textLines = listOf(42f to 66f, 50f to 60f, 58f to 52f) // y to x-end, x starts at 36
private val LineColor = Color(0xFFC9D0FF)

private fun seg(v: Float, a: Float, b: Float) = ((v - a) / (b - a)).coerceIn(0f, 1f)
private fun smooth(x: Float) = x * x * (3 - 2 * x)
private fun backOut(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1
    val y = x - 1
    return 1 + c3 * y * y * y + c1 * y * y
}

@Composable
fun Splash(onDone: () -> Unit) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(2800, easing = LinearEasing))
        onDone()
    }
    val v = t.value
    val measure = remember { PathMeasure() }
    val textA = smooth(seg(v, 0.70f, 0.92f))

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyTop, NavyBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(220.dp)) { drawLogo(v, size.minDimension / 108f, measure) }
            Spacer(Modifier.height(20.dp))
            Text(
                "SMS Sweeper",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = textA; translationY = (1 - textA) * 28f },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "پاکسازی هوشمند پیامک‌ها",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                modifier = Modifier.graphicsLayer { alpha = textA; translationY = (1 - textA) * 28f },
            )
        }
    }
}

private fun DrawScope.drawLogo(v: Float, s: Float, measure: PathMeasure) {
    // soft glow behind the logo
    val glow = smooth(seg(v, 0f, 0.35f))
    val gr = size.minDimension * 0.8f
    drawCircle(
        Brush.radialGradient(listOf(Teal.copy(alpha = 0.22f * glow), Color.Transparent), center, gr),
        radius = gr,
    )

    withTransform({ scale(s, s, Offset.Zero) }) {
        // 1. bubble pops in
        val pop = backOut(seg(v, 0f, 0.22f))
        val bs = 0.4f + 0.6f * pop
        withTransform({ scale(bs, bs, Offset(54f, 54f)) }) {
            drawPath(bubble, Color.White.copy(alpha = seg(v, 0f, 0.1f)))

            // 2. text lines appear, then get wiped by the broom (sweep right -> left)
            val m = smooth(seg(v, 0.28f, 0.58f))
            val clipX = 68f - 34f * m
            val la = seg(v, 0.15f, 0.30f)
            clipRect(left = 0f, top = 0f, right = clipX, bottom = 108f) {
                for ((y, x2) in textLines) {
                    drawLine(LineColor.copy(alpha = la), Offset(36f, y), Offset(x2, y), 3f, StrokeCap.Round)
                }
            }

            // 3. check mark draws itself
            val cp = smooth(seg(v, 0.58f, 0.76f))
            if (cp > 0f) {
                measure.setPath(checkPath, false)
                val part = Path()
                measure.getSegment(0f, measure.length * cp, part, true)
                drawPath(part, Teal, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            // broom: slides in, sweeps across, returns to its resting pose
            val entry = 1f - smooth(seg(v, 0.14f, 0.28f))
            val back = smooth(seg(v, 0.62f, 0.84f))
            val f = m * (1f - back)
            val wiggle = if (m > 0f && m < 1f) 10f * sin(m * 12.566f) else 0f
            val ba = seg(v, 0.14f, 0.24f)
            withTransform({
                translate(-34f * f + 24f * entry, 14f * f - 24f * entry)
                rotate(wiggle, Offset(72f, 36f))
            }) {
                drawLine(Teal.copy(alpha = ba), Offset(84f, 24f), Offset(72f, 36f), 4f, StrokeCap.Round)
                drawPath(broomHead, Teal.copy(alpha = ba))
            }
        }

        // 4. sparkles
        val sp = backOut(seg(v, 0.72f, 0.92f))
        if (sp > 0f) {
            withTransform({ translate(24f, 28f); scale(5f * sp, 5f * sp, Offset.Zero) }) { drawPath(star, Teal) }
            withTransform({ translate(92f, 66f); scale(4f * sp, 4f * sp, Offset.Zero) }) { drawPath(star, Teal) }
        }
    }
}
