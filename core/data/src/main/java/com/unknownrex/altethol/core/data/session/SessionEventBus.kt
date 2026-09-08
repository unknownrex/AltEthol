package com.unknownrex.altethol.core.data.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEventBus {
    private val _sessionExpired = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val sessionExpired: Flow<Unit> = _sessionExpired.asSharedFlow()

    fun emit() {
        _sessionExpired.tryEmit(Unit)
    }

    fun clear() {
        _sessionExpired.resetReplayCache()
    }
}