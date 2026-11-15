package com.arjunvision.tvlauncher.ui.gaming

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.theme.LauncherColors

/**
 * Opens the HDMI input the PS5 is connected to. It does not launch or control games.
 */
@Composable
fun PlayStationScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val requester = remember { FocusRequester() }
    val inputLabel = state.hdmiLabel(state.settings.playStationInputId)
    val motion = state.settings.animation.factor

    LaunchedEffect(Unit) {
        runCatching { requester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0C0F24), LauncherColors.Background, Color(0xFF1A0E33)),
                ),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 48.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PLAYSTATION 5",
                    color = LauncherColors.Violet,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Play on your console",
                    color = LauncherColors.TextPrimary,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "This opens the HDMI input your PS5 is connected to. Games are played on the console itself. This app does not launch or control games.",
                    color = LauncherColors.TextSecondary,
                    fontSize = 19.sp,
                )
                Spacer(modifier = Modifier.height(32.dp))
                FocusCard(
                    onClick = viewModel::openPlayStation,
                    motion = motion,
                    shape = RoundedCornerShape(50),
                    background = Brush.horizontalGradient(
                        listOf(LauncherColors.Blue, LauncherColors.Violet),
                    ),
                    modifier = Modifier
                        .width(480.dp)
                        .height(88.dp)
                        .focusRequester(requester),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Open PS5 HDMI input",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = inputLabel?.let { "Input: $it" } ?: "Found automatically from the PS5's HDMI-CEC name",
                    color = LauncherColors.TextSecondary,
                    fontSize = 16.sp,
                )
            }

            ControllerArt(modifier = Modifier
                .weight(1f)
                .height(340.dp))
        }
    }
}

/** Controller-inspired artwork drawn with shapes only. It does not use any Sony logo or trademark. */
@Composable
private fun ControllerArt(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val bodyWidth = size.width * 0.92f
        val bodyHeight = size.height * 0.62f
        val left = (size.width - bodyWidth) / 2f
        val top = size.height * 0.18f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LauncherColors.Violet.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.width * 0.55f,
            ),
            radius = size.width * 0.55f,
            center = Offset(size.width / 2f, size.height / 2f),
        )

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF1B2236), Color(0xFF0B0F1C))),
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(bodyHeight * 0.42f),
        )

        // D-pad on the left.
        val dpadX = left + bodyWidth * 0.24f
        val dpadY = top + bodyHeight * 0.5f
        val arm = bodyHeight * 0.13f
        val dpadColor = Color(0xFF2A3350)
        drawRoundRect(
            color = dpadColor,
            topLeft = Offset(dpadX - arm * 1.5f, dpadY - arm * 0.5f),
            size = Size(arm * 3f, arm),
            cornerRadius = CornerRadius(arm * 0.2f),
        )
        drawRoundRect(
            color = dpadColor,
            topLeft = Offset(dpadX - arm * 0.5f, dpadY - arm * 1.5f),
            size = Size(arm, arm * 3f),
            cornerRadius = CornerRadius(arm * 0.2f),
        )

        // Four face buttons on the right.
        val faceX = left + bodyWidth * 0.76f
        val faceY = top + bodyHeight * 0.5f
        val r = bodyHeight * 0.085f
        val gap = bodyHeight * 0.16f
        drawCircle(LauncherColors.Blue.copy(alpha = 0.9f), r, Offset(faceX, faceY - gap))
        drawCircle(LauncherColors.Violet.copy(alpha = 0.9f), r, Offset(faceX + gap, faceY))
        drawCircle(LauncherColors.TextPrimary.copy(alpha = 0.7f), r, Offset(faceX, faceY + gap))
        drawCircle(LauncherColors.TextSecondary.copy(alpha = 0.7f), r, Offset(faceX - gap, faceY))

        // Light bar.
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(LauncherColors.Blue, LauncherColors.Violet)),
            topLeft = Offset(size.width / 2f - bodyWidth * 0.06f, top + bodyHeight * 0.1f),
            size = Size(bodyWidth * 0.12f, bodyHeight * 0.03f),
            cornerRadius = CornerRadius(bodyHeight * 0.015f),
        )
    }
}
