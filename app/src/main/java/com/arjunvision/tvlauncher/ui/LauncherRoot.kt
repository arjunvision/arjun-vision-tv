package com.arjunvision.tvlauncher.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arjunvision.tvlauncher.ui.apps.AppsScreen
import com.arjunvision.tvlauncher.ui.gaming.PlayStationScreen
import com.arjunvision.tvlauncher.ui.home.HomeScreen
import com.arjunvision.tvlauncher.ui.profiles.ProfileScreen
import com.arjunvision.tvlauncher.ui.screensaver.ScreensaverScreen
import com.arjunvision.tvlauncher.ui.settings.SettingsScreen
import com.arjunvision.tvlauncher.ui.startup.StartupScreen
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle
import com.arjunvision.tvlauncher.ui.themes.ThemeStudioScreen

@Composable
fun LauncherRoot(viewModel: LauncherViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    val preview by viewModel.previewTheme.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Live theming: the preview (if any) or the active theme repaints every screen.
    val shownTheme = preview ?: viewModel.activeTheme(themeState)
    LaunchedEffect(shownTheme) { LauncherStyle.apply(shownTheme) }
    LaunchedEffect(state.settings.liteMode) { LauncherStyle.lite = state.settings.liteMode }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }

    // Back: preview -> studio, everything else -> Home. On Home, Back does nothing (normal launcher).
    BackHandler(enabled = state.screen != Screen.Home) { viewModel.onBack() }

    val fade = if (state.settings.liteMode) 150 else 500
    AnimatedContent(
        targetState = state.screen,
        transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade * 2 / 3)) },
        label = "screenTransition",
    ) { screen ->
        when (screen) {
            Screen.Startup -> StartupScreen(motion = state.settings.animation.factor, onFinished = viewModel::goHome)
            Screen.Home -> HomeScreen(state = state, viewModel = viewModel)
            Screen.Profiles -> ProfileScreen(state = state, viewModel = viewModel)
            Screen.Apps -> AppsScreen(state = state, viewModel = viewModel)
            Screen.Settings -> SettingsScreen(state = state, viewModel = viewModel)
            Screen.PlayStation -> PlayStationScreen(state = state, viewModel = viewModel)
            Screen.ThemeStudio -> ThemeStudioScreen(state = state, viewModel = viewModel)
            Screen.ThemePreview -> HomeScreen(state = state, viewModel = viewModel, preview = preview)
            Screen.Screensaver -> ScreensaverScreen(state = state)
        }
    }
}
