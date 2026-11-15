package com.arjunvision.tvlauncher.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arjunvision.tvlauncher.data.applications.InstalledAppsRepository
import com.arjunvision.tvlauncher.data.preferences.DataStoreSettingsRepository
import com.arjunvision.tvlauncher.data.themes.ThemeEngine
import com.arjunvision.tvlauncher.data.themes.ThemeRepository
import com.arjunvision.tvlauncher.domain.models.AppSettings
import com.arjunvision.tvlauncher.domain.models.HdmiInput
import com.arjunvision.tvlauncher.domain.models.InspirationItem
import com.arjunvision.tvlauncher.domain.models.KnownEntertainmentApp
import com.arjunvision.tvlauncher.domain.models.LauncherApp
import com.arjunvision.tvlauncher.domain.models.Profile
import com.arjunvision.tvlauncher.domain.models.ThemeSpec
import com.arjunvision.tvlauncher.domain.models.ThemeState
import com.arjunvision.tvlauncher.domain.repositories.SettingsRepository
import com.arjunvision.tvlauncher.domain.themes.ThemeFactory
import com.arjunvision.tvlauncher.platform.hdmi.HdmiInputRepository
import com.arjunvision.tvlauncher.platform.work.ThemeWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Startup : Screen
    data object Home : Screen
    data object Profiles : Screen
    data object Apps : Screen
    data object Settings : Screen
    data object PlayStation : Screen
    data object ThemeStudio : Screen
    data object ThemePreview : Screen
    data object Screensaver : Screen
}

data class LauncherUiState(
    val screen: Screen = Screen.Startup,
    val settings: AppSettings = AppSettings(),
    val apps: List<LauncherApp> = emptyList(),
    val hdmiInputs: List<HdmiInput> = emptyList(),
) {
    fun installedApp(entry: KnownEntertainmentApp): LauncherApp? =
        apps.firstOrNull { it.packageName in entry.packages }

    fun hdmiInput(inputId: String?): HdmiInput? = inputId?.let { id -> hdmiInputs.firstOrNull { it.inputId == id } }

    fun hdmiLabel(inputId: String?): String? = hdmiInput(inputId)?.label

    /** Apps shown in the library (hidden ones removed). */
    val visibleApps: List<LauncherApp> get() = apps.filter { it.packageName !in settings.hiddenPackages }

    val hiddenApps: List<LauncherApp> get() = apps.filter { it.packageName in settings.hiddenPackages }

    /** Ordered dock favourites that are really installed. Defaults to YouTube, Netflix, Hotstar. */
    val favouriteApps: List<LauncherApp>
        get() {
            val order = settings.favourites
                ?: KnownEntertainmentApp.entries.mapNotNull { installedApp(it)?.packageName }
            return order.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
        }
}

/**
 * Navigation, settings, themes and screensaver state. Repositories are wired by hand
 * (no DI framework) to keep the dependency graph small.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val settingsRepository: SettingsRepository = DataStoreSettingsRepository(application)
    private val appsRepository = InstalledAppsRepository(application)
    private val hdmiRepository = HdmiInputRepository(application)
    private val themeRepository = ThemeRepository.get(application)
    private val themeEngine = ThemeEngine(application)

    private val screen = MutableStateFlow<Screen>(Screen.Startup)
    private val apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val hdmiInputs = MutableStateFlow<List<HdmiInput>>(emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val themeState: StateFlow<ThemeState> = themeRepository.state

    private val _previewTheme = MutableStateFlow<ThemeSpec?>(null)
    val previewTheme: StateFlow<ThemeSpec?> = _previewTheme.asStateFlow()

    private val _working = MutableStateFlow(false)
    /** True while the studio is creating a theme in this session. */
    val working: StateFlow<Boolean> = _working.asStateFlow()

    val uiState: StateFlow<LauncherUiState> = combine(
        screen,
        settingsRepository.settings,
        apps,
        hdmiInputs,
    ) { currentScreen, settings, installed, inputs ->
        LauncherUiState(currentScreen, settings, installed, inputs)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState())

    private var lastInteraction = System.currentTimeMillis()
    private var foreground = true

    init {
        refreshDevices()
        ThemeWork.scheduleDaily(application)
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.startupAnimation && screen.value == Screen.Startup) screen.value = Screen.Home
        }
        // Apply today's scheduled theme from saved data (works offline).
        viewModelScope.launch(Dispatchers.IO) { themeEngine.applyAutomaticTheme() }
        viewModelScope.launch { screensaverLoop() }
    }

    fun activeTheme(state: ThemeState): ThemeSpec = themeRepository.activeTheme(state)

    fun allThemes(state: ThemeState): List<ThemeSpec> = themeRepository.allThemes(state)

    val sourceNames: List<String> get() = themeEngine.sourceNames

    fun refreshDevices() {
        apps.value = appsRepository.launchableApps()
        hdmiInputs.value = hdmiRepository.discoverHdmiInputs()
    }

    fun show(target: Screen) {
        screen.value = target
    }

    fun goHome() = show(Screen.Home)

    /** Back: preview returns to the studio; every other screen returns Home. */
    fun onBack(): Boolean = when (screen.value) {
        Screen.Home -> false
        Screen.ThemePreview -> {
            _previewTheme.value = null
            show(Screen.ThemeStudio)
            true
        }
        else -> {
            goHome()
            true
        }
    }

    // ---- Settings ----

    fun setProfile(profile: Profile) = updateSettings { it.copy(profile = profile) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.reset()
            _messages.tryEmit("Settings reset to defaults")
        }
    }

    /** Opens Android's own "Home app" chooser so the original Xiaomi launcher is always one step away. */
    fun openSystemHomeSettings() {
        val intents = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in intents) {
            try {
                app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                // try the next one
            }
        }
        _messages.tryEmit("Open the TV's Settings > Apps > Default apps > Home")
    }

    // ---- App library ----

    fun launchEntertainment(app: LauncherApp) {
        if (!appsRepository.launch(app)) _messages.tryEmit("${app.label} could not be opened")
    }

    /**
     * Opens a YouTube search inside the installed YouTube app (Tamil Cinema, gaming and tech news
     * tiles). No content is fetched by the launcher itself. Falls back to opening YouTube.
     */
    fun openYouTubeSearch(youtube: LauncherApp, query: String) {
        val uri = android.net.Uri.parse("https://www.youtube.com/results?search_query=" + android.net.Uri.encode(query))
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(youtube.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            launchEntertainment(youtube)
        }
    }

    fun toggleHidden(app: LauncherApp) = updateSettings { s ->
        val hidden = if (app.packageName in s.hiddenPackages) s.hiddenPackages - app.packageName else s.hiddenPackages + app.packageName
        s.copy(hiddenPackages = hidden)
    }

    fun toggleFavourite(app: LauncherApp) {
        val current = uiState.value.favouriteApps.map { it.packageName }
        val next = if (app.packageName in current) current - app.packageName else current + app.packageName
        updateSettings { it.copy(favourites = next) }
    }

    /** Moves a favourite one place left (-1) or right (+1) in the dock. */
    fun moveFavourite(app: LauncherApp, direction: Int) {
        val list = uiState.value.favouriteApps.map { it.packageName }.toMutableList()
        val i = list.indexOf(app.packageName)
        val j = i + direction
        if (i < 0 || j !in list.indices) return
        list[i] = list[j].also { list[j] = list[i] }
        updateSettings { it.copy(favourites = list) }
    }

    // ---- Inputs ----

    fun openCable() = openHdmi(uiState.value.settings.cableInputId, "Cable TV")

    fun openPlayStation() = openHdmi(uiState.value.settings.playStationInputId, "PlayStation 5")

    private fun openHdmi(inputId: String?, deviceName: String) {
        if (inputId == null) {
            _messages.tryEmit("Choose the $deviceName HDMI input in Settings first")
            show(Screen.Settings)
            return
        }
        if (!hdmiRepository.openPassthroughInput(inputId)) {
            _messages.tryEmit("The TV could not switch inputs from this app. Press INPUT on the TV remote and choose $deviceName.")
        }
    }

    // ---- Theme Studio ----

    fun preview(theme: ThemeSpec) {
        _previewTheme.value = theme
        show(Screen.ThemePreview)
    }

    /** Manual apply locks the theme so the scheduler never replaces it without permission. */
    fun applyTheme(theme: ThemeSpec) {
        viewModelScope.launch {
            themeRepository.update { it.copy(activeThemeId = theme.id, locked = true) }
            _previewTheme.value = null
            _messages.tryEmit("${theme.name} applied and locked. Unlock in Theme Studio to allow automatic themes.")
            show(Screen.ThemeStudio)
        }
    }

    fun saveTheme(theme: ThemeSpec) {
        if (!theme.generated) {
            _messages.tryEmit("Built-in themes are always available offline")
            return
        }
        viewModelScope.launch {
            themeRepository.update { s -> s.copy(savedThemes = s.savedThemes.map { if (it.id == theme.id) it.copy(saved = true) else it }) }
            _messages.tryEmit("${theme.name} saved")
        }
    }

    fun removeTheme(theme: ThemeSpec) {
        if (!theme.generated) {
            _messages.tryEmit("Built-in themes cannot be removed")
            return
        }
        viewModelScope.launch {
            themeRepository.update { s ->
                s.copy(
                    savedThemes = s.savedThemes.filter { it.id != theme.id },
                    activeThemeId = if (s.activeThemeId == theme.id) ThemeFactory.CLASSIC_ID else s.activeThemeId,
                )
            }
            _messages.tryEmit("${theme.name} removed")
        }
    }

    fun toggleAutomatic() {
        viewModelScope.launch {
            val s = themeRepository.update { it.copy(autoEnabled = !it.autoEnabled) }
            if (s.autoEnabled) themeEngine.applyAutomaticTheme()
        }
    }

    fun toggleLock() {
        viewModelScope.launch {
            val s = themeRepository.update { it.copy(locked = !it.locked) }
            if (!s.locked) themeEngine.applyAutomaticTheme()
        }
    }

    /** Runs discovery now in the app (so the user sees the result); WorkManager retries if offline. */
    fun discoverNow() {
        if (_working.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _working.value = true
            _messages.tryEmit("Looking for fresh licensed inspiration...")
            val result = runCatching { themeEngine.run(force = true) }
            _working.value = false
            val r = result.getOrNull()
            if (r == null) {
                _messages.tryEmit("Discovery failed: ${result.exceptionOrNull()?.message}")
            } else {
                _messages.tryEmit(r.message)
                if (r.needsNetworkRetry) ThemeWork.discoverWhenOnline(app, force = true)
            }
        }
    }

    fun createFromInspiration(item: InspirationItem) {
        if (_working.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _working.value = true
            val theme = runCatching { themeEngine.createFromInspiration(item) }.getOrNull()
            _working.value = false
            _messages.tryEmit(theme?.let { "Created ${it.name}" } ?: "That image could not be downloaded")
        }
    }

    // ---- Screensaver ----

    fun onUserActivity() {
        lastInteraction = System.currentTimeMillis()
    }

    fun setForeground(value: Boolean) {
        foreground = value
        lastInteraction = System.currentTimeMillis()
    }

    /** Any key wakes the screensaver; the key itself is consumed. */
    fun consumeKeyForScreensaver(event: KeyEvent): Boolean {
        if (screen.value != Screen.Screensaver) return false
        if (event.action == KeyEvent.ACTION_UP) goHome()
        return true
    }

    private suspend fun screensaverLoop() {
        while (true) {
            delay(15_000)
            val minutes = uiState.value.settings.screensaverMinutes
            val idle = System.currentTimeMillis() - lastInteraction
            val current = screen.value
            if (foreground && minutes > 0 && idle > minutes * 60_000L &&
                current != Screen.Screensaver && current != Screen.Startup && current != Screen.ThemePreview
            ) {
                show(Screen.Screensaver)
            }
        }
    }
}
