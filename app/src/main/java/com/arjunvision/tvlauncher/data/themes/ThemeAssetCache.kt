package com.arjunvision.tvlauncher.data.themes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.view.Display
import com.arjunvision.tvlauncher.domain.models.InspirationItem
import com.arjunvision.tvlauncher.domain.themes.ColorMath
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads approved wallpaper images once, resizes them for the TV panel (1080p or 4K),
 * de-duplicates by URL hash and deletes files nothing refers to any more.
 */
class ThemeAssetCache(private val context: Context) {

    private val dir = File(context.filesDir, "theme-assets").apply { mkdirs() }

    /** 3840 on a 4K panel, otherwise 1920. Read from the display mode, not guessed. */
    fun targetWidth(): Int {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val physical = display?.mode?.physicalWidth ?: 1920
        return if (physical >= 3840) 3840 else 1920
    }

    fun fileFor(url: String) = File(dir, sha(url).take(32) + ".jpg")

    /** Returns the cached file, downloading it if needed, or null for broken URLs / unreadable images. */
    suspend fun download(item: InspirationItem): File? = withContext(Dispatchers.IO) {
        if (!item.downloadAllowed || !item.imageUrl.startsWith("https://")) return@withContext null
        val target = fileFor(item.imageUrl)
        if (target.exists() && target.length() > 0) return@withContext target // no duplicate downloads
        val tmp = File(dir, target.name + ".part")
        runCatching {
            val connection = (URL(item.imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", HttpJson.USER_AGENT)
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                if (connection.contentLengthLong > MAX_BYTES) return@runCatching null
                connection.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            } finally {
                connection.disconnect()
            }
            val bitmap = decodeScaled(tmp, targetWidth()) ?: return@runCatching null
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bitmap.recycle()
            target
        }.getOrNull().also { tmp.delete() }
    }

    /** Representative vivid colour of an image, used to tint a generated theme. */
    suspend fun dominantColor(file: File): Long? = withContext(Dispatchers.IO) {
        val small = decodeScaled(file, 64) ?: return@withContext null
        val pixels = IntArray(small.width * small.height)
        small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
        small.recycle()
        ColorMath.dominantVividColor(pixels)
    }

    /** Deletes cached files not in [keepPaths]. Never touches anything outside theme-assets/. */
    fun cleanup(keepPaths: Set<String>): Int {
        var removed = 0
        dir.listFiles()?.forEach { f ->
            if (f.absolutePath !in keepPaths && f.delete()) removed++
        }
        return removed
    }

    private fun decodeScaled(file: File, maxWidth: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        if (decoded.width <= maxWidth) return decoded
        val h = (decoded.height * (maxWidth.toFloat() / decoded.width)).toInt()
        return Bitmap.createScaledBitmap(decoded, maxWidth, h, true).also { if (it != decoded) decoded.recycle() }
    }

    companion object {
        private const val MAX_BYTES = 25L * 1024 * 1024
    }
}
