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
import com.unknownrex.altethol.core.data.remote.dto.BacaNotifDto
import com.unknownrex.altethol.core.data.remote.dto.KuliahDto
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaRequest
import com.unknownrex.altethol.core.data.remote.dto.TerakhirKuliahDto
import kotlinx.coroutines.flow.Flow
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

    private fun engine(
        repo: AttendanceRepository,
        cache: FakeNotifCacheDao,
        history: FakeAbsensiHistoriDao,
        resolver: PresensiContextResolver,
    ) = AttendanceSyncEngine(
        repository = repo,
        cacheDao = cache,
        historyDao = history,
        coordinator = NotifDiffCoordinator(),
        flowRunner = AttendanceFlowRunner(repo, resolver, log = {}),
        log = {},
    )

    @Test
    fun `sync processes new notification and upserts cache`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Success(listOf(notif())))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val outcome = engine.syncOnce()

        assertThat(outcome).isEqualTo(SyncOutcome.OK)
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

        val outcome = engine.syncOnce()

        assertThat(outcome).isEqualTo(SyncOutcome.SESSION_EXPIRED)
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

        val outcome = engine.syncOnce()

        assertThat(outcome).isEqualTo(SyncOutcome.OK)
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

        val outcome = engine.syncOnce()

        assertThat(outcome).isEqualTo(SyncOutcome.OK)
        assertThat(repo.submitCalled).isEqualTo(false)
        assertThat(history.records).hasSize(0)
    }

    @Test
    fun `sync returns session expired when fetch is unauthorized`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Error(DataError.Network.UNAUTHORIZED))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val outcome = engine.syncOnce()

        assertThat(outcome).isEqualTo(SyncOutcome.SESSION_EXPIRED)
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

        val outcome = engine.syncOnce()

        assertThat(outcome).isEqualTo(SyncOutcome.SESSION_EXPIRED)
    }

    @Test
    fun `sync keeps cache updated after retryable fetch error`() = runTest {
        val repo = FakeAttendanceRepository(fetchResult = Result.Error(DataError.Network.NO_INTERNET))
        val cache = FakeNotifCacheDao()
        val history = FakeAbsensiHistoriDao()
        val engine = engine(repo, cache, history, FakeContextResolver())

        val outcome = engine.syncOnce()

        assertThat(outcome).isEqualTo(SyncOutcome.RETRYABLE_ERROR)
        assertThat(cache.getAll()).hasSize(0)
        assertThat(history.records).hasSize(0)
    }
}
