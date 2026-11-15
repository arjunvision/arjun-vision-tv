package com.arjunvision.tvlauncher.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.arjunvision.tvlauncher.domain.models.ParticleStyle
import com.arjunvision.tvlauncher.domain.models.ThemeSpec

/**
 * FUTURE CINEMA OS colour tokens. They are snapshot state, so applying a theme repaints every
 * screen that reads them. Names are kept from v0.1 (Blue = primary accent, Violet = secondary).
 */
object LauncherColors {
    var Background by mutableStateOf(Color(0xFF070910))   // OLED Black
        private set
    var Surface by mutableStateOf(Color(0xFF101522))      // Deep Navy
        private set
    var Blue by mutableStateOf(Color(0xFF00B8FF))         // Electric Blue (primary accent)
        private set
    var Violet by mutableStateOf(Color(0xFF8B5CF6))       // Neon Violet (secondary accent)
        private set
    val TextPrimary = Color(0xFFF8FAFC)                   // Soft White
    val TextSecondary = Color(0xFFA1A1AA)
    /** Frosted glass fill and edge, drawn without real-time blur (cheap on the Mi TV GPU). */
    val GlassFill = Color(0x1FFFFFFF)
    val GlassEdge = Color(0x33FFFFFF)

    internal fun set(background: Color, surface: Color, accent: Color, accent2: Color) {
        Background = background
        Surface = surface
        Blue = accent
        Violet = accent2
    }
}

/** Shape, motion and type tokens that themes can change. */
object LauncherStyle {
    var cardCornerDp by mutableIntStateOf(24)
        private set
    var focusScale by mutableFloatStateOf(0.06f)
        private set
    var titleWeight by mutableIntStateOf(700)
        private set
    var titleLetterSpacing by mutableFloatStateOf(0f)
        private set
    var particles by mutableStateOf(ParticleStyle.STARS)
        private set
    var wallpaperPath by mutableStateOf<String?>(null)
        private set
    var credit by mutableStateOf<String?>(null)
        private set
    var themeName by mutableStateOf("Arjun Vision Classic")
        private set
    /** Lite Mode: no particles, no aurora motion, smaller zoom. Set from Settings. */
    var lite by mutableStateOf(false)

    val titleFontWeight: FontWeight get() = FontWeight(titleWeight.coerceIn(100, 900))

    fun apply(theme: ThemeSpec) {
        LauncherColors.set(
            background = Color(theme.background.toInt()),
            surface = Color(theme.surface.toInt()),
            accent = Color(theme.accent.toInt()),
            accent2 = Color(theme.accent2.toInt()),
        )
        cardCornerDp = theme.cardCornerDp
        focusScale = theme.focusScale
        titleWeight = theme.titleWeight
        titleLetterSpacing = theme.titleLetterSpacing
        particles = theme.particles
        wallpaperPath = theme.wallpaperPath
        credit = theme.credit
        themeName = theme.name
    }
}

@Composable
fun ArjunVisionTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LauncherColors.Blue,
            secondary = LauncherColors.Violet,
            background = LauncherColors.Background,
            surface = LauncherColors.Surface,
            onBackground = LauncherColors.TextPrimary,
            onSurface = LauncherColors.TextPrimary,
            onSurfaceVariant = LauncherColors.TextSecondary,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LauncherColors.Background,
            contentColor = LauncherColors.TextPrimary,
            content = content,
        )
    }
}
