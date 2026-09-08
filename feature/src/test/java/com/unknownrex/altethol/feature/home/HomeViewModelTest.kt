package com.unknownrex.altethol.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.unknownrex.altethol.core.data.session.SessionEventBus
import com.unknownrex.altethol.core.data.session.SessionState
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.settings.SettingsStorage
import com.unknownrex.altethol.feature.home.engine.EngineController
import com.unknownrex.altethol.feature.home.engine.EngineTimeState
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
class HomeViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeEngineController(
        initialEnabled: Boolean = false,
    ) : EngineController {
        private val _enabled = MutableStateFlow(initialEnabled)
        override val enabled: StateFlow<Boolean> = _enabled
        var lastSetEnabled: Boolean? = null

        override fun setEnabled(enabled: Boolean) {
            lastSetEnabled = enabled
            _enabled.value = enabled
        }
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

    private class FakeSessionStorage(
        initialState: SessionState = SessionState(),
    ) : SessionStorage {
        private val _session = MutableStateFlow(initialState)
        override val session: Flow<SessionState> = _session
        var cleared = false

        override suspend fun saveSession(token: String, refreshToken: String?) = Unit

        override suspend fun saveToken(token: String) = Unit

        override suspend fun saveMahasiswaId(id: Int) = Unit

        override suspend fun clear() {
            cleared = true
            _session.value = SessionState()
        }
    }

    private fun viewModel(
        controller: FakeEngineController = FakeEngineController(),
        settings: FakeSettingsStorage = FakeSettingsStorage(),
        timeState: EngineTimeState = EngineTimeState(),
        eventBus: SessionEventBus = SessionEventBus(),
        sessionStorage: FakeSessionStorage = FakeSessionStorage(),
    ) = HomeViewModel(controller, settings, timeState, eventBus, sessionStorage)

    @Test
    fun `initial state reflects controller`() {
        val viewModel = viewModel(controller = FakeEngineController(initialEnabled = true))

        assertThat(viewModel.state.value.engineEnabled).isEqualTo(true)
    }

    @Test
    fun `session expired event stops engine clears session and emits ShowSessionExpired`() = runTest {
        val controller = FakeEngineController(initialEnabled = true)
        val eventBus = SessionEventBus()
        val sessionStorage = FakeSessionStorage()
        val viewModel = viewModel(
            controller = controller,
            eventBus = eventBus,
            sessionStorage = sessionStorage,
        )

        viewModel.events.test {
            eventBus.emit()

            assertThat((awaitItem() as HomeEvent.ShowSessionExpired)).isEqualTo(HomeEvent.ShowSessionExpired)
        }
        assertThat(controller.lastSetEnabled).isEqualTo(false)
        assertThat(sessionStorage.cleared).isTrue()
    }

    @Test
    fun `toggling engine delegates to controller`() {
        val controller = FakeEngineController()
        val viewModel = viewModel(controller = controller)

        viewModel.onAction(HomeAction.OnToggleEngine(true))

        assertThat(controller.lastSetEnabled).isEqualTo(true)
        assertThat(viewModel.state.value.engineEnabled).isEqualTo(true)
    }

    @Test
    fun `state reflects stored poll interval`() {
        val settings = FakeSettingsStorage(initialInterval = 10)
        val viewModel = viewModel(settings = settings)

        assertThat(viewModel.state.value.pollIntervalMinutes).isEqualTo(10)
    }

    @Test
    fun `absen now shows placeholder message`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnAbsenNow)

            assertThat(awaitItem() is HomeEvent.ShowMessage).isEqualTo(true)
        }
    }

    @Test
    fun `state reflects engine next sync time update`() {
        val timeState = EngineTimeState()
        val viewModel = viewModel(timeState = timeState)

        timeState.updateNextSync(123456789L)

        assertThat(viewModel.state.value.nextSyncAtEpochMillis).isEqualTo(123456789L)
    }

    @Test
    fun `state clears engine next sync time`() {
        val timeState = EngineTimeState()
        timeState.updateNextSync(123456789L)
        val viewModel = viewModel(timeState = timeState)

        timeState.updateNextSync(null)

        assertThat(viewModel.state.value.nextSyncAtEpochMillis).isNull()
    }
}
