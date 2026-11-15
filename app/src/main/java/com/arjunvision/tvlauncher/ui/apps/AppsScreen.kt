package com.arjunvision.tvlauncher.ui.apps

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.LauncherApp
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.AppIcon
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.components.rememberAppVisual
import com.arjunvision.tvlauncher.ui.theme.LauncherColors

/**
 * App library. OK opens an app. Hold OK (or MENU) for: add to / remove from dock, move left or
 * right in the dock, hide. Hidden apps stay listed at the bottom and can be restored.
 */
@Composable
fun AppsScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val firstTile = remember { FocusRequester() }
    val motion = if (state.settings.liteMode) 0f else state.settings.animation.factor
    var menuFor by remember { mutableStateOf<LauncherApp?>(null) }
    val favourites = state.favouriteApps.map { it.packageName }

    LaunchedEffect(Unit) { runCatching { firstTile.requestFocus() } }
    BackHandler(enabled = menuFor != null) { menuFor = null }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LauncherColors.Background, LauncherColors.Surface)))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("App library", color = LauncherColors.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("OK opens · hold OK or MENU to edit dock / hide", color = LauncherColors.TextSecondary, fontSize = 15.sp)
            }
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 190.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(state.visibleApps, key = { _, a -> a.packageName }) { index, app ->
                    AppTile(
                        app = app,
                        inDock = app.packageName in favourites,
                        hidden = false,
                        motion = motion,
                        onClick = { viewModel.launchEntertainment(app) },
                        onMenu = { menuFor = app },
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
                if (state.hiddenApps.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Hidden apps (OK restores)", color = LauncherColors.TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                    itemsIndexed(state.hiddenApps, key = { _, a -> "hidden-" + a.packageName }) { _, app ->
                        AppTile(app, inDock = false, hidden = true, motion = motion, onClick = { viewModel.toggleHidden(app) }, onMenu = { viewModel.toggleHidden(app) })
                    }
                }
            }
        }
        menuFor?.let { app ->
            TileMenu(
                app = app,
                inDock = app.packageName in favourites,
                motion = motion,
                onDock = { viewModel.toggleFavourite(app); menuFor = null },
                onLeft = { viewModel.moveFavourite(app, -1) },
                onRight = { viewModel.moveFavourite(app, 1) },
                onHide = { viewModel.toggleHidden(app); menuFor = null },
                onClose = { menuFor = null },
            )
        }
    }
}

@Composable
private fun AppTile(
    app: LauncherApp,
    inDock: Boolean,
    hidden: Boolean,
    motion: Float,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = rememberAppVisual(app.packageName).color ?: LauncherColors.Blue
    FocusCard(
        onClick = onClick,
        onLongPress = onMenu,
        motion = motion,
        background = Brush.linearGradient(listOf(glow.copy(alpha = if (hidden) 0.08f else 0.22f), LauncherColors.Surface)),
        modifier = modifier.fillMaxWidth().height(140.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(12.dp),
        ) {
            AppIcon(app.packageName, Modifier.size(54.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = app.label,
                color = if (hidden) LauncherColors.TextSecondary else LauncherColors.TextPrimary,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (inDock) Text("IN DOCK", color = glow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TileMenu(
    app: LauncherApp,
    inDock: Boolean,
    motion: Float,
    onDock: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onHide: () -> Unit,
    onClose: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(app.packageName) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(LauncherColors.Surface).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(app.packageName, Modifier.size(64.dp))
            Text(app.label, color = LauncherColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuButton(if (inDock) "Remove from dock" else "Add to dock", motion, Modifier.focusRequester(first), onDock)
                if (inDock) {
                    MenuButton("Move left", motion, onClick = onLeft)
                    MenuButton("Move right", motion, onClick = onRight)
                }
                MenuButton("Hide", motion, onClick = onHide)
                MenuButton("Close", motion, onClick = onClose)
            }
        }
    }
}

@Composable
private fun MenuButton(text: String, motion: Float, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusCard(
        onClick = onClick,
        motion = motion,
        shape = RoundedCornerShape(50),
        background = Brush.linearGradient(listOf(LauncherColors.GlassFill, LauncherColors.GlassFill)),
        modifier = modifier.height(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text(text, color = LauncherColors.TextPrimary, fontSize = 15.sp, maxLines = 1)
        }
    }
}
