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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.components.AuroraBackground
import com.arjunvision.tvlauncher.ui.components.LogoMark
import com.arjunvision.tvlauncher.ui.components.ParticleLayer
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Ambient clock screensaver inside the launcher. Every 60 s the clock fades out, moves to a new
 * random spot and fades back in, so nothing burns into the panel. Any remote key closes it.
 * Android's own system screensaver still runs on its own schedule; this one never replaces it.
 */
@Composable
fun ScreensaverScreen(state: LauncherUiState) {
    val settings = state.settings
    val animate = !settings.liteMode && settings.animation.factor > 0f
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1_000L)
        }
    }
    val time = remember(settings.clock24h) { SimpleDateFormat(if (settings.clock24h) "HH:mm" else "h:mm", Locale.getDefault()) }
    val date = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val alpha = remember { Animatable(0f) }
    var spot by remember { mutableStateOf(0.5f to 0.5f) }

    LaunchedEffect(Unit) {
        while (true) {
            alpha.animateTo(1f, tween(1200))
            delay(60_000)
            alpha.animateTo(0f, tween(900))
            spot = Random.nextFloat() * 0.8f + 0.1f to Random.nextFloat() * 0.8f + 0.1f
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(LauncherColors.Background)) {
        AuroraBackground(animate = animate)
        ParticleLayer(style = LauncherStyle.particles, animate = animate)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val blockW = 420.dp
            val blockH = 170.dp
            val x = (maxWidth - blockW) * spot.first
            val y = (maxHeight - blockH) * spot.second
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .width(blockW)
                    .graphicsLayer { this.alpha = alpha.value },
            ) {
                Text(time.format(now), color = LauncherColors.TextPrimary, fontSize = 96.sp, fontWeight = FontWeight.Thin)
                Text(date.format(now), color = LauncherColors.TextSecondary, fontSize = 20.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMark(size = 26.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("ARJUN VISION", color = LauncherColors.TextSecondary, fontSize = 13.sp, letterSpacing = 3.sp)
                }
            }
        }
    }
}
