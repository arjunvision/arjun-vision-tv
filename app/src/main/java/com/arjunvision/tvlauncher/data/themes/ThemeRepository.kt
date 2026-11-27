package com.arjunvision.tvlauncher.data.themes

import android.content.Context
import com.arjunvision.tvlauncher.domain.models.InspirationItem
import com.arjunvision.tvlauncher.domain.models.ParticleStyle
import com.arjunvision.tvlauncher.domain.models.ThemeCategory
import com.arjunvision.tvlauncher.domain.models.ThemeSpec
import com.arjunvision.tvlauncher.domain.models.ThemeState
import com.arjunvision.tvlauncher.domain.themes.ThemeFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores themes, inspiration records (with licences) and scheduler flags in
 * files/themes/state.json. One shared instance for the UI and the background worker.
 */
class ThemeRepository private constructor(context: Context) {

    private val file = File(context.filesDir, "themes/state.json").apply { parentFile?.mkdirs() }
    private val mutex = Mutex()
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ThemeState> = _state.asStateFlow()

    /** Built-in themes plus generated ones. */
    fun allThemes(s: ThemeState = _state.value): List<ThemeSpec> = ThemeFactory.builtIns() + s.savedThemes

    fun activeTheme(s: ThemeState = _state.value): ThemeSpec =
        allThemes(s).firstOrNull { it.id == s.activeThemeId } ?: ThemeFactory.builtIn(ThemeCategory.IRON_MAN)

    suspend fun update(transform: (ThemeState) -> ThemeState): ThemeState = mutex.withLock {
        val next = transform(_state.value)
        withContext(Dispatchers.IO) {
            val tmp = File(file.parentFile, "state.json.tmp")
            tmp.writeText(toJson(next).toString())
            tmp.renameTo(file) // atomic replace: a crash never leaves half a file
        }
        _state.value = next
        next
    }

    private fun load(): ThemeState = runCatching {
        if (!file.exists()) return ThemeState()
        val json = JSONObject(file.readText())
        val saved = fromJson(json)
        // Migration (2026-11): older installs saved "Arjun Vision Classic" as the active theme before
        // Iron Man existed. Move them to Iron Man once, unless Sri locked a theme on purpose.
        if (json.optInt("version", 1) < 2) {
            val migrated =
                if (saved.activeThemeId == ThemeFactory.CLASSIC_ID && !saved.locked) {
                    saved.copy(activeThemeId = ThemeFactory.IRON_MAN_ID)
                } else {
                    saved
                }
            runCatching { file.writeText(toJson(migrated).toString()) }
            return migrated
        }
        saved
    }.getOrDefault(ThemeState())

    // ---- JSON ----

    private fun toJson(s: ThemeState) = JSONObject().apply {
        put("version", 2)
        put("activeThemeId", s.activeThemeId)
        put("locked", s.locked)
        put("autoEnabled", s.autoEnabled)
        put("lastDiscoveryMillis", s.lastDiscoveryMillis)
        put("lastDiscoveryResult", s.lastDiscoveryResult ?: JSONObject.NULL)
        put("themes", JSONArray().apply { s.savedThemes.forEach { put(themeJson(it)) } })
        put("inspirations", JSONArray().apply { s.inspirations.forEach { put(inspirationJson(it)) } })
    }

    private fun fromJson(o: JSONObject) = ThemeState(
        activeThemeId = o.optString("activeThemeId", ThemeFactory.CLASSIC_ID),
        locked = o.optBoolean("locked"),
        autoEnabled = o.optBoolean("autoEnabled", true),
        lastDiscoveryMillis = o.optLong("lastDiscoveryMillis"),
        lastDiscoveryResult = o.optStringOrNull("lastDiscoveryResult"),
        savedThemes = o.optJSONArray("themes").objects().mapNotNull { runCatching { themeFrom(it) }.getOrNull() },
        inspirations = o.optJSONArray("inspirations").objects().mapNotNull { runCatching { inspirationFrom(it) }.getOrNull() },
    )

    private fun themeJson(t: ThemeSpec) = JSONObject().apply {
        put("id", t.id); put("name", t.name); put("category", t.category.name)
        put("background", t.background); put("surface", t.surface); put("accent", t.accent); put("accent2", t.accent2)
        put("cardCornerDp", t.cardCornerDp); put("focusScale", t.focusScale.toDouble())
        put("titleWeight", t.titleWeight); put("titleLetterSpacing", t.titleLetterSpacing.toDouble())
        put("particles", t.particles.name)
        put("wallpaperPath", t.wallpaperPath ?: JSONObject.NULL)
        put("inspirationId", t.inspirationId ?: JSONObject.NULL)
        put("credit", t.credit ?: JSONObject.NULL)
        put("generated", t.generated); put("saved", t.saved); put("createdAt", t.createdAt)
    }

    private fun themeFrom(o: JSONObject) = ThemeSpec(
        id = o.getString("id"),
        name = o.getString("name"),
        category = ThemeCategory.valueOf(o.getString("category")),
        background = o.getLong("background"),
        surface = o.getLong("surface"),
        accent = o.getLong("accent"),
        accent2 = o.getLong("accent2"),
        cardCornerDp = o.optInt("cardCornerDp", 24),
        focusScale = o.optDouble("focusScale", 0.06).toFloat(),
        titleWeight = o.optInt("titleWeight", 700),
        titleLetterSpacing = o.optDouble("titleLetterSpacing", 0.0).toFloat(),
        particles = runCatching { ParticleStyle.valueOf(o.getString("particles")) }.getOrDefault(ParticleStyle.NONE),
        wallpaperPath = o.optStringOrNull("wallpaperPath"),
        inspirationId = o.optStringOrNull("inspirationId"),
        credit = o.optStringOrNull("credit"),
        generated = o.optBoolean("generated"),
        saved = o.optBoolean("saved"),
        createdAt = o.optLong("createdAt"),
    )

    private fun inspirationJson(i: InspirationItem) = JSONObject().apply {
        put("id", i.id); put("category", i.category.name); put("title", i.title)
        put("imageUrl", i.imageUrl); put("pageUrl", i.pageUrl); put("creator", i.creator)
        put("source", i.source); put("license", i.license); put("licenseUrl", i.licenseUrl ?: JSONObject.NULL)
        put("downloadAllowed", i.downloadAllowed); put("discoveredAt", i.discoveredAt)
        put("localPath", i.localPath ?: JSONObject.NULL); put("failed", i.failed)
    }

    private fun inspirationFrom(o: JSONObject) = InspirationItem(
        id = o.getString("id"),
        category = ThemeCategory.valueOf(o.getString("category")),
        title = o.optString("title"),
        imageUrl = o.getString("imageUrl"),
        pageUrl = o.optString("pageUrl"),
        creator = o.optString("creator"),
        source = o.optString("source"),
        license = o.optString("license"),
        licenseUrl = o.optStringOrNull("licenseUrl"),
        downloadAllowed = o.optBoolean("downloadAllowed"),
        discoveredAt = o.optLong("discoveredAt"),
        localPath = o.optStringOrNull("localPath"),
        failed = o.optBoolean("failed"),
    )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key).ifBlank { null }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    companion object {
        @Volatile private var instance: ThemeRepository? = null

        fun get(context: Context): ThemeRepository =
            instance ?: synchronized(this) {
                instance ?: ThemeRepository(context.applicationContext).also { instance = it }
            }
    }
}
