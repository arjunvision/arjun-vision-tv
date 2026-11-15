package com.arjunvision.tvlauncher.domain.themes

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Plain-Kotlin colour helpers (no Android classes), so they run in JVM unit tests. */
object ColorMath {

    fun red(c: Long) = ((c shr 16) and 0xFF).toInt()
    fun green(c: Long) = ((c shr 8) and 0xFF).toInt()
    fun blue(c: Long) = (c and 0xFF).toInt()

    fun rgb(r: Int, g: Int, b: Int): Long =
        (0xFFL shl 24) or (r.coerceIn(0, 255).toLong() shl 16) or
            (g.coerceIn(0, 255).toLong() shl 8) or b.coerceIn(0, 255).toLong()

    /** Linear mix: amount 0 = a, 1 = b. */
    fun blend(a: Long, b: Long, amount: Float): Long {
        val t = amount.coerceIn(0f, 1f)
        fun mix(x: Int, y: Int) = (x + (y - x) * t).roundToInt()
        return rgb(mix(red(a), red(b)), mix(green(a), green(b)), mix(blue(a), blue(b)))
    }

    /** Returns [hue 0-360, saturation 0-1, value 0-1]. */
    fun toHsv(c: Long): FloatArray {
        val r = red(c) / 255f
        val g = green(c) / 255f
        val b = blue(c) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val s = if (mx == 0f) 0f else d / mx
        return floatArrayOf(h, s, mx)
    }

    fun fromHsv(h: Float, s: Float, v: Float): Long {
        val c = v * s
        val x = c * (1 - abs((h / 60f) % 2f - 1))
        val m = v - c
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return rgb(((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    /** Makes a colour vivid enough to work as an accent on a dark background. */
    fun asAccent(c: Long): Long {
        val (h, s, v) = toHsv(c).let { Triple(it[0], it[1], it[2]) }
        return fromHsv(h, max(s, 0.55f), max(v, 0.85f))
    }

    /**
     * Picks a representative vivid colour from ARGB pixels: hue buckets weighted by saturation,
     * ignoring near-black, near-white and grey pixels. Returns null if the image is all grey.
     */
    fun dominantVividColor(pixels: IntArray): Long? {
        val buckets = 24
        val weight = FloatArray(buckets)
        val rSum = FloatArray(buckets)
        val gSum = FloatArray(buckets)
        val bSum = FloatArray(buckets)
        for (p in pixels) {
            val c = p.toLong() and 0xFFFFFFFFL
            if (((c shr 24) and 0xFF) < 128) continue
            val hsv = toHsv(c)
            if (hsv[1] < 0.25f || hsv[2] < 0.20f || hsv[2] > 0.98f) continue
            val i = ((hsv[0] / 360f) * buckets).toInt().coerceIn(0, buckets - 1)
            val w = hsv[1] * hsv[2]
            weight[i] += w
            rSum[i] += red(c) * w
            gSum[i] += green(c) * w
            bSum[i] += blue(c) * w
        }
        val best = weight.indices.maxByOrNull { weight[it] } ?: return null
        if (weight[best] <= 0f) return null
        return rgb(
            (rSum[best] / weight[best]).roundToInt(),
            (gSum[best] / weight[best]).roundToInt(),
            (bSum[best] / weight[best]).roundToInt(),
        )
    }
}
