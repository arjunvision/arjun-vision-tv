package com.arjunvision.tvlauncher.ui.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.arjunvision.tvlauncher.R
import com.arjunvision.tvlauncher.domain.themes.ColorMath
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle

/** OK held at least this long opens the tile menu (timed from key-down, reliable on IR remotes). */
private const val HOLD_OK_MS = 550L

/** Two OK presses closer than this count as one, so an app or input is never launched twice. */
private const val OK_DEBOUNCE_MS = 600L

/** Shared across all cards: a fast second press on any card is ignored, not just on the same one. */
private object OkDebounce {
    private var lastAt = 0L
    fun allow(now: Long): Boolean {
        if (now - lastAt < OK_DEBOUNCE_MS) return false
        lastAt = now
        return true
    }
}

/**
 * A focusable surface for the TV remote. OK = [onClick]; holding OK or pressing MENU =
 * [onLongPress]. On focus it zooms with a spring and shows a glowing accent border, unless
 * [showFocusBorder] is false (the glass dock draws its own floating highlight instead).
 */
@Composable
fun FocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    motion: Float = 1f,
    shape: Shape = RoundedCornerShape(LauncherStyle.cardCornerDp.dp),
    background: Brush = Brush.linearGradient(listOf(LauncherColors.Surface, LauncherColors.Background)),
    showFocusBorder: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var okDownAt by remember { mutableLongStateOf(0L) }
    val zoom = if (LauncherStyle.lite) LauncherStyle.focusScale * 0.5f else LauncherStyle.focusScale
    val scale by animateFloatAsState(
        targetValue = if (focused) 1f + zoom * motion else 1f,
        animationSpec = if (motion == 0f) tween(0) else spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "focusScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused && showFocusBorder) LauncherColors.Blue else LauncherColors.GlassEdge.copy(alpha = 0.12f),
        animationSpec = tween(durationMillis = 200),
        label = "focusBorder",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(background)
            .border(width = if (focused && showFocusBorder) 3.dp else 1.dp, color = borderColor, shape = shape)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                when {
                    event.key == Key.Menu && event.type == KeyEventType.KeyUp && onLongPress != null -> {
                        onLongPress(); true
                    }
                    isOk && event.type == KeyEventType.KeyDown -> {
                        if (native.repeatCount == 0) okDownAt = native.downTime
                        true
                    }
                    isOk && event.type == KeyEventType.KeyUp -> {
                        // Only act if THIS card saw the press. A stray release (OK pressed in another
                        // app or during the startup animation) must never switch inputs or open apps.
                        if (okDownAt == 0L) return@onPreviewKeyEvent true
                        val held = native.eventTime - okDownAt
                        okDownAt = 0L
                        if (onLongPress != null && held >= HOLD_OK_MS) {
                            onLongPress()
                        } else if (OkDebounce.allow(native.eventTime)) {
                            onClick()
                        }
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
        content = content,
    )
}

/** The company logo (res/drawable-nodpi/arjun_vision_logo.png, copied from brand-assets). */
@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.arjun_vision_logo),
        contentDescription = "Arjun Vision logo",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

/** An installed app's icon plus its vivid colour (for the ambient glow), read once and cached. */
class AppVisual(val icon: androidx.compose.ui.graphics.ImageBitmap?, val color: Color?)

private val visualCache = HashMap<String, AppVisual>()

@Composable
fun rememberAppVisual(packageName: String): AppVisual {
    val context = LocalContext.current
    return remember(packageName) {
        visualCache.getOrPut(packageName) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val icon = drawable.toBitmap(160, 160)
                val small = drawable.toBitmap(24, 24)
                val pixels = IntArray(24 * 24).also { small.getPixels(it, 0, 24, 0, 0, 24, 24) }
                val vivid = ColorMath.dominantVividColor(pixels)?.let { Color(ColorMath.asAccent(it).toInt()) }
                AppVisual(icon.asImageBitmap(), vivid)
            }.getOrDefault(AppVisual(null, null))
        }
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val visual = rememberAppVisual(packageName)
    visual.icon?.let {
        Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier)
    }
}

/** True for the remote's back key; used where a screen needs its own Back handling. */
fun isBackKey(code: Int) = code == AndroidKeyEvent.KEYCODE_BACK
