package com.arjunvision.tvlauncher.ui.startup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.ui.components.LogoMark
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The launcher's own opening sequence (about 2 seconds). It does not replace the TV's boot animation.
 * [motion] is the animation factor from Settings; 0 keeps the logo still.
 */
@Composable
fun StartupScreen(motion: Float, onFinished: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(if (motion > 0f) 0.82f else 1f) }
    val taglineAlpha = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val finishedCallback by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        launch { glow.animateTo(1f, tween(durationMillis = 1200)) }
        launch { logoAlpha.animateTo(1f, tween(durationMillis = 800)) }
        launch { logoScale.animateTo(1f, tween(durationMillis = 1000, easing = FastOutSlowInEasing)) }
        delay(900)
        taglineAlpha.animateTo(1f, tween(durationMillis = 500))
        delay(1100)
        finishedCallback()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LauncherColors.Blue.copy(alpha = 0.30f * glow.value),
                        Color.Transparent,
                    ),
                    center = centre,
                    radius = size.minDimension * 0.55f,
                ),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(
                size = 200.dp,
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                },
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "ARJUN VISION TECH SOLUTIONS",
                color = LauncherColors.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = logoAlpha.value },
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "YOUR SMART TV EXPERIENCE",
                color = LauncherColors.Blue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value },
            )
        }
    }
}
