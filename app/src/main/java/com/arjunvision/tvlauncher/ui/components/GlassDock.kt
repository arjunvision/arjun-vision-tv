package com.arjunvision.tvlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle
import kotlinx.coroutines.launch

/** Shared state between the dock and its tiles: which tile is focused and where it is. */
class GlassHighlightState {
    var target by mutableStateOf<Rect?>(null)
    var accent by mutableStateOf<Color?>(null)
    internal var origin by mutableStateOf(Offset.Zero)

    /** Call from a tile when it gains focus (or moves while focused). */
    fun focusOn(coordinates: LayoutCoordinates, color: Color?) {
        val b = coordinates.boundsInWindow()
        target = b.translate(-origin.x, -origin.y)
        accent = color
    }
}

/**
 * Frosted-glass dock band with ONE floating glass highlight that springs from tile to tile
 * (inspired by the single overlay highlight in CleanYourTV, rebuilt in Compose).
 * The glass is gradients + a rim + a soft outer glow: no blur, so it runs at full frame rate
 * on Android 11.
 */
@Composable
fun GlassDock(
    state: GlassHighlightState,
    motion: Float,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    val rect = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    var visible by remember { mutableStateOf(false) }
    val springSpec = if (motion == 0f || LauncherStyle.lite) {
        spring(stiffness = Spring.StiffnessHigh * 10, dampingRatio = 1f)
    } else {
        spring<Rect>(dampingRatio = 0.62f, stiffness = 300f)
    }

    LaunchedEffect(state.target) {
        val t = state.target ?: run { visible = false; return@LaunchedEffect }
        val grown = Rect(t.left - pad, t.top - pad, t.right + pad, t.bottom + pad)
        if (!visible) {
            rect.snapTo(grown) // first focus: appear in place, then spring afterwards
            visible = true
        } else {
            launch { rect.animateTo(grown, springSpec) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { state.origin = it.boundsInWindow().topLeft },
    ) {
        // Frosted band behind the dock.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape((LauncherStyle.cardCornerDp + 12).dp))
                .background(
                    Brush.verticalGradient(listOf(LauncherColors.GlassFill, LauncherColors.GlassFill.copy(alpha = 0.04f))),
                ),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
            content = content,
        )
        if (visible) {
            val accent = state.accent ?: LauncherColors.Blue
            val corner = with(density) { (LauncherStyle.cardCornerDp + 6).dp.toPx() }
            Canvas(modifier = Modifier.matchParentSize()) {
                val r = rect.value
                val topLeft = r.topLeft
                val size = Size(r.width, r.height)
                // Soft outer glow: a few fading strokes (cheap, no blur).
                for (i in 1..5) {
                    val grow = i * 3f
                    drawRoundRect(
                        color = accent.copy(alpha = 0.10f / i),
                        topLeft = Offset(topLeft.x - grow, topLeft.y - grow),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = CornerRadius(corner + grow),
                        style = Stroke(width = 3f),
                    )
                }
                // Glass body and top sheen.
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f)),
                        startY = topLeft.y,
                        endY = topLeft.y + size.height,
                    ),
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(corner),
                )
                // Rim tinted with the focused app's colour.
                drawRoundRect(
                    color = accent.copy(alpha = 0.95f),
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(corner),
                    style = Stroke(width = with(density) { 2.5.dp.toPx() }),
                )
            }
        }
    }
}
