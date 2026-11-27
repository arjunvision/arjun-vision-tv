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
import com.arjunvision.tvlauncher.domain.models.HdmiDetection
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

/** The two HDMI devices the Smart HDMI Hub manages. */
enum class HubDevice(val title: String, val subtitle: String) {
    AIRTEL("AIRTEL DTH", "Father's TV"),
    PS5("PLAYSTATION 5", "My Gaming"),
}

sealed interface Screen {
    data object Startup : Screen
    data object Home : Screen
    data object Profiles : Screen
    data object Apps : Screen
    data object Settings : Screen
    data object PlayStation : Screen
    data object ThemeStudio : Screen
    data object HdmiHub : Screen
    data object Wallpapers : Screen
    /** Entertainment Centre; [gaming] = opened from the Gaming Centre tile (focus starts on gaming). */
    data class Entertainment(val gaming: Boolean = false) : Screen
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

    private val knownPackages: Set<String> get() = KnownEntertainmentApp.entries.flatMap { it.packages }.toSet()

    /** Dock favourites other than YouTube / Netflix / Hotstar (those three are fixed tiles since v2). */
    val extraFavouriteApps: List<LauncherApp> get() = favouriteApps.filter { it.packageName !in knownPackages }
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
    val wallpapers = com.arjunvision.tvlauncher.data.wallpapers.WallpaperRepository(application)
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

    /**
     * Opened again from the TV's Apps row while still running in the background: play the logo
     * video again (if the Startup animation setting is on). Returning with Back from an app does not.
     */
    fun replayStartup() {
        show(if (uiState.value.settings.startupAnimation) Screen.Startup else Screen.Home)
    }

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
    /** Home button helper status for Settings (Sri switches it on in Accessibility himself). */
    fun homeButtonHelperOn(): Boolean = com.arjunvision.tvlauncher.platform.home.StockHome.isEnabled(app)

    /** Android's screensaver settings, where Sri picks "Arjun Vision" himself. */
    fun openScreensaverSettings() {
        runCatching { app.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { openSystemHomeSettings() }
    }

    fun openAccessibilitySettings() {
        runCatching { app.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { openSystemHomeSettings() }
    }

    /** Escape hatch: show the Google TV home screen; the helper leaves it alone for one minute. */
    fun openGoogleTvHome() {
        com.arjunvision.tvlauncher.platform.home.StockHome.pause(android.os.SystemClock.elapsedRealtime())
        runCatching { app.startActivity(com.arjunvision.tvlauncher.platform.home.StockHome.googleTvHomeIntent()) }
            .onFailure { _messages.tryEmit("Google TV home could not be opened on this TV.") }
    }

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

    /** For a missing app: explain, then open its Play Store page (the Play Store is installed on this TV). */
    fun openNotInstalled(entry: KnownEntertainmentApp) {
        _messages.tryEmit("${entry.title} is not installed. Opening the Play Store page.")
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${entry.storePackage}"))
            .setPackage("com.android.vending")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            _messages.tryEmit("Install ${entry.title} from the Play Store app.")
        }
    }

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

    private val _setTopBoxPicker = MutableStateFlow(false)
    /** True only when Airtel's HDMI port could not be detected and has never been chosen (asked once). */
    val setTopBoxPicker: StateFlow<Boolean> = _setTopBoxPicker.asStateFlow()

    /**
     * AIRTEL DTH tile: a direct HDMI shortcut. Uses the saved input, else detects the box's port
     * (the one connected HDMI port that isn't the PS5's) and saves it, else asks once.
     * Stored under the v1 key `cable_input_id`, so earlier settings are kept.
     */
    fun openCable() {
        val s = uiState.value
        val inputs = s.hdmiInputs
        val saved = s.settings.cableInputId?.takeIf { id -> inputs.isEmpty() || inputs.any { it.inputId == id } }
        val target = saved ?: HdmiDetection.detectSetTopBox(inputs, s.settings.playStationInputId)?.inputId?.also { id ->
            updateSettings { it.copy(cableInputId = id) }
        }
        if (target == null) {
            _setTopBoxPicker.value = true
            return
        }
        switchInput(target, "Airtel DTH")
    }

    /** From the one-time picker: remember the port and switch to it now. */
    fun chooseSetTopBox(input: HdmiInput) {
        updateSettings { it.copy(cableInputId = input.inputId) }
        _setTopBoxPicker.value = false
        switchInput(input.inputId, "Airtel DTH")
    }

    fun dismissSetTopBoxPicker() {
        _setTopBoxPicker.value = false
    }

    /** PS5: saved input, else its HDMI-CEC input named "PlayStation 5" (saved), else Settings. */
    fun openPlayStation() {
        val s = uiState.value
        val target = s.settings.playStationInputId
            ?: HdmiDetection.detectPlayStation(s.hdmiInputs)?.inputId?.also { id -> updateSettings { it.copy(playStationInputId = id) } }
        if (target == null) {
            _messages.tryEmit("The TV does not report the PlayStation 5. Turn it on, then choose its HDMI input in Settings.")
            show(Screen.Settings)
            return
        }
        switchInput(target, "PlayStation 5")
    }

    // ---- Wallpapers ----

    /** What the Home screen shows for the current profile (see WallpaperRepository.resolve). */
    fun homeWallpaper(state: LauncherUiState, themeWallpaperPath: String?) =
        wallpapers.resolve(state.settings.wallpaperId, state.settings.profile, themeWallpaperPath)

    /** null = automatic (Iron Man heroes per profile), "theme" = theme photo, else a wallpaper id. */
    fun setWallpaper(id: String?, label: String) {
        updateSettings { it.copy(wallpaperId = id) }
        _messages.tryEmit("Home wallpaper: $label")
        goHome()
    }

    // ---- Smart HDMI Hub ----

    private val _hubPicker = MutableStateFlow<HubDevice?>(null)
    /** Device whose input is being changed in the Hub (shows the input picker), or null. */
    val hubPicker: StateFlow<HubDevice?> = _hubPicker.asStateFlow()

    /** The input the device will use: saved mapping, else what detection finds (not saved here). */
    fun hubInputFor(device: HubDevice, state: LauncherUiState = uiState.value): HdmiInput? {
        val inputs = state.hdmiInputs
        return when (device) {
            HubDevice.AIRTEL -> state.hdmiInput(state.settings.cableInputId)
                ?: HdmiDetection.detectSetTopBox(inputs, state.settings.playStationInputId)
            HubDevice.PS5 -> state.hdmiInput(state.settings.playStationInputId)
                ?: HdmiDetection.detectPlayStation(inputs)
        }
    }

    /** Which app would carry out the switch (diagnostics), e.g. "com.mitv.livetv". */
    fun inputAppFor(inputId: String): String? = hdmiRepository.inputAppFor(inputId)

    /** Same switch as the Home tile, but reports what happened. The app cannot see the picture. */
    fun testSwitch(device: HubDevice) {
        val input = hubInputFor(device)
        if (input == null) {
            _messages.tryEmit("No input found for ${device.title}. Use Change input.")
            return
        }
        if (hdmiRepository.openPassthroughInput(input.inputId)) {
            _messages.tryEmit("Switching requested to ${input.label} — check your TV.")
        } else {
            _messages.tryEmit("The TV did not accept the request. Press INPUT on the Xiaomi remote and choose ${input.label}.")
        }
    }

    fun changeHubInput(device: HubDevice) {
        _hubPicker.value = device
    }

    fun dismissHubPicker() {
        _hubPicker.value = null
    }

    /** Saves the chosen input for [device]. Persisted in DataStore (keys kept from v1). */
    fun pickHubInput(device: HubDevice, input: HdmiInput) {
        when (device) {
            HubDevice.AIRTEL -> updateSettings { it.copy(cableInputId = input.inputId) }
            HubDevice.PS5 -> updateSettings { it.copy(playStationInputId = input.inputId) }
        }
        _hubPicker.value = null
        _messages.tryEmit("${device.title} will use ${input.label}.")
    }

    /** No success message: the app cannot see the picture. On failure, give the manual remote steps. */
    private fun switchInput(inputId: String, deviceName: String) {
        if (!hdmiRepository.openPassthroughInput(inputId)) {
            val label = uiState.value.hdmiLabel(inputId) ?: "its HDMI input"
            _messages.tryEmit("Could not switch automatically. Press the INPUT (source) button on the Xiaomi remote and choose $label for $deviceName.")
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
