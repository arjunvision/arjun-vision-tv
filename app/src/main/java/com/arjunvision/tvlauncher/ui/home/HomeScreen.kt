package com.arjunvision.tvlauncher.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.arjunvision.tvlauncher.ui.components.CinematicBackdrop
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.components.LogoMark
import com.arjunvision.tvlauncher.ui.components.timeOfDayGreeting
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** Card artwork style. All original shapes and gradients; app cards use the real installed icon. */
private enum class CardArt { AIRTEL, PS5, APP, MISSING_APP, GAMING, ENTERTAINMENT, THEMES, HUB, APPS, SETTINGS }

private data class DockTile(
    val id: String,
    val name: String,
    val description: String,
    val art: CardArt,
    val packageName: String?,
    /** Hero text when this card is focused. */
    val heroTitle: String,
    val heroSubtitle: String,
    val cta: String,
    val status: String?,
    val statusOk: Boolean = true,
    val onClick: () -> Unit,
)

/** Text of the focused card, written by cards and read only by [Hero] (no full-screen recomposition). */
private class HeroState {
    var tile by mutableStateOf<DockTile?>(null)
}

/**
 * ARJUN VISION – CINEMATIC OS home (approved preview, 2026-11-16).
 * Full-screen artwork backdrop (one baked layer), split hero on the left, uniform 16:9 cards.
 * [preview] non-null = Theme Studio preview: OK applies the theme, BACK returns to the studio.
 */
@Composable
fun HomeScreen(state: LauncherUiState, viewModel: LauncherViewModel, preview: ThemeSpec? = null) {
    val settings = state.settings
    val isFather = settings.profile == Profile.FATHER
    val baseMotion = if (settings.liteMode) 0f else settings.animation.factor
    val motion = if (isFather) minOf(baseMotion, 0.5f) else baseMotion
    // Android TV card grid widths (developer.android.com/design/ui/tv): 268 dp = 3 per row, 196 dp = 4.
    val cardWidth: Dp = if (isFather) 268.dp else 196.dp
    val firstTile = remember { FocusRequester() }
    val hero = remember { HeroState() }
    val themeState by viewModel.themeState.collectAsState()
    val wallpaper = viewModel.homeWallpaper(state, viewModel.activeTheme(themeState).wallpaperPath)

    val tiles = buildDockTiles(state, viewModel, isFather)
    if (hero.tile == null || tiles.none { it.id == hero.tile?.id }) hero.tile = tiles.firstOrNull()

    LaunchedEffect(Unit) { runCatching { firstTile.requestFocus() } }

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackdrop(wallpaperPath = wallpaper?.source, animate = false)

        // 5 % overscan-safe margins from Android TV layout guidance: 58 dp sides, 28 dp top/bottom.
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 58.dp, vertical = 28.dp)) {
            HomeHeader(state = state, viewModel = viewModel, motion = motion)
            Hero(
                hero = hero,
                preview = preview,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 18.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 2.dp, end = 24.dp, top = 12.dp, bottom = 10.dp),
            ) {
                itemsIndexed(tiles, key = { _, t -> t.id }) { index, tile ->
                    MediaCard(
                        tile = tile,
                        width = cardWidth,
                        large = isFather,
                        motion = motion,
                        onClick = if (preview != null) ({ viewModel.applyTheme(preview) }) else tile.onClick,
                        onFocused = { hero.tile = tile },
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
            }
            Text(
                text = wallpaper?.credit.orEmpty(),
                color = LauncherColors.TextSecondary.copy(alpha = 0.75f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}

@Composable
private fun Hero(hero: HeroState, preview: ThemeSpec?, modifier: Modifier) {
    val tile = hero.tile
    Column(verticalArrangement = Arrangement.Center, modifier = modifier.padding(top = 24.dp)) {
        Text(
            text = if (preview != null) "THEME PREVIEW" else timeOfDayGreeting().uppercase(),
            color = LauncherColors.Blue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = preview?.name ?: tile?.heroTitle.orEmpty(),
            color = Color.White,
            fontSize = 50.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.62f),
        )
        Text(
            text = if (preview != null) "OK applies this theme · BACK returns to Theme Studio" else tile?.heroSubtitle.orEmpty(),
            color = Color(0xFFE4E4E7),
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.62f),
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(LauncherColors.Blue)
                    .padding(horizontal = 22.dp, vertical = 9.dp),
            ) {
                Text("OK  " + (if (preview != null) "Apply" else tile?.cta.orEmpty()), color = Color(0xFF0A0A0C), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            tile?.status?.let {
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (tile.statusOk) Color(0xFF22C55E) else Color(0xFFF59E0B)))
                Spacer(Modifier.width(8.dp))
                Text(it, color = Color(0xFFE4E4E7), fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun buildDockTiles(state: LauncherUiState, viewModel: LauncherViewModel, isFather: Boolean): List<DockTile> {
    val settings = state.settings
    fun inputStatus(id: String?): Pair<String?, Boolean> {
        val input = state.hdmiInput(id) ?: return null to true
        val conn = input.connection.takeIf { it != InputConnection.UNKNOWN }
        return listOfNotNull(input.label, conn?.label).joinToString(" · ") to (conn == null || conn == InputConnection.CONNECTED)
    }
    val (airtelStatus, airtelOk) = inputStatus(settings.cableInputId)
    val (ps5Status, ps5Ok) = inputStatus(settings.playStationInputId)
    // Airtel and PS5 cards both switch straight to their HDMI input.
    val airtel = DockTile("airtel", "AIRTEL DTH", "Father's TV · Watch Live TV", CardArt.AIRTEL, null,
        "AIRTEL DIGITAL TV", "Watch Live Television", "Watch now", airtelStatus, airtelOk, viewModel::openCable)
    // PS5 = direct HDMI too (Sri: both HDMI devices one press away, in both modes).
    val ps5 = DockTile("ps5", "PLAYSTATION 5", "HDMI · Play now", CardArt.PS5, null,
        "PLAYSTATION 5", "Your Gaming Experience", "Play now", ps5Status, ps5Ok, viewModel::openPlayStation)

    val knownApps = KnownEntertainmentApp.entries.map { entry ->
        val app = state.installedApp(entry)
        if (app != null) {
            DockTile(entry.name, app.label, "Open app", CardArt.APP, app.packageName,
                app.label.uppercase(), "Open ${app.label}", "Open", null) { viewModel.launchEntertainment(app) }
        } else {
            DockTile(entry.name, entry.title, "Not installed", CardArt.MISSING_APP, null,
                entry.title.uppercase(), "Not installed on this TV", "Get it", "Not installed", false) { viewModel.openNotInstalled(entry) }
        }
    }
    val extras = state.extraFavouriteApps.map { app ->
        DockTile(app.packageName, app.label, "Open app", CardArt.APP, app.packageName,
            app.label.uppercase(), "Open ${app.label}", "Open", null) { viewModel.launchEntertainment(app) }
    }
    val settingsTile = DockTile("settings", "Settings", "Personalise your TV", CardArt.SETTINGS, null,
        "SETTINGS", "Wallpaper, profiles, HDMI and more", "Open", null) { viewModel.show(Screen.Settings) }
    return if (isFather) {
        listOf(airtel, ps5) + knownApps + extras + settingsTile
    } else {
        listOf(ps5, airtel) + knownApps + extras + listOf(
            DockTile("gaming", "Gaming Centre", "PS5 and gaming videos", CardArt.GAMING, null,
                "GAMING CENTRE", "PlayStation and gaming videos", "Open", null) { viewModel.show(Screen.Entertainment(gaming = true)) },
            DockTile("entertainment", "Entertainment", "Tamil cinema · tech · gaming", CardArt.ENTERTAINMENT, null,
                "ENTERTAINMENT CENTRE", "Tamil cinema, technology and gaming", "Open", null) { viewModel.show(Screen.Entertainment()) },
            DockTile("themes", "Theme Studio", LauncherStyle.themeName, CardArt.THEMES, null,
                "THEME STUDIO", "Themes and wallpapers", "Open", null) { viewModel.show(Screen.ThemeStudio) },
            DockTile("hdmi-hub", "HDMI Hub", "Airtel DTH · PS5", CardArt.HUB, null,
                "SMART HDMI HUB", "Inputs, test switch and setup", "Open", null) { viewModel.show(Screen.HdmiHub) },
            DockTile("apps", "Apps", "${state.visibleApps.size} apps", CardArt.APPS, null,
                "APP LIBRARY", "All apps · hold OK to edit the dock", "Open", null) { viewModel.show(Screen.Apps) },
            settingsTile,
        )
    }
}

/**
 * Image-led 16:9 card: original artwork (or the real app icon), bottom scrim, name and one line.
 * Focus: gentle zoom (FocusCard), gold border and a coloured hardware shadow as the glow.
 */
@Composable
private fun MediaCard(
    tile: DockTile,
    width: Dp,
    large: Boolean,
    motion: Float,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val glow by animateDpAsState(if (focused) 18.dp else 2.dp, tween(if (motion == 0f) 0 else 220), label = "cardGlow")
    Box(
        modifier = modifier
            .width(width)
            .height(width * 0.5625f)
            .shadow(glow, shape, clip = false, ambientColor = LauncherColors.Blue, spotColor = LauncherColors.Blue),
    ) {
        FocusCard(
            onClick = onClick,
            motion = motion,
            shape = shape,
            background = Brush.linearGradient(listOf(Color(0xFF1C1C21), Color(0xFF0E0E11))),
            onFocusChange = {
                focused = it
                if (it) onFocused()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            CardArtwork(tile.art, Modifier.fillMaxSize())
            if (tile.packageName != null) {
                AppIcon(tile.packageName, Modifier.align(Alignment.Center).padding(bottom = if (large) 30.dp else 22.dp).size(if (large) 58.dp else 42.dp))
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f)),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(tile.name, color = Color.White, fontSize = if (large) 18.sp else 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tile.description, color = Color(0xFFD4D4D8), fontSize = if (large) 12.sp else 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Original abstract card art (gradients and light lines). No logos, characters or third-party artwork. */
@Composable
private fun CardArtwork(art: CardArt, modifier: Modifier) {
    val gold = LauncherColors.Blue
    val crimson = LauncherColors.Violet
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (art) {
            CardArt.AIRTEL -> { // live TV: crimson studio light, broadcast bands, a framed screen
                drawRect(Brush.linearGradient(listOf(Color(0xFF130D12), Color(0xFF3D0A12)), Offset.Zero, Offset(w, h)))
                for (i in 0 until 9) {
                    val y = h * (0.22f + i * 0.065f)
                    drawLine(crimson.copy(alpha = 0.18f + i * 0.04f), Offset(w * 0.42f, y), Offset(w, y - h * 0.12f), strokeWidth = 2.5f)
                }
                drawRoundRect(gold.copy(alpha = 0.75f), Offset(w * 0.52f, h * 0.14f), Size(w * 0.38f, h * 0.40f), CornerRadius(10f), style = Stroke(3f))
            }
            CardArt.PS5 -> { // gaming: deep blue-violet with light streaks
                drawRect(Brush.linearGradient(listOf(Color(0xFF07091A), Color(0xFF2A1F63)), Offset.Zero, Offset(w, h)))
                for (i in 0 until 7) {
                    val x = w * (0.32f + i * 0.1f)
                    drawLine(Color(0xFF7896FF).copy(alpha = 0.12f + i * 0.03f), Offset(x, h), Offset(x + w * 0.22f, 0f), strokeWidth = 9f)
                }
            }
            CardArt.APP -> drawRect(Brush.verticalGradient(listOf(Color(0xFF26262C), Color(0xFF111114))))
            CardArt.MISSING_APP -> drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1A1E), Color(0xFF0C0C0E))))
            CardArt.GAMING -> drawRect(Brush.linearGradient(listOf(Color(0xFF052530), Color(0xFF0B0B10)), Offset.Zero, Offset(w, h)))
            CardArt.ENTERTAINMENT -> drawRect(Brush.linearGradient(listOf(Color(0xFF3A2808), Color(0xFF0B0B10)), Offset.Zero, Offset(w, h)))
            CardArt.THEMES -> drawRect(Brush.linearGradient(listOf(gold.copy(alpha = 0.45f), Color(0xFF0B0B10)), Offset.Zero, Offset(w, h)))
            CardArt.HUB -> drawRect(Brush.linearGradient(listOf(Color(0xFF07302B), Color(0xFF0B0B10)), Offset.Zero, Offset(w, h)))
            CardArt.APPS, CardArt.SETTINGS -> drawRect(Brush.linearGradient(listOf(Color(0xFF26262C), Color(0xFF0B0B10)), Offset.Zero, Offset(w, h)))
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
