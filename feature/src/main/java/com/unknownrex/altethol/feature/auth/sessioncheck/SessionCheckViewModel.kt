package com.unknownrex.altethol.feature.auth.sessioncheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.ui.text.toUiText
import com.unknownrex.altethol.core.common.result.onFailure
import com.unknownrex.altethol.core.common.result.onSuccess
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.session.SessionRefreshResult
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.session.TokenRefresher
import com.unknownrex.altethol.core.ui.text.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SessionCheckState {
    data object Checking : SessionCheckState
    data class Error(val message: UiText) : SessionCheckState
}

sealed interface SessionCheckAction {
    data object Retry : SessionCheckAction
}

sealed interface SessionCheckEvent {
    data object NavigateToLogin : SessionCheckEvent
    data object NavigateToHome : SessionCheckEvent
}

class SessionCheckViewModel(
    private val authRepository: AuthRepository,
    private val sessionStorage: SessionStorage,
    private val tokenRefresher: TokenRefresher,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionCheckState>(SessionCheckState.Checking)
    val state = _state.asStateFlow()

    private val _events = Channel<SessionCheckEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        runSessionCheck()
    }

    fun onAction(action: SessionCheckAction) {
        when (action) {
            SessionCheckAction.Retry -> runSessionCheck()
        }
    }

    private fun runSessionCheck() {
        viewModelScope.launch {
            _state.update { SessionCheckState.Checking }

            val session = sessionStorage.session.first()
            if (!session.hasSession) {
                _events.send(SessionCheckEvent.NavigateToLogin)
                return@launch
            }

            when (val refreshResult = tokenRefresher.refreshIfNeeded()) {
                SessionRefreshResult.SESSION_EXPIRED -> {
                    sessionStorage.clear()
                    _events.send(SessionCheckEvent.NavigateToLogin)
                    return@launch
                }

                SessionRefreshResult.ERROR -> {
                    _state.update {
                        SessionCheckState.Error(DataError.Network.UNKNOWN.toUiText())
                    }
                    return@launch
                }

                SessionRefreshResult.REFRESHED,
                SessionRefreshResult.ALREADY_FRESH,
                -> Unit
            }

            authRepository.validateToken()
                .onSuccess { data ->
                    sessionStorage.saveMahasiswaId(data.nomor)
                    _events.send(SessionCheckEvent.NavigateToHome)
                }
                .onFailure { error ->
                    when (error) {
                        DataError.Network.UNAUTHORIZED,
                        DataError.Network.FORBIDDEN,
                        -> {
                            sessionStorage.clear()
                            _events.send(SessionCheckEvent.NavigateToLogin)
                        }

                        else -> _state.update { SessionCheckState.Error(error.toUiText()) }
                    }
                }
        }
    }
}
