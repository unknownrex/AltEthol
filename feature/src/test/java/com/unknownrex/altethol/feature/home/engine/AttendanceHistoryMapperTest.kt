package com.unknownrex.altethol.feature.home.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.unknownrex.altethol.core.data.model.AttendanceStatus
import com.unknownrex.altethol.core.data.model.AttendanceStep
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test

class AttendanceHistoryMapperTest {

    private val notif = NotifikasiDto(
        idNotifikasi = "n-1",
        keterangan = "Dosen telah melakukan presensi",
        status = "1",
        urlWeb = "/notifikasi/presensi/n-1",
        kodeNotifikasi = NOTIF_KODE_PRESENSI_KULIAH,
        dataTerkait = "219110-4",
        createdAt = "2026-08-13T07:58:51.000Z",
        waktuNotifikasi = "13 Agustus 2026 14:58",
        createdAtIndonesia = "Kamis, 13 Agustus 2026 - 14:58",
    )

    @Test
    fun `submitted maps to success with server message and parsed detection time`() {
        val entity = AttendanceFlowResult.Submitted(
            idNotifikasi = "n-1",
            matakuliah = "English for academic",
            kuliahId = 219110,
            pesan = "Berhasil",
        ).toAbsensiHistoriEntity(notif, now = 1000L)

        assertThat(entity.matakuliah).isEqualTo("English for academic")
        assertThat(entity.kuliahId).isEqualTo(219110)
        assertThat(entity.status).isEqualTo(AttendanceStatus.SUCCESS)
        assertThat(entity.failedStep).isNull()
        assertThat(entity.pesanServer).isEqualTo("Berhasil")
        assertThat(entity.waktuDeteksi).isEqualTo(
            OffsetDateTime.parse("2026-08-13T07:58:51.000Z").toInstant().toEpochMilli(),
        )
        assertThat(entity.waktuAbsenDieksekusi).isEqualTo(1000L)
    }

    @Test
    fun `failed maps to failed with step and message`() {
        val entity = AttendanceFlowResult.Failed(
            idNotifikasi = "n-1",
            matakuliah = "English for academic",
            kuliahId = 219110,
            failedStep = AttendanceStep.GET_KEY,
            message = "get-key: SERVER_ERROR",
            networkError = null,
        ).toAbsensiHistoriEntity(notif, now = 2000L)

        assertThat(entity.status).isEqualTo(AttendanceStatus.FAILED)
        assertThat(entity.failedStep).isEqualTo(AttendanceStep.GET_KEY)
        assertThat(entity.pesanServer).isEqualTo("get-key: SERVER_ERROR")
        assertThat(entity.waktuAbsenDieksekusi).isEqualTo(2000L)
    }

    @Test
    fun `skipped maps to failed without step and with reason`() {
        val entity = AttendanceFlowResult.Skipped(
            idNotifikasi = "n-1",
            matakuliah = "English for academic",
            kuliahId = 219110,
            reason = "tidak ada sesi presensi terbuka",
        ).toAbsensiHistoriEntity(notif, now = 3000L)

        assertThat(entity.status).isEqualTo(AttendanceStatus.FAILED)
        assertThat(entity.failedStep).isNull()
        assertThat(entity.pesanServer).isEqualTo("tidak ada sesi presensi terbuka")
    }

    @Test
    fun `failed before context keeps empty matakuliah and parsed kuliah id`() {
        val entity = AttendanceFlowResult.Failed(
            idNotifikasi = "n-1",
            matakuliah = "",
            kuliahId = 219110,
            failedStep = AttendanceStep.MARK_AS_READ,
            message = "mark-as-read: UNAUTHORIZED",
            networkError = null,
        ).toAbsensiHistoriEntity(notif)

        assertThat(entity.matakuliah).isEqualTo("")
        assertThat(entity.kuliahId).isEqualTo(219110)
        assertThat(entity.status).isEqualTo(AttendanceStatus.FAILED)
    }

    @Test
    fun `unparseable createdAt falls back to execution time`() {
        val badNotif = notif.copy(createdAt = "bukan-iso")
        val entity = AttendanceFlowResult.Submitted(
            idNotifikasi = "n-1",
            matakuliah = "English for academic",
            kuliahId = 219110,
            pesan = null,
        ).toAbsensiHistoriEntity(badNotif, now = 4000L)

        assertThat(entity.waktuDeteksi).isEqualTo(4000L)
        assertThat(entity.pesanServer).isEqualTo("")
    }
}
