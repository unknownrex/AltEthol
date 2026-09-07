package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.remote.dto.ValidasiTokenDto
import com.unknownrex.altethol.core.data.session.SessionRefreshResult
import com.unknownrex.altethol.core.data.session.SessionState
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.session.TokenRefresher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo

class TokenRefresherTest {

    private class FakeSessionStorage(
        initialState: SessionState,
    ) : SessionStorage {
        val sessionFlow = MutableStateFlow(initialState)
        var savedToken: String? = null

        override val session: Flow<SessionState> = sessionFlow

        override suspend fun saveSession(token: String, refreshToken: String?) = Unit

        override suspend fun saveToken(token: String) {
            savedToken = token
            sessionFlow.value = sessionFlow.value.copy(token = token)
        }

        override suspend fun saveMahasiswaId(id: Int) = Unit

        override suspend fun clear() = Unit
    }

    private class FakeAuthRepository(
        var result: Result<String, DataError.Network>,
    ) : AuthRepository {
        var refreshCalls = 0
        var delayMillis = 0L

        override suspend fun validateToken(): Result<ValidasiTokenDto, DataError.Network> =
            Result.Success(ValidasiTokenDto(nomor = 1, nipnrp = "n", nama = "n"))

        override suspend fun refreshToken(): Result<String, DataError.Network> {
            refreshCalls++
            if (delayMillis > 0) delay(delayMillis)
            return result
        }
    }

    private fun loggedInSession(
        token: String = "stale-token",
        refreshToken: String? = "refresh-token",
    ) = SessionState(isLoggedIn = true, token = token, refreshToken = refreshToken)

    private fun refresher(
        storage: FakeSessionStorage,
        auth: FakeAuthRepository,
        jwtExpiration: (String?) -> Long?,
        now: () -> Long = { 0L },
    ) = TokenRefresher(
        sessionStorage = storage,
        authRepository = auth,
        jwtExpiration = jwtExpiration,
        now = now,
    )

    @Test
    fun `does not refresh when token is still fresh`() = runTest {
        val storage = FakeSessionStorage(loggedInSession(token = "fresh-token"))
        val auth = FakeAuthRepository(Result.Success("new-token"))
        val refresher = refresher(storage, auth, jwtExpiration = { 4_000_000_000L })

        val result = refresher.refreshIfNeeded()

        assertThat(result).isEqualTo(SessionRefreshResult.ALREADY_FRESH)
        assertThat(auth.refreshCalls).isEqualTo(0)
    }

    @Test
    fun `refreshes within expiry buffer and saves new token`() = runTest {
        val storage = FakeSessionStorage(loggedInSession())
        val auth = FakeAuthRepository(Result.Success("new-token"))
        val refresher = refresher(storage, auth, jwtExpiration = { 60L })

        val result = refresher.refreshIfNeeded()

        assertThat(result).isEqualTo(SessionRefreshResult.REFRESHED)
        assertThat(storage.savedToken).isEqualTo("new-token")
        assertThat(auth.refreshCalls).isEqualTo(1)
    }

    @Test
    fun `refreshes when token exp cannot be decoded`() = runTest {
        val storage = FakeSessionStorage(loggedInSession(token = "not-a-jwt"))
        val auth = FakeAuthRepository(Result.Success("new-token"))
        val refresher = refresher(storage, auth, jwtExpiration = { null })

        val result = refresher.refreshIfNeeded()

        assertThat(result).isEqualTo(SessionRefreshResult.REFRESHED)
        assertThat(auth.refreshCalls).isEqualTo(1)
    }

    @Test
    fun `returns session expired when no refresh token available`() = runTest {
        val storage = FakeSessionStorage(loggedInSession(refreshToken = null))
        val auth = FakeAuthRepository(Result.Success("new-token"))
        val refresher = refresher(storage, auth, jwtExpiration = { 60L })

        val result = refresher.refreshIfNeeded()

        assertThat(result).isEqualTo(SessionRefreshResult.SESSION_EXPIRED)
        assertThat(auth.refreshCalls).isEqualTo(0)
    }

    @Test
    fun `returns session expired when refresh is unauthorized`() = runTest {
        val storage = FakeSessionStorage(loggedInSession())
        val auth = FakeAuthRepository(Result.Error(DataError.Network.UNAUTHORIZED))
        val refresher = refresher(storage, auth, jwtExpiration = { 60L })

        val result = refresher.refreshIfNeeded()

        assertThat(result).isEqualTo(SessionRefreshResult.SESSION_EXPIRED)
    }

    @Test
    fun `returns error for retryable refresh failures`() = runTest {
        val storage = FakeSessionStorage(loggedInSession())
        val auth = FakeAuthRepository(Result.Error(DataError.Network.NO_INTERNET))
        val refresher = refresher(storage, auth, jwtExpiration = { 60L })

        val result = refresher.refreshIfNeeded()

        assertThat(result).isEqualTo(SessionRefreshResult.ERROR)
    }

    @Test
    fun `concurrent callers refresh only once`() = runTest {
        val storage = FakeSessionStorage(loggedInSession())
        val auth = FakeAuthRepository(Result.Success("fresh-token")).apply {
            delayMillis = 50L
        }
        val refresher = refresher(
            storage,
            auth,
            jwtExpiration = { if (it == "fresh-token") 4_000_000_000L else 1L },
        )

        val first = async { refresher.refreshIfNeeded() }
        val second = async { refresher.refreshIfNeeded() }
        val results = listOf(first.await(), second.await())

        assertThat(auth.refreshCalls).isEqualTo(1)
        assertThat(results).containsExactlyInAnyOrder(
            SessionRefreshResult.REFRESHED,
            SessionRefreshResult.ALREADY_FRESH,
        )
    }
}