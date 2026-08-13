package com.unknownrex.altethol.feature.auth.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.ui.text.UiText
import com.unknownrex.altethol.feature.auth.CookieParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val isSaving: Boolean = false,
    val reloadTrigger: Int = 0,
    val error: UiText? = null,
)

sealed interface LoginAction {
    data class OnLoginSuccess(val cookieString: String) : LoginAction
    data object OnReload : LoginAction
}

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
}

class LoginViewModel(
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnLoginSuccess -> handleLoginSuccess(action.cookieString)
            LoginAction.OnReload -> _state.update {
                it.copy(error = null, reloadTrigger = it.reloadTrigger + 1)
            }
        }
    }

    private fun handleLoginSuccess(cookieString: String) {
        val token = CookieParser.extractToken(cookieString)
        val phpSessId = CookieParser.extractPhpSessId(cookieString)
        Log.d(TAG, "Login success. token=${token?.take(16)} phpSessId=${phpSessId?.take(16)}")

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            persistSessionBestEffort(token, phpSessId)
            _events.send(LoginEvent.NavigateToHome)
        }
    }

    private suspend fun persistSessionBestEffort(token: String?, phpSessId: String?) {
        if (token.isNullOrBlank()) {
            Log.d(TAG, "No token cookie found, skipping session persistence")
            return
        }
        runCatching {
            sessionStorage.saveSession(token, phpSessId)
        }.onSuccess {
            Log.d(TAG, "Session persisted")
        }.onFailure {
            Log.e(TAG, "Session persistence failed", it)
        }
    }

    private companion object {
        const val TAG = "AltEtholLogin"
    }
}
