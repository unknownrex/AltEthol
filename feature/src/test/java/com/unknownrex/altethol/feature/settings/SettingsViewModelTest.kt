package com.unknownrex.altethol.feature.settings

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.unknownrex.altethol.core.data.session.SessionState
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.settings.SettingsStorage
import com.unknownrex.altethol.feature.home.engine.EngineController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSettingsStorage(
        initialInterval: Int = SettingsStorage.DEFAULT_POLL_INTERVAL_MINUTES,
    ) : SettingsStorage {
        private val _pollIntervalMinutes = MutableStateFlow(initialInterval)
        override val pollIntervalMinutes: Flow<Int> = _pollIntervalMinutes
        var lastSetInterval: Int? = null

        override suspend fun setPollIntervalMinutes(minutes: Int) {
            lastSetInterval = minutes
            _pollIntervalMinutes.value = minutes
        }
    }

    private class FakeSessionStorage : SessionStorage {
        var cleared = false
        private val _session = MutableStateFlow(SessionState(isLoggedIn = true))
        override val session: Flow<SessionState> = _session

        override suspend fun saveSession(token: String, refreshToken: String?) = Unit

        override suspend fun saveToken(token: String) = Unit

        override suspend fun saveMahasiswaId(id: Int) = Unit

        override suspend fun clear() {
            cleared = true
        }
    }

    private class FakeEngineController : EngineController {
        private val _enabled = MutableStateFlow(false)
        override val enabled: StateFlow<Boolean> = _enabled
        var lastSetEnabled: Boolean? = null

        override fun setEnabled(enabled: Boolean) {
            lastSetEnabled = enabled
            _enabled.value = enabled
        }
    }

    private fun viewModel(
        storage: FakeSettingsStorage = FakeSettingsStorage(),
        session: FakeSessionStorage = FakeSessionStorage(),
        controller: FakeEngineController = FakeEngineController(),
    ) = SettingsViewModel(storage, session, controller)

    @Test
    fun `initial state reflects stored interval`() {
        val viewModel = viewModel(storage = FakeSettingsStorage(initialInterval = 10))

        assertThat(viewModel.state.value.pollIntervalMinutes).isEqualTo(10)
    }

    @Test
    fun `changing interval delegates to storage`() {
        val storage = FakeSettingsStorage()
        val viewModel = viewModel(storage = storage)

        viewModel.onAction(SettingsAction.OnIntervalChanged(15))

        assertThat(storage.lastSetInterval).isEqualTo(15)
        assertThat(viewModel.state.value.pollIntervalMinutes).isEqualTo(15)
    }

    @Test
    fun `logout stops engine clears session and navigates to login`() = runTest {
        val storage = FakeSettingsStorage()
        val session = FakeSessionStorage()
        val controller = FakeEngineController()
        val viewModel = viewModel(storage = storage, session = session, controller = controller)

        viewModel.events.test {
            viewModel.onAction(SettingsAction.OnLogout)

            assertThat(awaitItem()).isEqualTo(SettingsEvent.NavigateToLogin)
        }

        assertThat(controller.lastSetEnabled).isEqualTo(false)
        assertThat(session.cleared).isTrue()
    }
}
