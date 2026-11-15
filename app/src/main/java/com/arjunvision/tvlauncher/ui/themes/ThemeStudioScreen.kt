package com.arjunvision.tvlauncher.ui.themes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.InspirationItem
import com.arjunvision.tvlauncher.domain.models.ThemeGroup
import com.arjunvision.tvlauncher.domain.models.ThemeSpec
import com.arjunvision.tvlauncher.domain.themes.ThemeScheduler
import com.arjunvision.tvlauncher.platform.network.NetworkState
import com.arjunvision.tvlauncher.platform.network.networkStateFlow
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.AuroraBackground
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.components.decodeSampled
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Section(val title: String, val themes: List<ThemeSpec>)

/** ONLINE THEME STUDIO: browse, preview, apply, save, remove, schedule. Remote-only controls. */
@Composable
fun ThemeStudioScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val themeState by viewModel.themeState.collectAsState()
    val working by viewModel.working.collectAsState()
    val context = LocalContext.current
    val network by produceState(initialValue = NetworkState(), context) {
        networkStateFlow(context).collect { value = it }
    }
    val active = viewModel.activeTheme(themeState)
    val all = viewModel.allThemes(themeState)
    val generated = themeState.savedThemes.sortedByDescending { it.createdAt }
    val today = LocalDate.now()
    val upcoming = ThemeScheduler.upcoming(today)
    val scheduled = ThemeScheduler.decide(today)
    var selected by remember { mutableStateOf<ThemeSpec?>(null) }
    val firstButton = remember { FocusRequester() }
    val motion = if (state.settings.liteMode) 0f else state.settings.animation.factor

    LaunchedEffect(Unit) { runCatching { firstButton.requestFocus() } }
    BackHandler(enabled = selected != null) { selected = null }

    val sections = buildList {
        if (generated.isNotEmpty()) add(Section("Fresh from online inspiration", generated.filter { !it.saved }))
        add(Section("Saved themes", generated.filter { it.saved }))
        listOf(ThemeGroup.FESTIVAL, ThemeGroup.GAMING, ThemeGroup.CINEMATIC, ThemeGroup.FUTURISTIC, ThemeGroup.SEASONAL, ThemeGroup.SPECIAL)
            .forEach { g -> add(Section(g.title, all.filter { it.category.group == g && !it.generated })) }
    }.filter { it.themes.isNotEmpty() }

    Box(modifier = Modifier.fillMaxSize().background(LauncherColors.Background)) {
        AuroraBackground(animate = motion > 0f)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ONLINE THEME STUDIO", color = LauncherColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text(
                            text = "Active: ${active.name}" + if (themeState.locked) "  (locked)" else "",
                            color = LauncherColors.TextPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Today's schedule: ${scheduled.reason} · Sources: ${viewModel.sourceNames.joinToString()}",
                            color = LauncherColors.TextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                    StatusDot(network)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StudioButton(if (working) "Working..." else "Discover now", motion, Modifier.focusRequester(firstButton)) { viewModel.discoverNow() }
                    StudioButton("Automatic: " + if (themeState.autoEnabled) "On" else "Off", motion) { viewModel.toggleAutomatic() }
                    StudioButton("Lock theme: " + if (themeState.locked) "On" else "Off", motion) { viewModel.toggleLock() }
                    if (upcoming != null) {
                        val festivalTheme = generated.firstOrNull { it.category == upcoming.festival.category }
                            ?: all.first { it.category == upcoming.festival.category && !it.generated }
                        StudioButton("Preview ${upcoming.festival.name} (in ${upcoming.daysAway} days)", motion) { viewModel.preview(festivalTheme) }
                    }
                }
            }
            item {
                val last = themeState.lastDiscoveryResult ?: "No online discovery yet."
                val whenText = if (themeState.lastDiscoveryMillis > 0) " · " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(themeState.lastDiscoveryMillis)) else ""
                Text(text = last + whenText, color = LauncherColors.TextSecondary, fontSize = 14.sp, maxLines = 2)
                upcoming?.takeIf { it.festival.dateNeedsCheck }?.let {
                    Text(
                        text = "Check the ${it.festival.name} date (${it.festival.date}) against the official Tamil Nadu holiday list.",
                        color = Color(0xFFF59E0B),
                        fontSize = 13.sp,
                    )
                }
            }
            sections.forEach { section ->
                item(key = section.title) {
                    Column {
                        Text(section.title, color = LauncherColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
                            items(section.themes, key = { it.id }) { theme ->
                                ThemeTile(theme, isActive = theme.id == active.id, motion = motion) { selected = theme }
                            }
                        }
                    }
                }
            }
            val inspirations = themeState.inspirations.filter { !it.failed }.take(20)
            if (inspirations.isNotEmpty()) {
                item(key = "inspiration") {
                    Column {
                        Text("Recently discovered inspiration (OK = create a theme from it)", color = LauncherColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
                            items(inspirations, key = { it.id }) { item ->
                                InspirationTile(item, motion) { viewModel.createFromInspiration(item) }
                            }
                        }
                    }
                }
            }
        }

        selected?.let { theme ->
            ThemeActionPanel(theme, isActive = theme.id == active.id, motion = motion, viewModel = viewModel) { selected = null }
        }
    }
}

@Composable
private fun StatusDot(network: NetworkState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(LauncherColors.GlassFill).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(if (network.internet) Color(0xFF22C55E) else Color(0xFFF59E0B)))
        Spacer(Modifier.width(8.dp))
        Text(if (network.internet) network.label else "${network.label} · offline mode", color = LauncherColors.TextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun StudioButton(text: String, motion: Float, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusCard(
        onClick = onClick,
        motion = motion,
        shape = RoundedCornerShape(50),
        background = Brush.linearGradient(listOf(LauncherColors.GlassFill, LauncherColors.GlassFill)),
        modifier = modifier.height(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(text, color = LauncherColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun rememberThumb(path: String?): ImageBitmap? {
    val thumb by produceState<ImageBitmap?>(initialValue = null, path) {
        value = path?.let { withContext(Dispatchers.IO) { decodeSampled(it, 400) } }
    }
    return thumb
}

/** A swatch of the theme's own colours (and wallpaper thumbnail when it has one). */
@Composable
private fun ThemeTile(theme: ThemeSpec, isActive: Boolean, motion: Float, onClick: () -> Unit) {
    val thumb = rememberThumb(theme.wallpaperPath)
    val bg = Color(theme.background.toInt())
    val accent = Color(theme.accent.toInt())
    val accent2 = Color(theme.accent2.toInt())
    FocusCard(
        onClick = onClick,
        motion = motion,
        shape = RoundedCornerShape(theme.cardCornerDp.dp),
        background = Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), bg, accent2.copy(alpha = 0.5f))),
        modifier = Modifier.width(210.dp).height(124.dp),
    ) {
        thumb?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, bg.copy(alpha = 0.9f)))))
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
                Box(Modifier.size(14.dp).clip(CircleShape).background(accent2))
                if (isActive) Text("ACTIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (theme.saved) Text("SAVED", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(theme.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InspirationTile(item: InspirationItem, motion: Float, onClick: () -> Unit) {
    val thumb = rememberThumb(item.localPath)
    FocusCard(onClick = onClick, motion = motion, shape = RoundedCornerShape(18.dp), modifier = Modifier.width(240.dp).height(124.dp)) {
        thumb?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Bottom) {
            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.creator} · ${item.source}", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.license, color = LauncherColors.Blue, fontSize = 11.sp, maxLines = 1)
        }
    }
}

/** Preview / Apply / Save / Remove for one theme. BACK closes it. */
@Composable
private fun ThemeActionPanel(theme: ThemeSpec, isActive: Boolean, motion: Float, viewModel: LauncherViewModel, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(theme.id) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(LauncherColors.Surface)
                .padding(28.dp),
        ) {
            Text(theme.name, color = LauncherColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                text = theme.credit ?: if (theme.generated) "Original ARJUN VISION theme" else "Built-in ARJUN VISION theme · works offline",
                color = LauncherColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 2,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StudioButton("Preview", motion, Modifier.focusRequester(first)) { viewModel.preview(theme) }
                StudioButton(if (isActive) "Applied" else "Apply", motion) { viewModel.applyTheme(theme); onClose() }
                if (theme.generated) {
                    StudioButton(if (theme.saved) "Saved" else "Save", motion) { viewModel.saveTheme(theme) }
                    StudioButton("Remove", motion) { viewModel.removeTheme(theme); onClose() }
                }
                StudioButton("Close", motion) { onClose() }
            }
        }
    }
}
