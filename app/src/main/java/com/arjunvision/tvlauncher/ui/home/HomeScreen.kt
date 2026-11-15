package com.arjunvision.tvlauncher.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.InputConnection
import com.arjunvision.tvlauncher.domain.models.KnownEntertainmentApp
import com.arjunvision.tvlauncher.domain.models.Profile
import com.arjunvision.tvlauncher.domain.models.ThemeSpec
import com.arjunvision.tvlauncher.platform.network.NetworkState
import com.arjunvision.tvlauncher.platform.network.networkStateFlow
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.Screen
import com.arjunvision.tvlauncher.ui.components.AppIcon
import com.arjunvision.tvlauncher.ui.components.AuroraBackground
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.components.GlassDock
import com.arjunvision.tvlauncher.ui.components.GlassHighlightState
import com.arjunvision.tvlauncher.ui.components.LogoMark
import com.arjunvision.tvlauncher.ui.components.ParticleLayer
import com.arjunvision.tvlauncher.ui.components.ThemeWallpaper
import com.arjunvision.tvlauncher.ui.components.rememberAppVisual
import com.arjunvision.tvlauncher.ui.components.timeOfDayGreeting
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private data class DockTile(
    val id: String,
    val title: String,
    /** Name shown large in the top shelf when focused. */
    val displayName: String,
    val subtitle: String,
    val glyph: String,
    val packageName: String?,
    val accent: Color,
    val onClick: () -> Unit,
)

/**
 * FUTURE CINEMA OS home. [preview] non-null = Theme Studio preview: OK on any tile applies the
 * theme instead of opening it, and BACK returns to the studio.
 */
@Composable
fun HomeScreen(state: LauncherUiState, viewModel: LauncherViewModel, preview: ThemeSpec? = null) {
    val settings = state.settings
    val isFather = settings.profile == Profile.FATHER
    val motion = if (settings.liteMode) 0f else settings.animation.factor
    val tileWidth: Dp = if (isFather) maxOf(settings.cardSize.widthDp, 250).dp else settings.cardSize.widthDp.dp
    val firstTile = remember { FocusRequester() }
    val highlight = remember { GlassHighlightState() }
    var focusedTitle by remember { mutableStateOf<String?>(null) }
    var focusedSubtitle by remember { mutableStateOf<String?>(null) }
    val ambient by animateColorAsState(
        targetValue = highlight.accent ?: LauncherColors.Blue,
        animationSpec = tween(durationMillis = if (motion == 0f) 0 else 360),
        label = "ambientGlow",
    )

    val tiles = buildDockTiles(state, viewModel, isFather)

    LaunchedEffect(Unit) { runCatching { firstTile.requestFocus() } }

    Box(modifier = Modifier.fillMaxSize().background(LauncherColors.Background)) {
        ThemeWallpaper(path = LauncherStyle.wallpaperPath, modifier = Modifier.fillMaxSize())
        AuroraBackground(animate = motion > 0f, glow = ambient)
        if (!settings.liteMode) ParticleLayer(style = LauncherStyle.particles, animate = motion > 0f)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp)) {
            HomeHeader(state = state, viewModel = viewModel, motion = motion)

            // Top shelf: names the focused tile in large type over the ambient glow.
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 8.dp, bottom = 18.dp),
            ) {
                Text(
                    text = if (preview != null) "THEME PREVIEW" else timeOfDayGreeting().uppercase(),
                    color = ambient,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = preview?.name ?: focusedTitle ?: if (isFather) "Your TV, made simple" else "Your entertainment hub",
                    color = LauncherColors.TextPrimary,
                    fontSize = 50.sp,
                    fontWeight = LauncherStyle.titleFontWeight,
                    letterSpacing = LauncherStyle.titleLetterSpacing.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (preview != null) "OK applies this theme · BACK returns to Theme Studio"
                    else focusedSubtitle ?: settings.profile.subtitle,
                    color = LauncherColors.TextSecondary,
                    fontSize = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            GlassDock(state = highlight, motion = motion) {
                itemsIndexed(tiles, key = { _, t -> t.id }) { index, tile ->
                    DockTileView(
                        tile = tile,
                        width = tileWidth,
                        motion = motion,
                        highlight = highlight,
                        onClick = if (preview != null) ({ viewModel.applyTheme(preview) }) else tile.onClick,
                        onFocused = {
                            focusedTitle = tile.displayName
                            focusedSubtitle = tile.subtitle
                        },
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
            }

            LauncherStyle.credit?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = LauncherColors.TextSecondary.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun buildDockTiles(state: LauncherUiState, viewModel: LauncherViewModel, isFather: Boolean): List<DockTile> {
    val settings = state.settings
    fun inputSubtitle(id: String?): String {
        val input = state.hdmiInput(id) ?: return "Choose input in Settings"
        val status = input.connection.takeIf { it != InputConnection.UNKNOWN }?.label
        return listOfNotNull(input.label, status).joinToString(" · ")
    }
    val youtube = state.installedApp(KnownEntertainmentApp.YOUTUBE)
    return buildList {
        if (!isFather) {
            add(DockTile("ps5", "PLAYSTATION 5", "PlayStation 5", inputSubtitle(settings.playStationInputId), "PS5", null, LauncherColors.Violet) {
                viewModel.show(Screen.PlayStation)
            })
        }
        add(DockTile("cable", "CABLE TV", "Cable TV", inputSubtitle(settings.cableInputId), "TV", null, Color(0xFF00B8FF), viewModel::openCable))
        state.favouriteApps.forEach { app ->
            add(DockTile(app.packageName, app.label.uppercase(), app.label, "Open app", "", app.packageName, LauncherColors.Surface) {
                viewModel.launchEntertainment(app)
            })
        }
        if (!isFather && youtube != null) {
            add(DockTile("tamil-cinema", "TAMIL CINEMA", "Tamil Cinema", "On YouTube", "TC", null, Color(0xFFEAB308)) {
                viewModel.openYouTubeSearch(youtube, "Tamil cinema")
            })
            add(DockTile("gaming-news", "GAMING NEWS", "Gaming News", "On YouTube", "GN", null, Color(0xFF22D3EE)) {
                viewModel.openYouTubeSearch(youtube, "gaming news")
            })
            add(DockTile("tech-news", "TECH NEWS", "Tech News", "On YouTube", "AI", null, Color(0xFF8B5CF6)) {
                viewModel.openYouTubeSearch(youtube, "technology news")
            })
        }
        if (!isFather) {
            add(DockTile("apps", "APPS", "App Library", "${state.visibleApps.size} apps · hold OK to edit", "APP", null, LauncherColors.Blue) {
                viewModel.show(Screen.Apps)
            })
            add(DockTile("themes", "THEME STUDIO", "Online Theme Studio", LauncherStyle.themeName, "TH", null, Color(0xFFF472B6)) {
                viewModel.show(Screen.ThemeStudio)
            })
        }
        add(DockTile("settings", "SETTINGS", "Settings", "Personalise your TV", "SET", null, LauncherColors.TextSecondary) {
            viewModel.show(Screen.Settings)
        })
    }
}

@Composable
private fun DockTileView(
    tile: DockTile,
    width: Dp,
    motion: Float,
    highlight: GlassHighlightState,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val appColor = tile.packageName?.let { rememberAppVisual(it).color }
    val glowColor = appColor ?: tile.accent.takeIf { it != LauncherColors.Surface } ?: LauncherColors.Blue
    FocusCard(
        onClick = onClick,
        motion = motion,
        showFocusBorder = false,
        background = Brush.linearGradient(listOf(glowColor.copy(alpha = 0.28f), LauncherColors.Surface.copy(alpha = 0.92f))),
        onFocusChange = {
            focused = it
            if (it) onFocused()
        },
        modifier = modifier
            .width(width)
            .height(width * 0.6f)
            .onGloballyPositioned { if (focused) highlight.focusOn(it, glowColor) },
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(18.dp),
        ) {
            if (tile.packageName != null) {
                AppIcon(tile.packageName, Modifier.size(42.dp))
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(glowColor.copy(alpha = 0.55f)),
                ) {
                    Text(text = tile.glyph, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column {
                Text(
                    text = tile.title,
                    color = LauncherColors.TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tile.subtitle,
                    color = LauncherColors.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(state: LauncherUiState, viewModel: LauncherViewModel, motion: Float) {
    val settings = state.settings
    val context = LocalContext.current
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1_000L)
        }
    }
    val timeFormat = remember(settings.clock24h) {
        SimpleDateFormat(if (settings.clock24h) "HH:mm" else "h:mm a", Locale.getDefault())
    }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val network by produceState(initialValue = NetworkState(), context) {
        networkStateFlow(context).collect { value = it }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        LogoMark(size = 52.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = "ARJUN VISION", color = LauncherColors.TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(text = "YOUR SMART TV EXPERIENCE", color = LauncherColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(text = timeFormat.format(now), color = LauncherColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Text(text = dateFormat.format(now), color = LauncherColors.TextSecondary, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(20.dp))
        GlassPill(
            text = network.label,
            // Fixed status colours: a red festival accent must never read as "offline".
            dot = when {
                network.internet -> Color(0xFF22C55E)
                network.connected -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            },
        )
        Spacer(modifier = Modifier.width(12.dp))
        HeaderButton(text = settings.profile.title, motion = motion) { viewModel.show(Screen.Profiles) }
        Spacer(modifier = Modifier.width(10.dp))
        HeaderButton(text = "Settings", motion = motion) { viewModel.show(Screen.Settings) }
    }
}

@Composable
private fun GlassPill(text: String, dot: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LauncherColors.GlassFill)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(dot))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = LauncherColors.TextPrimary.copy(alpha = 0.85f), fontSize = 14.sp)
    }
}

@Composable
private fun HeaderButton(text: String, motion: Float, onClick: () -> Unit) {
    FocusCard(
        onClick = onClick,
        motion = motion,
        shape = RoundedCornerShape(50),
        background = Brush.linearGradient(listOf(LauncherColors.GlassFill, LauncherColors.GlassFill)),
        modifier = Modifier.height(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text(text = text, color = LauncherColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
