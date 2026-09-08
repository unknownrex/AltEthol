package com.unknownrex.altethol.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknownrex.altethol.core.data.session.SessionEventBus
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.settings.SettingsStorage
import com.unknownrex.altethol.core.ui.text.UiText
import com.unknownrex.altethol.feature.R
import com.unknownrex.altethol.feature.home.engine.EngineController
import com.unknownrex.altethol.feature.home.engine.EngineTimeState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val engineEnabled: Boolean = false,
    val pollIntervalMinutes: Int = SettingsStorage.DEFAULT_POLL_INTERVAL_MINUTES,
    val nextSyncAtEpochMillis: Long? = null,
)

sealed interface HomeAction {
    data class OnToggleEngine(val enabled: Boolean) : HomeAction
    data object OnAbsenNow : HomeAction
}

sealed interface HomeEvent {
    data class ShowMessage(val message: UiText) : HomeEvent
    data object ShowSessionExpired : HomeEvent
}

class HomeViewModel(
    private val engineController: EngineController,
    private val settingsStorage: SettingsStorage,
    private val engineTimeState: EngineTimeState,
    private val sessionEventBus: SessionEventBus,
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState(engineController.enabled.value))
    val state = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            engineController.enabled.collect { enabled ->
                _state.update { it.copy(engineEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsStorage.pollIntervalMinutes.collect { minutes ->
                _state.update { it.copy(pollIntervalMinutes = minutes) }
            }
        }
        viewModelScope.launch {
            engineTimeState.nextSyncAtEpochMillis.collect { epochMillis ->
                _state.update { it.copy(nextSyncAtEpochMillis = epochMillis) }
            }
        }
        viewModelScope.launch {
            sessionEventBus.sessionExpired.collect {
                engineController.setEnabled(false)
                sessionStorage.clear()
                _events.send(HomeEvent.ShowSessionExpired)
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnToggleEngine -> engineController.setEnabled(action.enabled)
            HomeAction.OnAbsenNow -> viewModelScope.launch {
                _events.send(HomeEvent.ShowMessage(UiText.StringResource(R.string.absen_now_coming_soon)))
            }
        }
    }
}
