package com.unknownrex.altethol.feature.home.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EngineTimeState {
    private val _nextSyncAtEpochMillis = MutableStateFlow<Long?>(null)
    val nextSyncAtEpochMillis: StateFlow<Long?> = _nextSyncAtEpochMillis.asStateFlow()

    fun updateNextSync(epochMillis: Long?) {
        _nextSyncAtEpochMillis.value = epochMillis
    }
}