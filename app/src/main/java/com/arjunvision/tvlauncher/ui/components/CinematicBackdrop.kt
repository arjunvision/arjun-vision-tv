package com.arjunvision.tvlauncher.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect as AndroidRect
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import java.io.File
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val BACKDROP_W = 960
private const val BACKDROP_H = 540

/**
 * The whole screen background (theme colour + licensed wallpaper + readability scrim + aurora glow)
 * baked off the main thread into ONE opaque 960×540 image and drawn stretched.
 *
 * Why: an atrace on the MiTV-MOOR2 showed the GPU spending 45-53 ms per frame when wallpaper,
 * scrim and aurora were separate full-screen layers (about six full-screen fills per frame).
 * One opaque layer brings that down to a single fill. Motion: the aurora drifts by re-baking
 * twice a second while [animate] is true; otherwise only when colours change.
 */
@Composable
fun CinematicBackdrop(wallpaperPath: String?, animate: Boolean, modifier: Modifier = Modifier, glow: Color = LauncherColors.Blue) {
    val glowNow by rememberUpdatedState(glow)
    val background by rememberUpdatedState(LauncherColors.Background)
    val violet by rememberUpdatedState(LauncherColors.Violet)
    val blue by rememberUpdatedState(LauncherColors.Blue)

    val frame by produceState<ImageBitmap?>(initialValue = null, wallpaperPath, animate) {
        val wallpaper = wallpaperPath?.let { path -> withContext(Dispatchers.IO) { loadCropped(path) } }
        val buffers = arrayOf(newOpaque(), newOpaque())
        var index = 0
        var lastKey: List<Int>? = null
        while (true) {
            val key = listOf(glowNow.toArgb(), background.toArgb(), violet.toArgb(), blue.toArgb())
            if (animate || key != lastKey) {
                val t = if (animate) (System.currentTimeMillis() % 24_000L) / 24_000f else 0.15f
                val target = buffers[index]
                index = 1 - index // never repaint the bitmap currently on screen
                withContext(Dispatchers.Default) { bake(target, wallpaper, t, key, auroraStrength()) }
                value = target.asImageBitmap()
                lastKey = key
            }
            delay(if (animate) 500L else 400L)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frame?.let {
            drawImage(
                image = it,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(BACKDROP_W, BACKDROP_H),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.Low,
            )
        } ?: drawRect(LauncherColors.Background)
    }
}

private fun newOpaque(): Bitmap =
    Bitmap.createBitmap(BACKDROP_W, BACKDROP_H, Bitmap.Config.ARGB_8888).apply { setHasAlpha(false) }

private fun auroraStrength(now: LocalTime = LocalTime.now()): Float = when (now.hour) {
    in 7..17 -> 0.38f
    in 18..21 -> 0.30f
    else -> 0.22f
}

/** Wallpaper decoded small and centre-cropped to 16:9 once, so each bake is a plain copy. */
private fun loadCropped(path: String): Bitmap? = runCatching {
    if (!File(path).exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= BACKDROP_W) sample *= 2
    val src = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val out = Bitmap.createBitmap(BACKDROP_W, BACKDROP_H, Bitmap.Config.ARGB_8888)
    val scale = max(BACKDROP_W / src.width.toFloat(), BACKDROP_H / src.height.toFloat())
    val w = (BACKDROP_W / scale).toInt()
    val h = (BACKDROP_H / scale).toInt()
    val left = (src.width - w) / 2
    val top = (src.height - h) / 2
    AndroidCanvas(out).drawBitmap(src, AndroidRect(left, top, left + w, top + h), AndroidRect(0, 0, BACKDROP_W, BACKDROP_H), Paint(Paint.FILTER_BITMAP_FLAG))
    src.recycle()
    out
}.getOrNull()

private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

/** [colors] = glow, background, violet, blue (ARGB ints). */
private fun bake(target: Bitmap, wallpaper: Bitmap?, t: Float, colors: List<Int>, strength: Float) {
    val (glow, background, violet, blue) = colors
    val c = AndroidCanvas(target)
    val w = BACKDROP_W.toFloat()
    val h = BACKDROP_H.toFloat()
    c.drawColor(background or 0xFF000000.toInt())
    paint.shader = null
    if (wallpaper != null) {
        c.drawBitmap(wallpaper, 0f, 0f, paint)
        // Readability scrim: dark on the left where the text sits, lighter on the right.
        paint.shader = LinearGradient(0f, 0f, w, 0f, withAlpha(background, 0.92f), withAlpha(background, 0.35f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
    }
    val a = (t * 2 * PI).toFloat()
    fun field(color: Int, cx: Float, cy: Float, r: Float, alpha: Float) {
        val radius = max(w, h) * r
        paint.shader = RadialGradient(w * cx, h * cy, radius, withAlpha(color, alpha), 0, Shader.TileMode.CLAMP)
        c.drawCircle(w * cx, h * cy, radius, paint)
    }
    field(glow, 0.78f + 0.08f * cos(a), 0.18f + 0.06f * sin(a), 0.55f, strength)
    field(violet, 0.12f + 0.06f * sin(a * 0.7f), 0.85f + 0.05f * cos(a), 0.50f, strength * 0.85f)
    field(blue, 0.45f + 0.10f * sin(a * 1.3f), 0.55f + 0.08f * cos(a * 0.6f), 0.35f, strength * 0.35f)
    paint.shader = null
}

private fun withAlpha(color: Int, alpha: Float): Int =
    (color and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24)
