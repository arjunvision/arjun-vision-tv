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
