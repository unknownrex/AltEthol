package com.unknownrex.altethol.feature.auth.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.onFailure
import com.unknownrex.altethol.core.common.result.onSuccess
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.ui.text.UiText
import com.unknownrex.altethol.core.ui.text.toUiText
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
    private val authRepository: AuthRepository,
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
        val refreshToken = CookieParser.extractRefreshToken(cookieString)
        Log.d(TAG, "Login success. token=${token?.take(16)} refreshToken=${refreshToken?.take(16)}")

        if (token.isNullOrBlank()) {
            Log.d(TAG, "No token cookie found, staying on login")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            persistSessionBestEffort(token, refreshToken)

            authRepository.validateToken().onSuccess { data ->
                Log.d(TAG, "Validation OK, nomor=${data.nomor}")
                sessionStorage.saveMahasiswaId(data.nomor)
                _events.send(LoginEvent.NavigateToHome)
            }.onFailure { error ->
                Log.e(TAG, "Validation failed: $error")
                when (error) {
                    DataError.Network.UNAUTHORIZED,
                    DataError.Network.FORBIDDEN,
                    -> sessionStorage.clear()

                    else -> Unit
                }
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = error.toUiText(),
                    )
                }
            }
        }
    }

    private suspend fun persistSessionBestEffort(token: String?, refreshToken: String?) {
        if (token.isNullOrBlank()) {
            Log.d(TAG, "No token cookie found, skipping session persistence")
            return
        }
        runCatching {
            sessionStorage.saveSession(token, refreshToken)
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
