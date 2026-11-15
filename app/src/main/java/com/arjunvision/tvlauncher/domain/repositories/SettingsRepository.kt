package com.arjunvision.tvlauncher.domain.repositories

import com.arjunvision.tvlauncher.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    /** Atomically reads the current settings, applies [transform] and saves the result. */
    suspend fun update(transform: (AppSettings) -> AppSettings)

    /** Clears every saved setting, so the defaults apply again. */
    suspend fun reset()
}
