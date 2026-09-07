package com.unknownrex.altethol.core.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsStorage {
    val pollIntervalMinutes: Flow<Int>

    suspend fun setPollIntervalMinutes(minutes: Int)

    companion object {
        const val DEFAULT_POLL_INTERVAL_MINUTES = 5
        const val MIN_POLL_INTERVAL_MINUTES = 3
    }
}
