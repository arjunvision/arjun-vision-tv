package com.arjunvision.tvlauncher.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arjunvision.tvlauncher.domain.models.AnimationLevel
import com.arjunvision.tvlauncher.domain.models.AppSettings
import com.arjunvision.tvlauncher.domain.models.CardSize
import com.arjunvision.tvlauncher.domain.models.Profile
import com.arjunvision.tvlauncher.domain.repositories.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore by preferencesDataStore(name = "arjun_vision_tv_settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    private object Keys {
        val PROFILE = stringPreferencesKey("profile")
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        val CARD_SIZE = stringPreferencesKey("card_size")
        val ANIMATION = stringPreferencesKey("animation")
        val STARTUP_ANIMATION = booleanPreferencesKey("startup_animation")
        val CABLE_INPUT = stringPreferencesKey("cable_input_id")
        val PLAYSTATION_INPUT = stringPreferencesKey("playstation_input_id")
        val LITE_MODE = booleanPreferencesKey("lite_mode")
        val SCREENSAVER_MINUTES = intPreferencesKey("screensaver_minutes")
        val HIDDEN = stringSetPreferencesKey("hidden_packages")
        val FAVOURITES = stringPreferencesKey("favourites")
        val WALLPAPER = stringPreferencesKey("wallpaper_id")
    }

    override val settings: Flow<AppSettings> = context.launcherDataStore.data
        .catch { error ->
            // A corrupt or unreadable file falls back to defaults instead of crashing the launcher.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.launcherDataStore.edit { prefs ->
            prefs.writeSettings(transform(prefs.toSettings()))
        }
    }

    override suspend fun reset() {
        context.launcherDataStore.edit { it.clear() }
    }

    private fun Preferences.toSettings() = AppSettings(
        profile = enumOrDefault(this[Keys.PROFILE], Profile.FATHER),
        clock24h = this[Keys.CLOCK_24H] ?: false,
        cardSize = enumOrDefault(this[Keys.CARD_SIZE], CardSize.STANDARD),
        animation = enumOrDefault(this[Keys.ANIMATION], AnimationLevel.NORMAL),
        startupAnimation = this[Keys.STARTUP_ANIMATION] ?: true,
        cableInputId = this[Keys.CABLE_INPUT],
        playStationInputId = this[Keys.PLAYSTATION_INPUT],
        liteMode = this[Keys.LITE_MODE] ?: false,
        screensaverMinutes = this[Keys.SCREENSAVER_MINUTES] ?: 5,
        hiddenPackages = this[Keys.HIDDEN] ?: emptySet(),
        favourites = this[Keys.FAVOURITES]?.split(',')?.filter { it.isNotBlank() },
        wallpaperId = this[Keys.WALLPAPER],
    )

    private fun MutablePreferences.writeSettings(settings: AppSettings) {
        this[Keys.PROFILE] = settings.profile.name
        this[Keys.CLOCK_24H] = settings.clock24h
        this[Keys.CARD_SIZE] = settings.cardSize.name
        this[Keys.ANIMATION] = settings.animation.name
        this[Keys.STARTUP_ANIMATION] = settings.startupAnimation
        writeOptional(Keys.CABLE_INPUT, settings.cableInputId)
        writeOptional(Keys.PLAYSTATION_INPUT, settings.playStationInputId)
        this[Keys.LITE_MODE] = settings.liteMode
        this[Keys.SCREENSAVER_MINUTES] = settings.screensaverMinutes
        this[Keys.HIDDEN] = settings.hiddenPackages
        writeOptional(Keys.FAVOURITES, settings.favourites?.joinToString(","))
        writeOptional(Keys.WALLPAPER, settings.wallpaperId)
    }

    private fun MutablePreferences.writeOptional(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
