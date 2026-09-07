package com.unknownrex.altethol.feature.auth.login

import app.cash.turbine.test
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.remote.dto.ValidasiTokenDto
import com.unknownrex.altethol.core.data.session.SessionState
import com.unknownrex.altethol.core.data.session.SessionStorage
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
import assertk.assertions.isNull
import assertk.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

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
    ) : AuthRepository {
        var validateTokenCalled = false

        override suspend fun validateToken(): Result<ValidasiTokenDto, DataError.Network> {
            validateTokenCalled = true
            return result
        }

        override suspend fun refreshToken(): Result<String, DataError.Network> =
            Result.Success("new-token")
    }

    private class FakeSessionStorage : SessionStorage {
        val sessionFlow = MutableStateFlow(SessionState())
        var savedToken: String? = null
        var savedRefreshToken: String? = null
        var savedMahasiswaId: Int? = null
        var clearCalled = false
        var throwOnSave = false

        override val session: Flow<SessionState> = sessionFlow

        override suspend fun saveSession(token: String, refreshToken: String?) {
            if (throwOnSave) error("cipher failure")
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

    @Test
    fun `successful login saves session validates token and navigates home`() = runTest {
        val storage = FakeSessionStorage()
        val auth = FakeAuthRepository(Result.Success(validToken))
        val viewModel = LoginViewModel(storage, auth)

        viewModel.events.test {
            viewModel.onAction(
                LoginAction.OnLoginSuccess(
                    "token=jwt-token; refresh_token=refresh-xyz; PHPSESSID=session-cookie",
                ),
            )

            assertThat(awaitItem()).isEqualTo(LoginEvent.NavigateToHome)
        }

        assertThat(storage.savedToken).isEqualTo("jwt-token")
        assertThat(storage.savedRefreshToken).isEqualTo("refresh-xyz")
        assertThat(storage.savedMahasiswaId).isEqualTo(28801)
        assertThat(auth.validateTokenCalled).isTrue()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `login without token cookie stays on login`() = runTest {
        val storage = FakeSessionStorage()
        val auth = FakeAuthRepository(Result.Success(validToken))
        val viewModel = LoginViewModel(storage, auth)

        viewModel.onAction(LoginAction.OnLoginSuccess("PHPSESSID=only"))

        assertThat(storage.savedToken).isNull()
        assertThat(auth.validateTokenCalled).isFalse()
        assertThat(viewModel.state.value.isSaving).isFalse()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `invalid token clears session shows error and does not navigate`() = runTest {
        val storage = FakeSessionStorage()
        val auth = FakeAuthRepository(Result.Error(DataError.Network.UNAUTHORIZED))
        val viewModel = LoginViewModel(storage, auth)

        viewModel.onAction(LoginAction.OnLoginSuccess("token=jwt-token"))

        assertThat(storage.savedToken).isEqualTo("jwt-token")
        assertThat(storage.clearCalled).isTrue()
        assertThat(viewModel.state.value.isSaving).isFalse()
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `network failure shows error and does not navigate nor clear session`() = runTest {
        val storage = FakeSessionStorage()
        val auth = FakeAuthRepository(Result.Error(DataError.Network.NO_INTERNET))
        val viewModel = LoginViewModel(storage, auth)

        viewModel.onAction(LoginAction.OnLoginSuccess("token=jwt-token"))

        assertThat(storage.clearCalled).isFalse()
        assertThat(viewModel.state.value.isSaving).isFalse()
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `storage failure does not block validation and navigation`() = runTest {
        val storage = FakeSessionStorage().apply { throwOnSave = true }
        val auth = FakeAuthRepository(Result.Success(validToken))
        val viewModel = LoginViewModel(storage, auth)

        viewModel.events.test {
            viewModel.onAction(LoginAction.OnLoginSuccess("token=jwt-token"))

            assertThat(awaitItem()).isEqualTo(LoginEvent.NavigateToHome)
        }

        assertThat(auth.validateTokenCalled).isTrue()
    }
}