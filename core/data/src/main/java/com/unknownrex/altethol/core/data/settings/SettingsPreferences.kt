package com.unknownrex.altethol.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsPreferences(
    private val dataStore: DataStore<Preferences>,
) : SettingsStorage {

    override val pollIntervalMinutes: Flow<Int> = dataStore.data.map {
        it[Keys.POLL_INTERVAL_MINUTES]
            ?: SettingsStorage.DEFAULT_POLL_INTERVAL_MINUTES
    }

    override suspend fun setPollIntervalMinutes(minutes: Int) {
        val clamped = minutes.coerceAtLeast(SettingsStorage.MIN_POLL_INTERVAL_MINUTES)
        dataStore.edit { prefs ->
            prefs[Keys.POLL_INTERVAL_MINUTES] = clamped
        }
    }

    private object Keys {
        val POLL_INTERVAL_MINUTES = intPreferencesKey("poll_interval_minutes")
    }
}
