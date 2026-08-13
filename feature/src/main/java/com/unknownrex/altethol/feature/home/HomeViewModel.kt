package com.unknownrex.altethol.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknownrex.altethol.core.ui.text.UiText
import com.unknownrex.altethol.feature.R
import com.unknownrex.altethol.feature.home.engine.EngineController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val engineEnabled: Boolean = false,
)

sealed interface HomeAction {
    data class OnToggleEngine(val enabled: Boolean) : HomeAction
    data object OnAbsenNow : HomeAction
}

sealed interface HomeEvent {
    data class ShowMessage(val message: UiText) : HomeEvent
}

class HomeViewModel(
    private val engineController: EngineController,
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
