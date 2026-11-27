package com.arjunvision.tvlauncher.ui.screensaver

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.AppSettings
import com.arjunvision.tvlauncher.domain.models.ParticleStyle
import com.arjunvision.tvlauncher.domain.models.ScreensaverStyle
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.CinematicBackdrop
import com.arjunvision.tvlauncher.ui.components.LogoMark
import com.arjunvision.tvlauncher.ui.components.ParticleLayer
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay

/** In-launcher screensaver (after N idle minutes on Home). Same look as Android's screensaver. */
@Composable
fun ScreensaverScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val settings = state.settings
    ScreensaverContent(
        settings = settings,
        slides = remember { viewModel.wallpapers.slideshow.map { it.source } },
        homeWallpaper = viewModel.wallpapers.resolve(settings.wallpaperId, settings.profile, LauncherStyle.wallpaperPath)?.source,
    )
}

/**
 * ARJUN VISION ambient screensaver (CINEMATIC OS).
 * - [ScreensaverStyle.SLIDESHOW]: Sri's Iron Man art + the licensed photos, a new one every minute.
 * - [ScreensaverStyle.HOME_WALLPAPER]: the Home wallpaper.
 * - [ScreensaverStyle.DARK]: dark aurora (the original look).
 * Every minute everything fades through black and the clock moves, so nothing burns into the panel.
 * The background is ONE baked opaque layer ([CinematicBackdrop]); the black veil is only drawn while fading.
 */
@Composable
fun ScreensaverContent(settings: AppSettings, slides: List<String>, homeWallpaper: String?) {
    val animate = !settings.liteMode && settings.animation.factor > 0f
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1_000L)
        }
    }
    val time = remember(settings.clock24h) { SimpleDateFormat(if (settings.clock24h) "HH:mm" else "h:mm", Locale.getDefault()) }
    val amPm = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val date = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    val veil = remember { Animatable(1f) } // 1 = black, 0 = fully visible
    var slide by remember { mutableIntStateOf(if (slides.isEmpty()) 0 else Random.nextInt(slides.size)) }
    var spot by remember { mutableStateOf(ScreensaverMath.nextSpot(Random)) }

    LaunchedEffect(settings.screensaverStyle, slides.size) {
        while (true) {
            veil.animateTo(0f, tween(1_600))
            delay(60_000)
            veil.animateTo(1f, tween(1_000))
            if (settings.screensaverStyle == ScreensaverStyle.SLIDESHOW) slide = ScreensaverMath.nextIndex(slide, slides.size)
            spot = ScreensaverMath.nextSpot(Random)
        }
    }

    val wallpaper = when (settings.screensaverStyle) {
        ScreensaverStyle.SLIDESHOW -> slides.getOrNull(slide) ?: homeWallpaper
        ScreensaverStyle.HOME_WALLPAPER -> homeWallpaper
        ScreensaverStyle.DARK -> null
    }
    val textShadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 3f), blurRadius = 18f)

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackdrop(
            wallpaperPath = wallpaper,
            animate = animate && wallpaper == null,
            aurora = wallpaper == null,
        )
        if (animate) ParticleLayer(style = ParticleStyle.STARS, animate = true)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val blockW = 520.dp
            val blockH = 230.dp
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth - blockW) * spot.first, y = (maxHeight - blockH) * spot.second)
                    .size(blockW, blockH)
                    // Soft dark pool behind the clock so it reads on any photo (small area, cheap).
                    .background(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            time.format(now),
                            style = TextStyle(color = Color.White, fontSize = 112.sp, fontWeight = FontWeight.Thin, shadow = textShadow),
                        )
                        if (!settings.clock24h) {
                            Text(
                                amPm.format(now).lowercase(Locale.getDefault()),
                                style = TextStyle(color = LauncherColors.Blue, fontSize = 26.sp, fontWeight = FontWeight.Light, shadow = textShadow),
                                modifier = Modifier.offset(x = 6.dp, y = (-22).dp),
                            )
                        }
                    }
                    Text(
                        date.format(now),
                        style = TextStyle(color = Color(0xFFE4E4E7), fontSize = 24.sp, shadow = textShadow),
                    )
                    if (settings.screensaverLogo) {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LogoMark(size = 28.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "ARJUN VISION",
                                style = TextStyle(color = LauncherColors.Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp, shadow = textShadow),
                            )
                        }
                    }
                }
            }
        }
        if (veil.value > 0.01f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = veil.value }.background(Color.Black))
        }
    }
}

/** Pure helpers (unit-tested in ScreensaverMathTest). */
object ScreensaverMath {
    fun nextIndex(current: Int, size: Int): Int = if (size <= 0) 0 else (current + 1).mod(size)

    /** Where the clock block goes (fractions of the free space), kept away from the very edges (overscan). */
    fun nextSpot(random: Random): Pair<Float, Float> =
        (0.06f + random.nextFloat() * 0.88f) to (0.08f + random.nextFloat() * 0.84f)
}
