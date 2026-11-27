package com.arjunvision.tvlauncher.data.wallpapers

import android.content.Context
import com.arjunvision.tvlauncher.domain.models.Profile
import org.json.JSONArray

/** One wallpaper the Home screen can show. [source] is "asset:<path>" or an absolute file path. */
data class Wallpaper(
    val id: String,
    val name: String,
    val category: String,
    val source: String,
    val credit: String,
)

/**
 * Wallpapers available offline:
 * - the licensed set bundled in assets/wallpapers (Wikimedia Commons, credits in wallpapers.json);
 * - Sri's own Iron Man art (assets ironman/, only present in his personal build, never in git).
 */
class WallpaperRepository(private val context: Context) {

    val bundled: List<Wallpaper> by lazy { readList("wallpapers/wallpapers.json", "wallpapers/") { o ->
        "Photo: ${o.optString("creator").take(60)} · ${o.optString("source")} · ${o.optString("license")}"
    } }

    val userArt: List<Wallpaper> by lazy { readList("ironman/user_art.json", "") { o -> o.optString("credit") } }

    val all: List<Wallpaper> get() = userArt + bundled

    fun byId(id: String?): Wallpaper? = id?.let { wanted -> all.firstOrNull { it.id == wanted } }

    /**
     * What Home shows for [profile]:
     * null/"auto" → Iron Man hero for that profile if the art is present, else a calm default photo;
     * "theme" → the active theme's downloaded photo ([themeWallpaperPath]) if any;
     * otherwise the chosen wallpaper id.
     */
    fun resolve(choice: String?, profile: Profile, themeWallpaperPath: String?): Wallpaper? {
        if (choice == "theme") {
            return themeWallpaperPath?.let { Wallpaper("theme", "Theme photo", "Theme", it, "") } ?: defaultFor(profile)
        }
        return byId(choice) ?: defaultFor(profile)
    }

    private fun defaultFor(profile: Profile): Wallpaper? {
        val hero = if (profile == Profile.FATHER) "ironman_hero_father" else "ironman_hero_my"
        return byId(hero) ?: byId(if (profile == Profile.FATHER) "blue_mountain_night" else "nebula") ?: bundled.firstOrNull()
    }

    private fun readList(path: String, folder: String, credit: (org.json.JSONObject) -> String): List<Wallpaper> =
        runCatching {
            val arr = JSONArray(context.assets.open(path).bufferedReader().use { it.readText() })
            (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
                Wallpaper(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    category = o.optString("category"),
                    source = "asset:" + folder + o.getString("file"),
                    credit = credit(o),
                )
            }
        }.getOrDefault(emptyList()) // folder missing (e.g. a build without Sri's art) → simply none
}
