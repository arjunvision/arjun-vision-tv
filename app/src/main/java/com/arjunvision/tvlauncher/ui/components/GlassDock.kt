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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
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
    // Only the POSITION animates (a GPU translation of a cached layer). The glass itself is redrawn
    // only when its size changes (moving between tiles of different widths). Redrawing seven
    // anti-aliased outlines every spring frame cost ~50 ms per frame on the MiTV-MOOR2.
    val position = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var glassSize by remember { mutableStateOf(Size.Zero) }
    var visible by remember { mutableStateOf(false) }
    val instant = motion == 0f || LauncherStyle.lite
    val currentInstant by rememberUpdatedState(instant)

    // Observed with snapshotFlow (not read during composition), so moving focus or scrolling the
    // dock never rebuilds the dock and its tiles.
    LaunchedEffect(state) {
        snapshotFlow { state.target }.collectLatest { t ->
            if (t == null) {
                visible = false
                return@collectLatest
            }
            val grown = Rect(t.left - pad, t.top - pad, t.right + pad, t.bottom + pad)
            glassSize = grown.size
            if (!visible || currentInstant) {
                position.snapTo(grown.topLeft) // first focus (or Lite Mode): appear in place
                visible = true
            } else {
                position.animateTo(grown.topLeft, spring(dampingRatio = 0.62f, stiffness = 300f))
            }
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
        if (visible && glassSize != Size.Zero) {
            val corner = with(density) { (LauncherStyle.cardCornerDp + 6).dp.toPx() }
            val rim = with(density) { 2.5.dp.toPx() }
            Canvas(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = position.value.x // read in the layer phase: no redraw, no relayout
                        translationY = position.value.y
                    }
                    .size(with(density) { glassSize.width.toDp() }, with(density) { glassSize.height.toDp() }),
            ) {
                val accent = state.accent ?: LauncherColors.Blue
                // Soft outer glow: two fading outlines (no blur).
                for (i in 1..2) {
                    val grow = i * 4f
                    drawRoundRect(
                        color = accent.copy(alpha = 0.16f / i),
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = CornerRadius(corner + grow),
                        style = Stroke(width = 4f),
                    )
                }
                // Glass body and top sheen.
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))),
                    cornerRadius = CornerRadius(corner),
                )
                // Rim tinted with the focused app's colour.
                drawRoundRect(color = accent.copy(alpha = 0.95f), cornerRadius = CornerRadius(corner), style = Stroke(width = rim))
            }
        }
    }
}
