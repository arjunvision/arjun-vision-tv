package com.arjunvision.tvlauncher.domain.themes

import com.arjunvision.tvlauncher.domain.models.InspirationItem
import com.arjunvision.tvlauncher.domain.models.ParticleStyle
import com.arjunvision.tvlauncher.domain.models.ThemeCategory
import com.arjunvision.tvlauncher.domain.models.ThemeSpec
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds original ARJUN VISION themes. Every category has a hand-made base design; online
 * inspiration only contributes a wallpaper photo and a colour tint, never another site's layout.
 */
object ThemeFactory {

    const val CLASSIC_ID = "builtin-CLASSIC"

    private data class Base(
        val background: Long,
        val surface: Long,
        val accent: Long,
        val accent2: Long,
        val corner: Int,
        val focus: Float,
        val weight: Int,
        val spacing: Float,
        val particles: ParticleStyle,
    )

    private fun base(category: ThemeCategory): Base = when (category) {
        ThemeCategory.CLASSIC -> Base(0xFF070910, 0xFF101522, 0xFF00B8FF, 0xFF8B5CF6, 24, 0.06f, 700, 0f, ParticleStyle.STARS)
        ThemeCategory.CHRISTMAS -> Base(0xFF07120D, 0xFF0F1F17, 0xFFE11D48, 0xFF22C55E, 28, 0.06f, 700, 0.5f, ParticleStyle.SNOW)
        ThemeCategory.NEW_YEAR -> Base(0xFF08080F, 0xFF14141F, 0xFFFACC15, 0xFFA78BFA, 24, 0.07f, 800, 1f, ParticleStyle.SPARKS)
        ThemeCategory.PONGAL -> Base(0xFF120B04, 0xFF20150A, 0xFFF59E0B, 0xFF84CC16, 30, 0.06f, 800, 0.5f, ParticleStyle.SPARKS)
        ThemeCategory.TAMIL_NEW_YEAR -> Base(0xFF120709, 0xFF1F0F13, 0xFFF97316, 0xFFFDE68A, 30, 0.06f, 700, 0.5f, ParticleStyle.PETALS)
        ThemeCategory.DIWALI -> Base(0xFF0E0704, 0xFF1C1009, 0xFFF59E0B, 0xFFEC4899, 26, 0.07f, 800, 0.5f, ParticleStyle.SPARKS)
        ThemeCategory.INDEPENDENCE_DAY -> Base(0xFF060A12, 0xFF0E1522, 0xFFFF9933, 0xFF22A447, 24, 0.06f, 800, 1f, ParticleStyle.STARS)
        ThemeCategory.SUMMER -> Base(0xFF0C0907, 0xFF1A1410, 0xFFFB923C, 0xFF38BDF8, 32, 0.06f, 700, 0f, ParticleStyle.NONE)
        ThemeCategory.MONSOON -> Base(0xFF050B12, 0xFF0C1824, 0xFF38BDF8, 0xFF94A3B8, 28, 0.05f, 600, 0.5f, ParticleStyle.RAIN)
        ThemeCategory.GAMING -> Base(0xFF04060A, 0xFF0C1118, 0xFF22D3EE, 0xFFA3E635, 18, 0.08f, 900, 1.5f, ParticleStyle.STARS)
        ThemeCategory.PLAYSTATION -> Base(0xFF020617, 0xFF0B1630, 0xFF2563EB, 0xFF60A5FA, 22, 0.08f, 800, 1f, ParticleStyle.STARS)
        ThemeCategory.VICE_CITY -> Base(0xFF12051C, 0xFF200B30, 0xFFF472B6, 0xFF22D3EE, 20, 0.08f, 900, 1.5f, ParticleStyle.SPARKS)
        ThemeCategory.AI_FUTURE -> Base(0xFF05080F, 0xFF0D1424, 0xFF00E5FF, 0xFF7C3AED, 16, 0.07f, 600, 2f, ParticleStyle.STARS)
        ThemeCategory.CINEMA -> Base(0xFF080808, 0xFF151515, 0xFFEAB308, 0xFFDC2626, 14, 0.05f, 300, 3f, ParticleStyle.NONE)
        ThemeCategory.SPECIAL_EVENT -> Base(0xFF0A0705, 0xFF17110C, 0xFFF57C00, 0xFFFF914C, 26, 0.07f, 800, 1f, ParticleStyle.SPARKS)
    }

    fun builtIn(category: ThemeCategory): ThemeSpec {
        val b = base(category)
        return ThemeSpec(
            id = "builtin-${category.name}",
            name = category.title,
            category = category,
            background = b.background,
            surface = b.surface,
            accent = b.accent,
            accent2 = b.accent2,
            cardCornerDp = b.corner,
            focusScale = b.focus,
            titleWeight = b.weight,
            titleLetterSpacing = b.spacing,
            particles = b.particles,
        )
    }

    fun builtIns(): List<ThemeSpec> = ThemeCategory.entries.map(::builtIn)

    /**
     * Creates a new original theme for [category]. [dominantColor] comes from the inspiration
     * photo and tints the secondary accent and surfaces; the primary accent stays the category's
     * own, so every theme still reads as ARJUN VISION.
     */
    fun generate(
        category: ThemeCategory,
        inspiration: InspirationItem?,
        wallpaperPath: String?,
        dominantColor: Long?,
        nowMillis: Long,
    ): ThemeSpec {
        val b = base(category)
        val tint = dominantColor?.let(ColorMath::asAccent)
        val month = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
            .format(Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()))
        val variant = ((inspiration?.id?.hashCode() ?: nowMillis.toInt()) and 0x7FFFFFFF) % 3
        return ThemeSpec(
            id = "gen-${category.name}-${inspiration?.id?.take(12) ?: nowMillis.toString()}",
            name = "${category.title} · $month",
            category = category,
            background = tint?.let { ColorMath.blend(b.background, it, 0.06f) } ?: b.background,
            surface = tint?.let { ColorMath.blend(b.surface, it, 0.14f) } ?: b.surface,
            accent = b.accent,
            accent2 = tint?.let { ColorMath.blend(b.accent2, it, 0.6f) } ?: b.accent2,
            cardCornerDp = (b.corner + listOf(0, 6, -4)[variant]).coerceIn(10, 36),
            focusScale = b.focus,
            titleWeight = b.weight,
            titleLetterSpacing = b.spacing,
            particles = b.particles,
            wallpaperPath = wallpaperPath,
            inspirationId = inspiration?.id,
            credit = inspiration?.credit,
            generated = true,
            createdAt = nowMillis,
        )
    }
}
