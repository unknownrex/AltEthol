package com.unknownrex.altethol.feature.auth.sessioncheck

import app.cash.turbine.test
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.remote.dto.ValidasiTokenDto
import com.unknownrex.altethol.core.data.session.SessionState
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.session.TokenRefresher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCheckViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAuthRepository(
        var result: Result<ValidasiTokenDto, DataError.Network>,
        var refreshResult: Result<String, DataError.Network> = Result.Success("new-token"),
    ) : AuthRepository {
        override suspend fun validateToken(): Result<ValidasiTokenDto, DataError.Network> = result

        override suspend fun refreshToken(): Result<String, DataError.Network> = refreshResult
    }

    private class FakeSessionStorage(
        initialState: SessionState = SessionState(),
    ) : SessionStorage {
        val sessionFlow = MutableStateFlow(initialState)
        var savedToken: String? = null
        var savedRefreshToken: String? = null
        var savedMahasiswaId: Int? = null
        var clearCalled = false

        override val session: Flow<SessionState> = sessionFlow

        override suspend fun saveSession(token: String, refreshToken: String?) {
            savedToken = token
            savedRefreshToken = refreshToken
        }

        override suspend fun saveToken(token: String) {
            savedToken = token
        }

        override suspend fun saveMahasiswaId(id: Int) {
            savedMahasiswaId = id
        }

        override suspend fun clear() {
            clearCalled = true
        }
    }

    private val validToken = ValidasiTokenDto(
        nomor = 28801,
        nipnrp = "3122500019",
        nama = "Rey",
    )

    private fun loggedInSession() = SessionState(
        isLoggedIn = true,
        token = "jwt-token",
        refreshToken = "refresh-token",
        mahasiswaId = null,
    )

    private fun freshRefresher(storage: SessionStorage): TokenRefresher = TokenRefresher(
        sessionStorage = storage,
        authRepository = FakeAuthRepository(
            Result.Success(validToken),
            Result.Success("new-token"),
        ),
        jwtExpiration = { 4_000_000_000L },
        now = { 0L },
    )

    private fun viewModel(
        auth: FakeAuthRepository,
        storage: FakeSessionStorage,
        refresher: TokenRefresher,
    ) = SessionCheckViewModel(auth, storage, refresher)

    @Test
    fun `no session routes to login`() = runTest {
        val storage = FakeSessionStorage()
        val viewModel = viewModel(
            FakeAuthRepository(Result.Success(validToken)),
            storage,
            freshRefresher(storage),
        )

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(SessionCheckEvent.NavigateToLogin)
        }
    }

    @Test
    fun `valid session validates token and routes to home`() = runTest {
        val storage = FakeSessionStorage(initialState = loggedInSession())
        val viewModel = viewModel(
            FakeAuthRepository(Result.Success(validToken)),
            storage,
            freshRefresher(storage),
        )

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(SessionCheckEvent.NavigateToHome)
        }

        assertThat(storage.savedMahasiswaId).isEqualTo(28801)
    }

    @Test
    fun `unauthorized token clears session and routes to login`() = runTest {
        val storage = FakeSessionStorage(initialState = loggedInSession())
        val viewModel = viewModel(
            FakeAuthRepository(Result.Error(DataError.Network.UNAUTHORIZED)),
            storage,
            freshRefresher(storage),
        )

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(SessionCheckEvent.NavigateToLogin)
        }

        assertThat(storage.clearCalled).isTrue()
    }

    @Test
    fun `forbidden token clears session and routes to login`() = runTest {
        val storage = FakeSessionStorage(initialState = loggedInSession())
        val viewModel = viewModel(
            FakeAuthRepository(Result.Error(DataError.Network.FORBIDDEN)),
            storage,
            freshRefresher(storage),
        )

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(SessionCheckEvent.NavigateToLogin)
        }

        assertThat(storage.clearCalled).isTrue()
    }

    @Test
    fun `network error shows error state without clearing`() = runTest {
        val storage = FakeSessionStorage(initialState = loggedInSession())
        val viewModel = viewModel(
            FakeAuthRepository(Result.Error(DataError.Network.NO_INTERNET)),
            storage,
            freshRefresher(storage),
        )

        assertThat(viewModel.state.value).isNotNull()
        assertThat(viewModel.state.value is SessionCheckState.Error).isTrue()
        assertThat(storage.clearCalled).isFalse()
    }

    @Test
    fun `retry after error can recover`() = runTest {
        val repository = FakeAuthRepository(Result.Error(DataError.Network.NO_INTERNET))
        val storage = FakeSessionStorage(initialState = loggedInSession())
        val viewModel = viewModel(repository, storage, freshRefresher(storage))

        assertThat(viewModel.state.value is SessionCheckState.Error).isTrue()

        repository.result = Result.Success(validToken)
        viewModel.onAction(SessionCheckAction.Retry)

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(SessionCheckEvent.NavigateToHome)
        }
        assertThat(storage.savedMahasiswaId).isEqualTo(28801)
    }

    @Test
    fun `expired token refresh failure clears session and routes to login`() = runTest {
        val storage = FakeSessionStorage(initialState = loggedInSession())
        val auth = FakeAuthRepository(
            result = Result.Success(validToken),
            refreshResult = Result.Error(DataError.Network.UNAUTHORIZED),
        )
        val stalledRefresher = TokenRefresher(
            sessionStorage = storage,
            authRepository = auth,
            jwtExpiration = { 1L },
            now = { 0L },
        )
        val viewModel = viewModel(auth, storage, stalledRefresher)

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(SessionCheckEvent.NavigateToLogin)
        }

        assertThat(storage.clearCalled).isTrue()
    }
}
