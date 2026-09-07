package com.unknownrex.altethol.core.data.session

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.jwt.JwtExpiration
import com.unknownrex.altethol.core.data.remote.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SessionRefreshResult {
    REFRESHED,
    ALREADY_FRESH,
    SESSION_EXPIRED,
    ERROR,
}

/**
 * Preemptively refreshes the short-lived token before it expires (default: 2 minute buffer).
 * Uses the stored refresh_token against `POST /auth/refresh`. Single-flight so concurrent
 * callers (session check + background poll) never fire parallel refreshes.
 */
class TokenRefresher(
    private val sessionStorage: SessionStorage,
    private val authRepository: AuthRepository,
    private val jwtExpiration: (String?) -> Long? = JwtExpiration::expEpochSeconds,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val minRemainingMillis: Long = DEFAULT_MIN_REMAINING_MILLIS,
) {

    private val mutex = Mutex()

    suspend fun refreshIfNeeded(): SessionRefreshResult = mutex.withLock {
        val session = sessionStorage.session.first()
        when {
            !session.hasSession -> SessionRefreshResult.SESSION_EXPIRED

            isFreshEnough(session.token) -> SessionRefreshResult.ALREADY_FRESH

            !session.hasRefreshToken -> SessionRefreshResult.SESSION_EXPIRED

            else -> refresh(session)
        }
    }

    private fun isFreshEnough(token: String?): Boolean {
        val exp = jwtExpiration(token) ?: return false
        return exp * 1000L - now() > minRemainingMillis
    }

    private suspend fun refresh(session: SessionState): SessionRefreshResult {
        return when (val result = authRepository.refreshToken()) {
            is Result.Success -> {
                sessionStorage.saveToken(result.data)
                SessionRefreshResult.REFRESHED
            }

            is Result.Error -> when (result.error) {
                DataError.Network.UNAUTHORIZED,
                DataError.Network.FORBIDDEN,
                -> SessionRefreshResult.SESSION_EXPIRED

                else -> SessionRefreshResult.ERROR
            }
        }
    }

    private companion object {
        const val DEFAULT_MIN_REMAINING_MILLIS = 2 * 60 * 1000L
    }
}