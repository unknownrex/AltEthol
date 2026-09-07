package com.unknownrex.altethol.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.settings.SettingsStorage
import com.unknownrex.altethol.feature.home.engine.EngineController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val pollIntervalMinutes: Int = SettingsStorage.DEFAULT_POLL_INTERVAL_MINUTES,
    val isLoggingOut: Boolean = false,
)

sealed interface SettingsAction {
    data class OnIntervalChanged(val minutes: Int) : SettingsAction
    data object OnLogout : SettingsAction
}

sealed interface SettingsEvent {
    data object NavigateToLogin : SettingsEvent
}

class SettingsViewModel(
    private val settingsStorage: SettingsStorage,
    private val sessionStorage: SessionStorage,
    private val engineController: EngineController,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsStorage.pollIntervalMinutes.collect { minutes ->
                _state.update { it.copy(pollIntervalMinutes = minutes) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnIntervalChanged -> changeInterval(action.minutes)
            SettingsAction.OnLogout -> logout()
        }
    }

    private fun changeInterval(minutes: Int) {
        _state.update { it.copy(pollIntervalMinutes = minutes) }
        viewModelScope.launch {
            settingsStorage.setPollIntervalMinutes(minutes)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoggingOut = true) }
            engineController.setEnabled(false)
            sessionStorage.clear()
            _events.send(SettingsEvent.NavigateToLogin)
        }
    }
}
