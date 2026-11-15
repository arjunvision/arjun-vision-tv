package com.arjunvision.tvlauncher.data.themes

import com.arjunvision.tvlauncher.domain.models.InspirationItem
import com.arjunvision.tvlauncher.domain.models.ThemeCategory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Minimal HTTPS JSON client. Plain HTTP is refused. No cookies, no tracking headers. */
object HttpJson {
    const val USER_AGENT = "ArjunVisionTV/0.2 (Android TV launcher; personal use)"

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): JSONObject = withContext(Dispatchers.IO) {
        require(url.startsWith("https://")) { "Only HTTPS is allowed" }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            val code = connection.responseCode
            if (code == 429) throw RateLimitedException()
            if (code !in 200..299) throw java.io.IOException("HTTP $code")
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /** True if the service answers at all (checks "online service availability", not just Wi-Fi). */
    suspend fun isReachable(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try { c.responseCode in 200..399 } finally { c.disconnect() }
        }.getOrDefault(false)
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}

class RateLimitedException : java.io.IOException("Rate limited by the image service")

fun sha(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

/** A licensed image source reached through its official API. */
interface InspirationSource {
    val name: String
    suspend fun search(category: ThemeCategory, limit: Int, targetWidth: Int): List<InspirationItem>
}

/**
 * Wikimedia Commons (official MediaWiki API, no key needed). Only freely licensed files are kept:
 * CC0, public domain, CC BY and CC BY-SA. Attribution is stored and shown on screen.
 */
class WikimediaCommonsSource : InspirationSource {
    override val name = "Wikimedia Commons"

    override suspend fun search(category: ThemeCategory, limit: Int, targetWidth: Int): List<InspirationItem> {
        val query = HttpJson.enc("${category.inspirationQuery} filetype:bitmap")
        val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2" +
            "&generator=search&gsrnamespace=6&gsrlimit=${limit * 3}&gsrsearch=$query" +
            "&prop=imageinfo&iiprop=url|size|extmetadata&iiurlwidth=$targetWidth"
        val pages = HttpJson.get(url).optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        val now = System.currentTimeMillis()
        val out = mutableListOf<InspirationItem>()
        for (i in 0 until pages.length()) {
            val page = pages.getJSONObject(i)
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val width = info.optInt("width")
            val height = info.optInt("height")
            if (width < 1600 || width < height * 1.3) continue // landscape TV wallpapers only
            val meta = info.optJSONObject("extmetadata") ?: continue
            val license = meta.optJSONObject("LicenseShortName")?.optString("value").orEmpty()
            if (!isFreeLicense(license)) continue
            val image = info.optString("thumburl").ifBlank { info.optString("url") }
            if (!image.startsWith("https://")) continue
            out += InspirationItem(
                id = sha(image).take(24),
                category = category,
                title = page.optString("title").removePrefix("File:").substringBeforeLast('.'),
                imageUrl = image,
                pageUrl = info.optString("descriptionurl"),
                creator = stripHtml(meta.optJSONObject("Artist")?.optString("value")).ifBlank { "Unknown author" },
                source = name,
                license = license,
                licenseUrl = meta.optJSONObject("LicenseUrl")?.optString("value"),
                downloadAllowed = true,
                discoveredAt = now,
            )
            if (out.size >= limit) break
        }
        return out
    }

    private fun isFreeLicense(license: String): Boolean {
        val l = license.lowercase()
        if ("nc" in l.split('-', ' ') || "fair use" in l) return false
        return l.startsWith("cc0") || l.contains("public domain") || l.startsWith("cc by") || l == "pd"
    }

    private fun stripHtml(html: String?): String =
        html.orEmpty().replace(Regex("<[^>]*>"), "").replace("&amp;", "&").replace(Regex("\\s+"), " ").trim().take(80)
}

/** Pexels official API. Needs PEXELS_API_KEY in local.properties; disabled without it. */
class PexelsSource(private val apiKey: String) : InspirationSource {
    override val name = "Pexels"

    override suspend fun search(category: ThemeCategory, limit: Int, targetWidth: Int): List<InspirationItem> {
        val url = "https://api.pexels.com/v1/search?orientation=landscape&size=large&per_page=$limit&query=" +
            HttpJson.enc(category.inspirationQuery)
        val photos = HttpJson.get(url, mapOf("Authorization" to apiKey)).optJSONArray("photos") ?: return emptyList()
        val now = System.currentTimeMillis()
        return (0 until photos.length()).map { photos.getJSONObject(it) }.mapNotNull { p ->
            val src = p.optJSONObject("src") ?: return@mapNotNull null
            val image = if (targetWidth > 1920) src.optString("original") else src.optString("large2x")
            if (!image.startsWith("https://")) return@mapNotNull null
            InspirationItem(
                id = sha(image).take(24),
                category = category,
                title = p.optString("alt").ifBlank { category.title },
                imageUrl = image,
                pageUrl = p.optString("url"),
                creator = p.optString("photographer").ifBlank { "Pexels photographer" },
                source = name,
                license = "Pexels License",
                licenseUrl = "https://www.pexels.com/license/",
                downloadAllowed = true,
                discoveredAt = now,
            )
        }
    }
}

/** Pixabay official API. Needs PIXABAY_API_KEY in local.properties; images are cached, never hotlinked. */
class PixabaySource(private val apiKey: String) : InspirationSource {
    override val name = "Pixabay"

    override suspend fun search(category: ThemeCategory, limit: Int, targetWidth: Int): List<InspirationItem> {
        val url = "https://pixabay.com/api/?image_type=photo&orientation=horizontal&safesearch=true&min_width=1280" +
            "&per_page=${limit.coerceAtLeast(3)}&key=${HttpJson.enc(apiKey)}&q=${HttpJson.enc(category.inspirationQuery)}"
        val hits = HttpJson.get(url).optJSONArray("hits") ?: return emptyList()
        val now = System.currentTimeMillis()
        return (0 until hits.length()).map { hits.getJSONObject(it) }.mapNotNull { h ->
            val image = h.optString("largeImageURL")
            if (!image.startsWith("https://")) return@mapNotNull null
            InspirationItem(
                id = sha(image).take(24),
                category = category,
                title = h.optString("tags").ifBlank { category.title },
                imageUrl = image,
                pageUrl = h.optString("pageURL"),
                creator = h.optString("user").ifBlank { "Pixabay contributor" },
                source = name,
                license = "Pixabay Content License",
                licenseUrl = "https://pixabay.com/service/license-summary/",
                downloadAllowed = true,
                discoveredAt = now,
            )
        }.take(limit)
    }
}
