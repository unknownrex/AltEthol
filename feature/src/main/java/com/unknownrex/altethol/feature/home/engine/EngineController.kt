package com.unknownrex.altethol.feature.home.engine

import kotlinx.coroutines.flow.StateFlow

interface EngineController {
    val enabled: StateFlow<Boolean>

    fun setEnabled(enabled: Boolean)
}
