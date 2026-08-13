package com.unknownrex.altethol.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.unknownrex.altethol.feature.home.engine.EngineController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Test
    fun `initial state reflects controller`() {
        val viewModel = HomeViewModel(FakeEngineController(initialEnabled = true))

        assertThat(viewModel.state.value.engineEnabled).isEqualTo(true)
    }

    @Test
    fun `toggling engine delegates to controller`() {
        val controller = FakeEngineController()
        val viewModel = HomeViewModel(controller)

        viewModel.onAction(HomeAction.OnToggleEngine(true))

        assertThat(controller.lastSetEnabled).isEqualTo(true)
        assertThat(viewModel.state.value.engineEnabled).isEqualTo(true)
    }

    @Test
    fun `absen now shows placeholder message`() = runTest {
        val viewModel = HomeViewModel(FakeEngineController())

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnAbsenNow)

            assertThat(awaitItem() is HomeEvent.ShowMessage).isEqualTo(true)
        }
    }
}
