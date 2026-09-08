package com.unknownrex.altethol.feature.home.engine

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.local.db.dao.AbsensiHistoriDao
import com.unknownrex.altethol.core.data.local.db.dao.NotifCacheDao
import com.unknownrex.altethol.core.data.local.db.entity.AbsensiHistoriEntity
import com.unknownrex.altethol.core.data.local.db.entity.NotifCacheEntity
import com.unknownrex.altethol.core.data.model.AttendanceStatus
import com.unknownrex.altethol.core.data.model.AttendanceStep
import com.unknownrex.altethol.core.data.remote.AttendanceRepository
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.remote.dto.BacaNotifDto
import com.unknownrex.altethol.core.data.remote.dto.KuliahDto
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaRequest
import com.unknownrex.altethol.core.data.remote.dto.TerakhirKuliahDto
import com.unknownrex.altethol.core.data.remote.dto.ValidasiTokenDto
import com.unknownrex.altethol.core.data.session.SessionEventBus
import com.unknownrex.altethol.core.data.session.SessionState
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.session.TokenRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AttendanceSyncEngineTest {

    private class FakeNotifCacheDao(
        initial: List<NotifCacheEntity> = emptyList(),
    ) : NotifCacheDao {
        private val store = initial.toMutableList()

        override suspend fun upsert(notif: NotifCacheEntity) {
            store.removeAll { it.idNotifikasi == notif.idNotifikasi }
            store.add(notif)
        }

        override suspend fun upsertAll(notifs: List<NotifCacheEntity>) {
            notifs.forEach { upsert(it) }
        }

        override suspend fun getAll(): List<NotifCacheEntity> = store.toList()

        override suspend fun getById(id: String): NotifCacheEntity? =
            store.firstOrNull { it.idNotifikasi == id }

        override fun observeAll(): Flow<List<NotifCacheEntity>> = flowOf(store.toList())

        override suspend fun clear() = store.clear()
    }

    private class FakeAbsensiHistoriDao : AbsensiHistoriDao {
        val records = mutableListOf<AbsensiHistoriEntity>()

        override suspend fun insert(histori: AbsensiHistoriEntity): Long {
            records.add(histori)
            return records.size.toLong()
        }

        override fun observeAll(): Flow<List<AbsensiHistoriEntity>> = flowOf(records.toList())

        override suspend fun clear() = records.clear()
    }

    private class FakeAttendanceRepository(
        var fetchResult: Result<List<NotifikasiDto>, DataError.Network> = Result.Success(emptyList()),
        var markAsReadResult: Result<BacaNotifDto, DataError.Network> = Result.Success(BacaNotifDto()),
        var keyResult: Result<TerakhirKuliahDto, DataError.Network> = Result.Success(TerakhirKuliahDto(ditemukan = true, open = true, key = "KEY1")),
        var submitResult: Result<PresensiMahasiswaDto, DataError.Network> = Result.Success(PresensiMahasiswaDto(sukses = true)),
    ) : AttendanceRepository {
        var submitCalled = false

        override suspend fun fetchPresensiNotifications(): Result<List<NotifikasiDto>, DataError.Network> = fetchResult

        override suspend fun markAsRead(idNotifikasi: String): Result<BacaNotifDto, DataError.Network> = markAsReadResult

        override suspend fun getAttendanceKey(
            kuliah: Int,
            jenisSchema: Int,
        ): Result<TerakhirKuliahDto, DataError.Network> = keyResult

        override suspend fun submitAttendance(
            request: PresensiMahasiswaRequest,
        ): Result<PresensiMahasiswaDto, DataError.Network> {
            submitCalled = true
            return submitResult
        }

        override suspend fun getCourseDetail(
            kuliah: Int,
            jenisSchema: Int,
        ): Result<KuliahDto?, DataError.Network> = Result.Success(null)
    }

    private class FakeContextResolver(
        var result: Result<AttendanceContext, DataError.Network> = Result.Success(
            AttendanceContext(219110, 4, 219105, 28801, "English for academic"),
        ),
    ) : PresensiContextResolver {
        override suspend fun resolve(notif: NotifikasiDto) = result
    }

    private class FakeAuthRepository(
        var refreshResult: Result<String, DataError.Network>,
    ) : AuthRepository {
        var refreshCalls = 0

        override suspend fun validateToken(): Result<ValidasiTokenDto, DataError.Network> =
            Result.Success(ValidasiTokenDto(nomor = 28801, nipnrp = "n", nama = "n"))

        override suspend fun refreshToken(): Result<String, DataError.Network> {
            refreshCalls++
            return refreshResult
        }
    }

    private class FakeSessionStorage(
        initialState: SessionState,
    ) : SessionStorage {
        val sessionFlow = MutableStateFlow(initialState)
        var savedToken: String? = null

        override val session: Flow<SessionState> = sessionFlow

        override suspend fun saveSession(token: String, refreshToken: String?) = Unit

        override suspend fun saveToken(token: String) {
            savedToken = token
        }

        override suspend fun saveMahasiswaId(id: Int) = Unit

        override suspend fun clear() = Unit
    }

    private fun notif(id: String = "new-1") = NotifikasiDto(
        idNotifikasi = id,
        keterangan = "Dosen telah melakukan presensi",
        status = "1",
        urlWeb = "/notifikasi/presensi/$id",
        kodeNotifikasi = NOTIF_KODE_PRESENSI_KULIAH,
        dataTerkait = "219110-4",
        createdAt = "2026-08-13T07:58:51.000Z",
        waktuNotifikasi = "13 Agustus 2026 14:58",
        createdAtIndonesia = "Kamis, 13 Agustus 2026 - 14:58",
    )

    private fun loggedInSession() = SessionState(
        isLoggedIn = true,
        token = "token",
        refreshToken = "refresh-token",
    )

    private fun freshRefresher() = TokenRefresher(
        sessionStorage = FakeSessionStorage(loggedInSession()),
        authRepository = FakeAuthRepository(Result.Success("new-token")),
        jwtExpiration = { 4_000_000_000L },
        now = { 0L },
    )

    private fun stalledRefresher(
        auth: FakeAuthRepository,
    ): TokenRefresher = TokenRefresher(
        sessionStorage = FakeSessionStorage(loggedInSession()),
        authRepository = auth,
        jwtExpiration = { 1L },
        now = { 0L },
    )

    private fun engine(
        repo: AttendanceRepository,
        cache: FakeNotifCacheDao,
        history: FakeAbsensiHistoriDao,
        resolver: PresensiContextResolver,
        refresher: TokenRefresher = freshRefresher(),
    ) = AttendanceSyncEngine(
        repository = repo,
        cacheDao = cache,
        historyDao = history,
        coordinator = NotifDiffCoordinator(),
        flowRunner = AttendanceFlowRunner(repo, resolver, log = {}),
        tokenRefresher = refresher,
        sessionEventBus = SessionEventBus(),
        log = {},
    )

    @Test
    fun `sync processes new notification and upserts cache`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Success(listOf(notif())))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.OK)
        assertThat(result.flowResults).hasSize(1)
        assertThat(result.flowResults.first() is AttendanceFlowResult.Submitted).isTrue()
        assertThat(repo.submitCalled).isTrue()
        assertThat(cache.getAll()).hasSize(1)
        assertThat(cache.getAll().first().idNotifikasi).isEqualTo("new-1")
        assertThat(history.records).hasSize(1)
        assertThat(history.records.first().status).isEqualTo(AttendanceStatus.SUCCESS)
        assertThat(history.records.first().matakuliah).isEqualTo("English for academic")
        assertThat(history.records.first().kuliahId).isEqualTo(219110)
        assertThat(history.records.first().failedStep).isNull()
    }

    @Test
    fun `sync persists failed outcome with failed step`() = runTest {
        val repo = FakeAttendanceRepository(
            fetchResult = Result.Success(listOf(notif())),
            markAsReadResult = Result.Error(DataError.Network.UNAUTHORIZED),
        )
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.SESSION_EXPIRED)
        assertThat(result.flowResults).hasSize(1)
        assertThat(result.flowResults.first() is AttendanceFlowResult.Failed).isTrue()
        assertThat(history.records).hasSize(1)
        assertThat(history.records.first().status).isEqualTo(AttendanceStatus.FAILED)
        assertThat(history.records.first().failedStep).isEqualTo(AttendanceStep.MARK_AS_READ)
    }

    @Test
    fun `sync persists skipped outcome as failed`() = runTest {
        val repo = FakeAttendanceRepository(
            fetchResult = Result.Success(listOf(notif())),
            keyResult = Result.Success(TerakhirKuliahDto(ditemukan = true, open = false, key = "KEY1")),
        )
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.OK)
        assertThat(result.flowResults).hasSize(1)
        assertThat(result.flowResults.first() is AttendanceFlowResult.Skipped).isTrue()
        assertThat(history.records).hasSize(1)
        assertThat(history.records.first().status).isEqualTo(AttendanceStatus.FAILED)
        assertThat(history.records.first().failedStep).isNull()
        assertThat(history.records.first().pesanServer).isEqualTo("tidak ada sesi presensi terbuka")
    }

    @Test
    fun `sync does not reprocess cached notification`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Success(listOf(notif())))
        val cache = FakeNotifCacheDao(
            initial = listOf(NotifCacheEntity(idNotifikasi = "new-1", status = "1", lastSeenAt = 1L)),
        )
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.OK)
        assertThat(repo.submitCalled).isEqualTo(false)
        assertThat(history.records).hasSize(0)
    }

    @Test
    fun `sync returns session expired when fetch is unauthorized`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Error(DataError.Network.UNAUTHORIZED))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.SESSION_EXPIRED)
    }

    @Test
    fun `sync returns session expired when a step fails with unauthorized`() = runTest {
        val repo = FakeAttendanceRepository(
            fetchResult = Result.Success(listOf(notif())),
            markAsReadResult = Result.Error(DataError.Network.UNAUTHORIZED),
        )
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.SESSION_EXPIRED)
    }

    @Test
    fun `sync keeps cache updated after retryable fetch error`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Error(DataError.Network.NO_INTERNET))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.RETRYABLE_ERROR)
        assertThat(cache.getAll()).hasSize(0)
        assertThat(history.records).hasSize(0)
    }

    @Test
    fun `sync refreshes stale token before fetching`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Success(listOf(notif())))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val auth = FakeAuthRepository(Result.Success("refreshed-token"))
        val refresher = stalledRefresher(auth)
        val engine = engine(repo, cache, history, FakeContextResolver(), refresher)

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.OK)
        assertThat(auth.refreshCalls).isEqualTo(1)
    }

    @Test
    fun `sync returns session expired when token refresh is unauthorized`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Success(emptyList()))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val auth = FakeAuthRepository(Result.Error(DataError.Network.UNAUTHORIZED))
        val engine = engine(repo, cache, history, FakeContextResolver(), stalledRefresher(auth))

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.SESSION_EXPIRED)
        assertThat(cache.getAll()).hasSize(0)
    }

    @Test
    fun `sync returns retryable when token refresh fails with network error`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Success(emptyList()))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val auth = FakeAuthRepository(Result.Error(DataError.Network.NO_INTERNET))
        val engine = engine(repo, cache, history, FakeContextResolver(), stalledRefresher(auth))

        val result = engine.syncOnce()

        assertThat(result.outcome).isEqualTo(SyncOutcome.RETRYABLE_ERROR)
    }
}
