package com.arjunvision.tvlauncher.platform.screensaver

import android.service.dreams.DreamService
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.arjunvision.tvlauncher.data.preferences.DataStoreSettingsRepository
import com.arjunvision.tvlauncher.data.themes.ThemeRepository
import com.arjunvision.tvlauncher.data.wallpapers.WallpaperRepository
import com.arjunvision.tvlauncher.ui.screensaver.ScreensaverContent
import com.arjunvision.tvlauncher.ui.theme.ArjunVisionTvTheme
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle

/**
 * "Arjun Vision" in Android's own screensaver list (Settings > System > Screensaver), so the
 * ambient clock also shows when the TV idles in YouTube, Live TV or anywhere else.
 *
 * Android starts and stops this itself; nothing of ours runs while it isn't showing. It is not
 * interactive, so any remote key wakes the TV back to where it was. Brightness and the
 * "put TV to sleep" timer stay with Android's own settings.
 *
 * DreamService is not a LifecycleOwner, so it provides one (and a SavedStateRegistry) for Compose.
 */
class ArjunDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val settingsRepository = DataStoreSettingsRepository(this)
        val themes = ThemeRepository.get(this)
        val wallpapers = WallpaperRepository(this)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ArjunDreamService)
            setViewTreeSavedStateRegistryOwner(this@ArjunDreamService)
            setContent {
                val settings by settingsRepository.settings.collectAsState(initial = null)
                val themeState by themes.state.collectAsState()
                LaunchedEffect(themeState) { LauncherStyle.apply(themes.activeTheme(themeState)) }
                val slides = remember { wallpapers.slideshow.map { it.source } }
                ArjunVisionTvTheme {
                    settings?.let { s ->
                        LauncherStyle.lite = s.liteMode
                        ScreensaverContent(
                            settings = s,
                            slides = slides,
                            homeWallpaper = wallpapers.resolve(s.wallpaperId, s.profile, LauncherStyle.wallpaperPath)?.source,
                        )
                    }
                }
            }
        }
        // Compose also looks the owners up from the window's root view.
        window?.decorView?.let { root ->
            root.setViewTreeLifecycleOwner(this)
            root.setViewTreeSavedStateRegistryOwner(this)
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
