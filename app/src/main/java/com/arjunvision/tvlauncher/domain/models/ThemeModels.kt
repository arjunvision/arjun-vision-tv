package com.arjunvision.tvlauncher.domain.models

import java.time.LocalDate

enum class ThemeGroup(val title: String) {
    FESTIVAL("Festival themes"),
    SEASONAL("Seasonal themes"),
    GAMING("Gaming themes"),
    CINEMATIC("Cinematic themes"),
    FUTURISTIC("Futuristic themes"),
    SPECIAL("Special events"),
}

enum class ParticleStyle { NONE, STARS, SNOW, SPARKS, RAIN, PETALS }

/**
 * [inspirationQuery] is a generic search phrase sent to licensed image APIs. It never names
 * a copyrighted game, film or brand, so results are photos, not commercial artwork.
 */
enum class ThemeCategory(val title: String, val group: ThemeGroup, val inspirationQuery: String) {
    CLASSIC("Arjun Vision Classic", ThemeGroup.FUTURISTIC, "abstract blue light"),
    /** Deep black, metallic gold, crimson; hero artwork = Sri's own Iron Man images (personal use). */
    IRON_MAN("Iron Man · Cinematic Dark", ThemeGroup.CINEMATIC, "red gold metallic abstract"),
    CHRISTMAS("Christmas", ThemeGroup.FESTIVAL, "christmas lights night"),
    NEW_YEAR("New Year", ThemeGroup.FESTIVAL, "fireworks night sky"),
    PONGAL("Pongal", ThemeGroup.FESTIVAL, "pongal festival"),
    TAMIL_NEW_YEAR("Tamil New Year", ThemeGroup.FESTIVAL, "kolam rangoli"),
    DIWALI("Diwali", ThemeGroup.FESTIVAL, "diwali diya lamps"),
    INDEPENDENCE_DAY("Independence Day", ThemeGroup.FESTIVAL, "india flag tricolour"),
    SUMMER("Summer", ThemeGroup.SEASONAL, "beach sunset"),
    MONSOON("Monsoon", ThemeGroup.SEASONAL, "monsoon rain clouds"),
    GAMING("Gaming", ThemeGroup.GAMING, "neon arcade"),
    PLAYSTATION("PlayStation Night", ThemeGroup.GAMING, "game controller blue light"),
    VICE_CITY("Neon Coast (Vice City-inspired)", ThemeGroup.GAMING, "neon sunset palm trees"),
    AI_FUTURE("AI & Future Tech", ThemeGroup.FUTURISTIC, "circuit board macro"),
    CINEMA("Cinema", ThemeGroup.CINEMATIC, "cinema theatre seats"),
    SPECIAL_EVENT("Special Event", ThemeGroup.SPECIAL, "stage lights concert"),
}

/** A complete launcher theme. Colours are ARGB values (0xAARRGGBB). */
data class ThemeSpec(
    val id: String,
    val name: String,
    val category: ThemeCategory,
    val background: Long,
    val surface: Long,
    val accent: Long,
    val accent2: Long,
    val cardCornerDp: Int,
    /** Extra zoom on focus, e.g. 0.06 = 6 %. */
    val focusScale: Float,
    val titleWeight: Int,
    val titleLetterSpacing: Float,
    val particles: ParticleStyle,
    val wallpaperPath: String? = null,
    val inspirationId: String? = null,
    /** Attribution shown on screen when the wallpaper came from a licensed photo. */
    val credit: String? = null,
    /** True for themes created from online inspiration; false for the built-in set. */
    val generated: Boolean = false,
    /** User pressed Save; protects it from automatic clean-up. */
    val saved: Boolean = false,
    val createdAt: Long = 0L,
)

/** One discovered image with everything needed to respect its licence. */
data class InspirationItem(
    val id: String,
    val category: ThemeCategory,
    val title: String,
    val imageUrl: String,
    val pageUrl: String,
    val creator: String,
    val source: String,
    val license: String,
    val licenseUrl: String?,
    val downloadAllowed: Boolean,
    val discoveredAt: Long,
    val localPath: String? = null,
    val failed: Boolean = false,
) {
    val credit: String get() = "Photo: $creator · $source · $license"
}

data class Festival(
    val name: String,
    val category: ThemeCategory,
    val date: LocalDate,
    /** First and last day the festival theme is shown. */
    val start: LocalDate,
    val end: LocalDate,
    /** True when the date moves each year and must be checked against an official calendar. */
    val dateNeedsCheck: Boolean,
)

/** Saved theme-engine state (themes/state.json). */
data class ThemeState(
    val activeThemeId: String = "builtin-IRON_MAN",
    val locked: Boolean = false,
    val autoEnabled: Boolean = true,
    val savedThemes: List<ThemeSpec> = emptyList(),
    val inspirations: List<InspirationItem> = emptyList(),
    val lastDiscoveryMillis: Long = 0L,
    val lastDiscoveryResult: String? = null,
)
