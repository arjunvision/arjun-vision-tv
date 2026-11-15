package com.arjunvision.tvlauncher.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.arjunvision.tvlauncher.domain.models.ParticleStyle
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import java.io.File
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Time-of-day mood: warmer in the morning, brighter by day, deeper at night. */
fun timeOfDayGreeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Good night"
}

private fun timeOfDayGlow(now: LocalTime = LocalTime.now()): Float = when (now.hour) {
    in 7..17 -> 0.38f
    in 18..21 -> 0.30f
    else -> 0.22f
}

/**
 * Aurora: three soft colour fields drifting slowly. Pure gradients (no blur), so it stays smooth
 * on Android 11 TV GPUs. [animate] false (Lite Mode / animations off) draws a still frame.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, animate: Boolean, glow: Color = LauncherColors.Blue) {
    // Rendered into a tiny 160x90 bitmap 4 times a second, then stretched with bilinear filtering:
    // one cheap texture blit per frame instead of three full-screen gradient fills. Full-screen
    // gradients measured ~60 ms per frame on the MiTV-MOOR2 GPU.
    val glowColor by rememberUpdatedState(glow)
    val violet by rememberUpdatedState(LauncherColors.Violet)
    val blue by rememberUpdatedState(LauncherColors.Blue)
    val frame by produceState<ImageBitmap?>(initialValue = null, animate) {
        val buffers = arrayOf(
            Bitmap.createBitmap(AURORA_W, AURORA_H, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(AURORA_W, AURORA_H, Bitmap.Config.ARGB_8888),
        )
        var index = 0
        while (true) {
            val t = if (animate) (System.currentTimeMillis() % 24_000L) / 24_000f else 0.15f
            val target = buffers[index]
            index = 1 - index // double buffer: never repaint the bitmap that is on screen
            renderAurora(target, t, glowColor.toArgb(), violet.toArgb(), blue.toArgb(), timeOfDayGlow())
            value = target.asImageBitmap()
            delay(if (animate) 250L else 1_000L) // still mode refreshes slowly so accent changes show
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        frame?.let {
            drawImage(
                image = it,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(AURORA_W, AURORA_H),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.Low,
            )
        }
    }
}

private const val AURORA_W = 160
private const val AURORA_H = 90
private val auroraPaint = Paint(Paint.ANTI_ALIAS_FLAG)

private fun renderAurora(bitmap: Bitmap, t: Float, glow: Int, violet: Int, blue: Int, strength: Float) {
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()
    val a = (t * 2 * PI).toFloat()
    fun field(color: Int, cx: Float, cy: Float, r: Float, alpha: Float) {
        val radius = maxOf(w, h) * r
        val c = (color and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24)
        auroraPaint.shader = RadialGradient(w * cx, h * cy, radius, c, 0, Shader.TileMode.CLAMP)
        canvas.drawCircle(w * cx, h * cy, radius, auroraPaint)
    }
    field(glow, 0.78f + 0.08f * cos(a), 0.18f + 0.06f * sin(a), 0.55f, strength)
    field(violet, 0.12f + 0.06f * sin(a * 0.7f), 0.85f + 0.05f * cos(a), 0.50f, strength * 0.85f)
    field(blue, 0.45f + 0.10f * sin(a * 1.3f), 0.55f + 0.08f * cos(a * 0.6f), 0.35f, strength * 0.35f)
}

/** A 0..1 phase that advances every [stepMs] while [running]; a fixed [still] value otherwise. */
@Composable
private fun rememberSteppedPhase(periodMs: Long, stepMs: Long, running: Boolean, still: Float): State<Float> =
    produceState(initialValue = still, running) {
        if (!running) {
            value = still
            return@produceState
        }
        while (true) {
            value = (System.currentTimeMillis() % periodMs) / periodMs.toFloat()
            delay(stepMs)
        }
    }

/** Subtle themed particles (stars, snow, sparks, rain, petals). Fixed count, no allocation per frame. */
@Composable
fun ParticleLayer(style: ParticleStyle, animate: Boolean, modifier: Modifier = Modifier) {
    if (style == ParticleStyle.NONE) return
    // 20 updates a second, read at draw time only (no recomposition), 24 particles.
    val phase = rememberSteppedPhase(periodMs = 30_000, stepMs = 50, running = animate, still = 0f)
    val accent = LauncherColors.Blue
    val accent2 = LauncherColors.Violet
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = phase.value
        val count = 24
        for (i in 0 until count) {
            val seedX = ((i * 7919) % 1000) / 1000f
            val seedY = ((i * 104729) % 1000) / 1000f
            val speed = 0.5f + ((i * 31) % 10) / 10f
            val phase = (t * speed + seedY) % 1f
            val x: Float
            val y: Float
            when (style) {
                ParticleStyle.SNOW, ParticleStyle.PETALS -> {
                    x = size.width * ((seedX + 0.03f * sin((phase * 6.28f) + i)) % 1f)
                    y = size.height * phase
                }
                ParticleStyle.RAIN -> {
                    x = size.width * seedX
                    y = size.height * ((phase * 2f) % 1f)
                }
                ParticleStyle.SPARKS -> {
                    x = size.width * seedX
                    y = size.height * (1f - phase)
                }
                else -> { // STARS twinkle in place
                    x = size.width * seedX
                    y = size.height * seedY
                }
            }
            val twinkle = if (style == ParticleStyle.STARS) (0.35f + 0.65f * ((sin((t * 6.28f * speed) + i) + 1f) / 2f)) else 0.7f
            val color = when (style) {
                ParticleStyle.SNOW, ParticleStyle.STARS -> Color.White
                ParticleStyle.PETALS -> accent2
                ParticleStyle.SPARKS -> if (i % 2 == 0) accent else accent2
                else -> accent
            }.copy(alpha = 0.55f * twinkle)
            if (style == ParticleStyle.RAIN) {
                drawLine(color, Offset(x, y), Offset(x - 4f, y + 26f), strokeWidth = 2f)
            } else {
                drawCircle(color, radius = if (style == ParticleStyle.STARS) 2.2f else 3.5f, center = Offset(x, y))
            }
        }
    }
}

/**
 * Theme wallpaper with a cinematic cross-fade when the theme changes, plus a dark scrim so text
 * stays readable. Decoding happens off the main thread, sampled down to the screen width.
 */
@Composable
fun ThemeWallpaper(path: String?, modifier: Modifier = Modifier, maxWidth: Int = 1920) {
    Crossfade(targetState = path, animationSpec = tween(900), label = "wallpaper", modifier = modifier) { p ->
        if (p == null) return@Crossfade
        val bitmap by produceState<ImageBitmap?>(initialValue = null, p) {
            value = withContext(Dispatchers.IO) { decodeSampled(p, maxWidth) }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            bitmap?.let {
                Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(LauncherColors.Background.copy(alpha = 0.92f), LauncherColors.Background.copy(alpha = 0.35f)),
                        ),
                    ),
            )
        }
    }
}

fun decodeSampled(path: String, maxWidth: Int): ImageBitmap? = runCatching {
    if (!File(path).exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
    BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply {
            inSampleSize = sample
            // RGB_565 halves wallpaper memory (photos have no alpha); CleanYourTV does the same on 1 GB TVs.
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        },
    )?.asImageBitmap()
}.getOrNull()
