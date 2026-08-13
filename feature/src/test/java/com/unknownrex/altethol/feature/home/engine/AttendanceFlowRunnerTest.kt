package com.unknownrex.altethol.feature.home.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.model.AttendanceStep
import com.unknownrex.altethol.core.data.remote.AttendanceRepository
import com.unknownrex.altethol.core.data.remote.dto.BacaNotifDto
import com.unknownrex.altethol.core.data.remote.dto.KuliahDto
import com.unknownrex.altethol.core.data.remote.dto.MatakuliahDto
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaRequest
import com.unknownrex.altethol.core.data.remote.dto.TerakhirKuliahDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AttendanceFlowRunnerTest {

    private class FakeAttendanceRepository(
        var markAsReadResult: Result<BacaNotifDto, DataError.Network> = Result.Success(BacaNotifDto()),
        var keyResult: Result<TerakhirKuliahDto, DataError.Network> = Result.Success(TerakhirKuliahDto(ditemukan = true, open = true, key = "KEY1")),
        var submitResult: Result<PresensiMahasiswaDto, DataError.Network> = Result.Success(PresensiMahasiswaDto(sukses = true, pesan = "Berhasil")),
        var courseResult: Result<KuliahDto?, DataError.Network> = Result.Success(null),
    ) : AttendanceRepository {
        var markAsReadCalled = false
        var lastMarkAsReadId: String? = null
        var lastKeyCall: Pair<Int, Int>? = null
        var lastSubmitRequest: PresensiMahasiswaRequest? = null
        var submitCalled = false

        override suspend fun fetchPresensiNotifications() = Result.Success(emptyList<NotifikasiDto>())

        override suspend fun markAsRead(idNotifikasi: String): Result<BacaNotifDto, DataError.Network> {
            markAsReadCalled = true
            lastMarkAsReadId = idNotifikasi
            return markAsReadResult
        }

        override suspend fun getAttendanceKey(
            kuliah: Int,
            jenisSchema: Int,
        ): Result<TerakhirKuliahDto, DataError.Network> {
            lastKeyCall = kuliah to jenisSchema
            return keyResult
        }

        override suspend fun submitAttendance(
            request: PresensiMahasiswaRequest,
        ): Result<PresensiMahasiswaDto, DataError.Network> {
            submitCalled = true
            lastSubmitRequest = request
            return submitResult
        }

        override suspend fun getCourseDetail(
            kuliah: Int,
            jenisSchema: Int,
        ): Result<KuliahDto?, DataError.Network> = courseResult
    }

    private class FakeContextResolver(
        var result: Result<AttendanceContext, DataError.Network> = Result.Success(
            AttendanceContext(
                kuliah = 219110,
                jenisSchema = 4,
                kuliahAsal = 219105,
                mahasiswa = 28801,
                matakuliah = "English for academic",
            ),
        ),
    ) : PresensiContextResolver {
        override suspend fun resolve(notif: NotifikasiDto) = result
    }

    private fun notif(id: String = "n-1") = NotifikasiDto(
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

    private fun runner(repo: AttendanceRepository, resolver: PresensiContextResolver) =
        AttendanceFlowRunner(repo, resolver, log = {})

    @Test
    fun `mark as read failure returns failed with mark-as-read step`() = runTest {
        val repo = FakeAttendanceRepository(markAsReadResult = Result.Error(DataError.Network.UNAUTHORIZED))
        val runner = runner(repo, FakeContextResolver())

        val result = runner.run(notif())

        assertThat(result is AttendanceFlowResult.Failed).isEqualTo(true)
        assertThat((result as AttendanceFlowResult.Failed).failedStep)
            .isEqualTo(AttendanceStep.MARK_AS_READ)
        assertThat(result.networkError).isEqualTo(DataError.Network.UNAUTHORIZED)
    }

    @Test
    fun `context resolution failure returns failed without step`() = runTest {
        val repo = FakeAttendanceRepository()
        val resolver = FakeContextResolver(result = Result.Error(DataError.Network.SERIALIZATION))

        val result = runner(repo, resolver).run(notif())

        assertThat(result is AttendanceFlowResult.Failed).isEqualTo(true)
        assertThat((result as AttendanceFlowResult.Failed).failedStep).isNull()
        assertThat(repo.lastKeyCall).isNull()
    }

    @Test
    fun `closed session returns skipped`() = runTest {
        val repo = FakeAttendanceRepository(
            keyResult = Result.Success(TerakhirKuliahDto(ditemukan = true, open = false, key = "KEY1")),
        )

        val result = runner(repo, FakeContextResolver()).run(notif())

        assertThat(result is AttendanceFlowResult.Skipped).isEqualTo(true)
        assertThat(repo.submitCalled).isEqualTo(false)
    }

    @Test
    fun `successful flow submits attendance with resolved context and key`() = runTest {
        val repo = FakeAttendanceRepository()
        val runner = runner(repo, FakeContextResolver())

        val result = runner.run(notif())

        assertThat(result is AttendanceFlowResult.Submitted).isEqualTo(true)
        assertThat(repo.markAsReadCalled).isEqualTo(true)
        assertThat(repo.lastKeyCall).isEqualTo(219110 to 4)
        assertThat(repo.lastSubmitRequest).isEqualTo(
            PresensiMahasiswaRequest(
                kuliah = 219110,
                mahasiswa = 28801,
                jenisSchema = 4,
                kuliahAsal = 219105,
                key = "KEY1",
            ),
        )
    }

    @Test
    fun `get key failure returns failed with get-key step`() = runTest {
        val repo = FakeAttendanceRepository(keyResult = Result.Error(DataError.Network.SERVER_ERROR))

        val result = runner(repo, FakeContextResolver()).run(notif())

        assertThat((result as AttendanceFlowResult.Failed).failedStep)
            .isEqualTo(AttendanceStep.GET_KEY)
        assertThat(result.networkError).isEqualTo(DataError.Network.SERVER_ERROR)
    }

    @Test
    fun `submit failure returns failed with submit step`() = runTest {
        val repo = FakeAttendanceRepository(submitResult = Result.Error(DataError.Network.BAD_REQUEST))

        val result = runner(repo, FakeContextResolver()).run(notif())

        assertThat((result as AttendanceFlowResult.Failed).failedStep)
            .isEqualTo(AttendanceStep.SUBMIT)
        assertThat(repo.lastSubmitRequest).isNotNull()
    }
}
