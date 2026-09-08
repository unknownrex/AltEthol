package com.unknownrex.altethol.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.unknownrex.altethol.core.data.local.db.dao.AbsensiHistoriDao
import com.unknownrex.altethol.core.data.local.db.entity.AbsensiHistoriEntity
import com.unknownrex.altethol.core.data.model.AttendanceStatus
import com.unknownrex.altethol.core.data.session.SessionEventBus
import com.unknownrex.altethol.core.data.session.SessionState
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.settings.SettingsStorage
import com.unknownrex.altethol.core.ui.text.UiText
import com.unknownrex.altethol.feature.home.engine.AttendanceFlowResult
import com.unknownrex.altethol.feature.home.engine.EngineController
import com.unknownrex.altethol.feature.home.engine.EngineTimeState
import com.unknownrex.altethol.feature.home.engine.SyncEngine
import com.unknownrex.altethol.feature.home.engine.SyncOutcome
import com.unknownrex.altethol.feature.home.engine.SyncResult
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

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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

    private class FakeAbsensiHistoriDao(
        initialRecords: List<AbsensiHistoriEntity> = emptyList(),
    ) : AbsensiHistoriDao {
        private val _records = MutableStateFlow(initialRecords)
        override suspend fun insert(histori: AbsensiHistoriEntity): Long {
            _records.value = _records.value + histori
            return 1L
        }
        override fun observeAll(): Flow<List<AbsensiHistoriEntity>> = _records
        override suspend fun clear() {
            _records.value = emptyList()
        }
    }

    private class FakeSyncEngine(
        var result: SyncResult = SyncResult(SyncOutcome.OK),
    ) : SyncEngine {
        var lastCalled = false
        override suspend fun syncOnce(): SyncResult {
            lastCalled = true
            return result
        }
    }

    private fun viewModel(
        controller: FakeEngineController = FakeEngineController(),
        settings: FakeSettingsStorage = FakeSettingsStorage(),
        timeState: EngineTimeState = EngineTimeState(),
        eventBus: SessionEventBus = SessionEventBus(),
        sessionStorage: FakeSessionStorage = FakeSessionStorage(),
        historyDao: FakeAbsensiHistoriDao = FakeAbsensiHistoriDao(),
        syncEngine: FakeSyncEngine = FakeSyncEngine(),
    ) = HomeViewModel(controller, settings, timeState, eventBus, sessionStorage, historyDao, syncEngine)

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
    fun `total attendance counts only successful records from history`() {
        val historyDao = FakeAbsensiHistoriDao(
            initialRecords = listOf(
                historyEntity(status = AttendanceStatus.SUCCESS),
                historyEntity(status = AttendanceStatus.SUCCESS),
                historyEntity(status = AttendanceStatus.FAILED),
            ),
        )
        val viewModel = viewModel(historyDao = historyDao)

        assertThat(viewModel.state.value.totalAttendanceSuccess).isEqualTo(2)
    }

    @Test
    fun `total attendance updates when history changes`() = runTest(testDispatcher.scheduler) {
        val historyDao = FakeAbsensiHistoriDao()
        val viewModel = viewModel(historyDao = historyDao)

        assertThat(viewModel.state.value.totalAttendanceSuccess).isEqualTo(0)

        historyDao.insert(historyEntity(status = AttendanceStatus.SUCCESS))
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value.totalAttendanceSuccess).isEqualTo(1)
    }

    private fun historyEntity(status: AttendanceStatus) = AbsensiHistoriEntity(
        matakuliah = "Matakuliah",
        kuliahId = 1,
        waktuDeteksi = 0L,
        waktuAbsenDieksekusi = 0L,
        status = status,
        pesanServer = "ok",
    )

    @Test
    fun `absen now with no candidates shows no notification message`() = runTest {
        val syncEngine = FakeSyncEngine(SyncResult(SyncOutcome.OK, emptyList()))
        val viewModel = viewModel(syncEngine = syncEngine)

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnAbsenNow)

            assertThat((awaitItem() as HomeEvent.ShowMessage).message)
                .isEqualTo(UiText.DynamicString("Tidak ada notifikasi presensi baru"))
        }
        assertThat(syncEngine.lastCalled).isTrue()
    }

    @Test
    fun `absen now with a submitted result shows success message with matakuliah`() = runTest {
        val syncEngine = FakeSyncEngine(
            SyncResult(
                SyncOutcome.OK,
                listOf(
                    AttendanceFlowResult.Submitted(
                        idNotifikasi = "1",
                        matakuliah = "Matematika",
                        kuliahId = 10,
                        pesan = "ok",
                    ),
                ),
            ),
        )
        val viewModel = viewModel(syncEngine = syncEngine)

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnAbsenNow)

            assertThat((awaitItem() as HomeEvent.ShowMessage).message)
                .isEqualTo(UiText.DynamicString("Absensi berhasil: Matematika"))
        }
    }

    @Test
    fun `absen now with only skipped or failed results shows no session message`() = runTest {
        val syncEngine = FakeSyncEngine(
            SyncResult(
                SyncOutcome.OK,
                listOf(
                    AttendanceFlowResult.Skipped(
                        idNotifikasi = "1",
                        matakuliah = "Fisika",
                        kuliahId = 10,
                        reason = "tidak ada sesi presensi terbuka",
                    ),
                ),
            ),
        )
        val viewModel = viewModel(syncEngine = syncEngine)

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnAbsenNow)

            assertThat((awaitItem() as HomeEvent.ShowMessage).message)
                .isEqualTo(UiText.DynamicString("Tidak ada sesi presensi yang dapat diisi"))
        }
    }

    @Test
    fun `absen now sets running flag true then false`() = runTest {
        val syncEngine = FakeSyncEngine()
        val viewModel = viewModel(syncEngine = syncEngine)

        viewModel.onAction(HomeAction.OnAbsenNow)

        assertThat(viewModel.state.value.isAbsenNowRunning).isEqualTo(false)
    }

    @Test
    fun `absen now on session expired emits ShowSessionExpired`() = runTest {
        val syncEngine = FakeSyncEngine(SyncResult(SyncOutcome.SESSION_EXPIRED))
        val viewModel = viewModel(syncEngine = syncEngine)

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnAbsenNow)

            assertThat(awaitItem()).isEqualTo(HomeEvent.ShowSessionExpired)
        }
    }

    @Test
    fun `absen now on retryable error shows error message`() = runTest {
        val syncEngine = FakeSyncEngine(SyncResult(SyncOutcome.RETRYABLE_ERROR))
        val viewModel = viewModel(syncEngine = syncEngine)

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnAbsenNow)

            assertThat((awaitItem() as HomeEvent.ShowMessage).message)
                .isEqualTo(UiText.DynamicString("Gagal mengambil data notifikasi. Coba lagi."))
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
