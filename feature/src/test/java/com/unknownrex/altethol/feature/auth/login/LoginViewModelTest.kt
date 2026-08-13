package com.unknownrex.altethol.feature.auth.login

import app.cash.turbine.test
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
import assertk.assertions.isNotNull
import assertk.assertions.isNull

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

    private class FakeSessionStorage : SessionStorage {
        val sessionFlow = MutableStateFlow(SessionState())
        var savedToken: String? = null
        var savedCookie: String? = null
        var savedMahasiswaId: Int? = null
        var clearCalled = false
        var throwOnSave = false

        override val session: Flow<SessionState> = sessionFlow

        override suspend fun saveSession(token: String, cookie: String?) {
            if (throwOnSave) error("cipher failure")
            savedToken = token
            savedCookie = cookie
        }

        override suspend fun saveMahasiswaId(id: Int) {
            savedMahasiswaId = id
        }

        override suspend fun clear() {
            clearCalled = true
        }
    }

    @Test
    fun `successful login saves token and cookie then navigates home`() = runTest {
        val storage = FakeSessionStorage()
        val viewModel = LoginViewModel(storage)

        viewModel.events.test {
            viewModel.onAction(
                LoginAction.OnLoginSuccess("token=jwt-token; PHPSESSID=session-cookie"),
            )

            assertThat(awaitItem()).isEqualTo(LoginEvent.NavigateToHome)
        }

        assertThat(storage.savedToken).isEqualTo("jwt-token")
        assertThat(storage.savedCookie).isEqualTo("session-cookie")
    }

    @Test
    fun `login without token cookie still navigates home`() = runTest {
        val storage = FakeSessionStorage()
        val viewModel = LoginViewModel(storage)

        viewModel.events.test {
            viewModel.onAction(LoginAction.OnLoginSuccess("PHPSESSID=only"))

            assertThat(awaitItem()).isEqualTo(LoginEvent.NavigateToHome)
        }

        assertThat(storage.savedToken).isNull()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `storage failure does not block navigation`() = runTest {
        val storage = FakeSessionStorage().apply { throwOnSave = true }
        val viewModel = LoginViewModel(storage)

        viewModel.events.test {
            viewModel.onAction(LoginAction.OnLoginSuccess("token=jwt-token"))

            assertThat(awaitItem()).isEqualTo(LoginEvent.NavigateToHome)
        }
    }
}
