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
import com.arjunvision.tvlauncher.ui.entertainment.EntertainmentScreen
import com.arjunvision.tvlauncher.ui.gaming.PlayStationScreen
import com.arjunvision.tvlauncher.ui.home.HomeScreen
import com.arjunvision.tvlauncher.ui.hdmi.HdmiHubScreen
import com.arjunvision.tvlauncher.ui.wallpapers.WallpapersScreen
import com.arjunvision.tvlauncher.ui.home.SetTopBoxPicker
import com.arjunvision.tvlauncher.domain.models.HdmiDetection
import androidx.compose.foundation.layout.Box
import com.arjunvision.tvlauncher.ui.profiles.ProfileScreen
import com.arjunvision.tvlauncher.ui.screensaver.ScreensaverScreen
import com.arjunvision.tvlauncher.ui.settings.SettingsScreen
import com.arjunvision.tvlauncher.ui.startup.StartupVideo
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle
import com.arjunvision.tvlauncher.ui.themes.ThemeStudioScreen

@Composable
fun LauncherRoot(viewModel: LauncherViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    val preview by viewModel.previewTheme.collectAsStateWithLifecycle()
    val showSetTopBoxPicker by viewModel.setTopBoxPicker.collectAsStateWithLifecycle()
    val hubPicker by viewModel.hubPicker.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Live theming: the preview (if any) or the active theme repaints every screen.
    val shownTheme = preview ?: viewModel.activeTheme(themeState)
    LaunchedEffect(shownTheme) { LauncherStyle.apply(shownTheme) }
    LaunchedEffect(state.settings.liteMode) { LauncherStyle.lite = state.settings.liteMode }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }

    // Back: preview -> studio, everything else -> Home. On Home, Back is consumed and does nothing,
    // so it never drops Father Mode onto the Google TV home (this launcher is not the system Home).
    BackHandler(enabled = true) { viewModel.onBack() }

    val fade = if (state.settings.liteMode) 150 else 500
    Box {
    AnimatedContent(
        targetState = state.screen,
        transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade * 2 / 3)) },
        label = "screenTransition",
    ) { screen ->
        when (screen) {
            Screen.Startup -> StartupVideo(motion = state.settings.animation.factor, onFinished = viewModel::goHome)
            Screen.Home -> HomeScreen(state = state, viewModel = viewModel)
            Screen.Profiles -> ProfileScreen(state = state, viewModel = viewModel)
            Screen.Apps -> AppsScreen(state = state, viewModel = viewModel)
            Screen.Settings -> SettingsScreen(state = state, viewModel = viewModel)
            Screen.PlayStation -> PlayStationScreen(state = state, viewModel = viewModel)
            Screen.ThemeStudio -> ThemeStudioScreen(state = state, viewModel = viewModel)
            Screen.HdmiHub -> HdmiHubScreen(state = state, viewModel = viewModel)
            Screen.Wallpapers -> WallpapersScreen(state = state, viewModel = viewModel)
            is Screen.Entertainment -> EntertainmentScreen(state = state, viewModel = viewModel, gamingFirst = screen.gaming)
            Screen.ThemePreview -> HomeScreen(state = state, viewModel = viewModel, preview = preview)
            Screen.Screensaver -> ScreensaverScreen(state = state)
        }
    }
    hubPicker?.let { device ->
        SetTopBoxPicker(
            choices = if (device == HubDevice.AIRTEL) HdmiDetection.setTopBoxChoices(state.hdmiInputs, state.settings.playStationInputId)
            else state.hdmiInputs,
            onPick = { viewModel.pickHubInput(device, it) },
            onDismiss = viewModel::dismissHubPicker,
            title = "Which input is ${device.title} on?",
            subtitle = "Saved on this TV. You can change it again here any time.",
        )
    }
    if (showSetTopBoxPicker) {
        SetTopBoxPicker(
            choices = HdmiDetection.setTopBoxChoices(state.hdmiInputs, state.settings.playStationInputId),
            onPick = viewModel::chooseSetTopBox,
            onDismiss = viewModel::dismissSetTopBoxPicker,
        )
    }
    }
}
