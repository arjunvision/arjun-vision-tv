package com.arjunvision.tvlauncher.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.Profile
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.theme.LauncherColors

@Composable
fun ProfileScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val active = state.settings.profile
    val activeRequester = remember { FocusRequester() }
    val motion = state.settings.animation.factor

    LaunchedEffect(Unit) {
        runCatching { activeRequester.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LauncherColors.Background, LauncherColors.Surface)))
            .padding(horizontal = 48.dp, vertical = 48.dp),
    ) {
        Text(
            text = "Choose a profile",
            color = LauncherColors.TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your choice changes the home screen and is remembered on this TV.",
            color = LauncherColors.TextSecondary,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Profile.entries.forEach { profile ->
                val isActive = profile == active
                FocusCard(
                    onClick = {
                        viewModel.setProfile(profile)
                        viewModel.goHome()
                    },
                    motion = motion,
                    shape = RoundedCornerShape(28.dp),
                    background = Brush.linearGradient(
                        listOf(
                            (if (profile == Profile.FATHER) LauncherColors.Blue else LauncherColors.Violet)
                                .copy(alpha = 0.35f),
                            LauncherColors.Surface,
                        ),
                    ),
                    modifier = Modifier
                        .width(460.dp)
                        .height(260.dp)
                        .then(if (isActive) Modifier.focusRequester(activeRequester) else Modifier),
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                    ) {
                        Column {
                            Text(
                                text = profile.title,
                                color = LauncherColors.TextPrimary,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = profile.subtitle,
                                color = LauncherColors.TextSecondary,
                                fontSize = 18.sp,
                            )
                        }
                        Text(
                            text = if (isActive) "ACTIVE PROFILE" else "Switch to this profile",
                            color = if (isActive) LauncherColors.Blue else LauncherColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
