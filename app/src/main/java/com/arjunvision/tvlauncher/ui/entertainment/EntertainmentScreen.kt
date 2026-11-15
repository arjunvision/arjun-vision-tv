package com.arjunvision.tvlauncher.ui.entertainment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.KnownEntertainmentApp
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.CinematicBackdrop
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.theme.LauncherColors

private data class Section(val title: String, val subtitle: String, val query: String, val glyph: String, val accent: Color)

/**
 * Entertainment Centre (Phase 3 shell). Each section opens a YouTube search in the installed
 * YouTube app (the v0.2 behaviour, verified on the TV). Live news, trailers and release dates
 * come in Phase 6, only from sources Sri approves in SOURCES.md. Nothing here is fabricated.
 */
@Composable
fun EntertainmentScreen(state: LauncherUiState, viewModel: LauncherViewModel, gamingFirst: Boolean) {
    val youtube = state.installedApp(KnownEntertainmentApp.YOUTUBE)
    val motion = if (state.settings.liteMode) 0f else state.settings.animation.factor
    val sections = listOf(
        Section("Tamil Cinema", "Trailers and new Tamil films", "Tamil cinema", "TC", Color(0xFFEAB308)),
        Section("Technology & AI", "AI, gadgets, developer tools", "technology news", "AI", Color(0xFF8B5CF6)),
        Section("Gaming Centre", "PlayStation and gaming news", "PlayStation news", "GC", Color(0xFF22D3EE)),
    )
    val first = remember { FocusRequester() }
    val focusIndex = if (gamingFirst) 2 else 0
    LaunchedEffect(gamingFirst) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        CinematicBackdrop(wallpaperPath = null, animate = motion > 0f)
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 28.dp)) {
            Text("ENTERTAINMENT CENTRE", color = LauncherColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("Tamil cinema · Technology & AI · Gaming", color = LauncherColors.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (youtube != null) "OK opens the search in YouTube. Live news, trailers and release dates arrive in the next update, only from sources you approve."
                else "YouTube is not installed, so these shortcuts are unavailable. Install YouTube from the Play Store.",
                color = LauncherColors.TextSecondary,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                sections.forEachIndexed { i, section ->
                    FocusCard(
                        onClick = {
                            if (youtube != null) viewModel.openYouTubeSearch(youtube, section.query)
                            else viewModel.openNotInstalled(KnownEntertainmentApp.YOUTUBE)
                        },
                        motion = motion,
                        background = Brush.linearGradient(listOf(section.accent.copy(alpha = 0.35f), LauncherColors.Surface)),
                        modifier = Modifier
                            .width(270.dp)
                            .height(190.dp)
                            .then(if (i == focusIndex) Modifier.focusRequester(first) else Modifier),
                    ) {
                        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(46.dp).clip(CircleShape).background(section.accent.copy(alpha = 0.6f)),
                            ) {
                                Text(section.glyph, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(section.title, color = LauncherColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text(section.subtitle, color = LauncherColors.TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(if (youtube != null) "Search on YouTube" else "Not installed", color = section.accent, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
