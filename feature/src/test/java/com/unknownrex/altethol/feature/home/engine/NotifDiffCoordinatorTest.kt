package com.unknownrex.altethol.feature.home.engine

import assertk.assertThat
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.unknownrex.altethol.core.data.local.db.entity.NotifCacheEntity
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import org.junit.jupiter.api.Test

class NotifDiffCoordinatorTest {

    private val coordinator = NotifDiffCoordinator()

    private fun notif(
        id: String,
        status: String = NOTIF_STATUS_UNREAD,
        kode: String = NOTIF_KODE_PRESENSI_KULIAH,
    ) = NotifikasiDto(
        idNotifikasi = id,
        keterangan = "keterangan",
        status = status,
        urlWeb = "/notifikasi/presensi/$id",
        kodeNotifikasi = kode,
        dataTerkait = "219110-4",
        createdAt = "2026-08-13T07:58:51.000Z",
        waktuNotifikasi = "13 Agustus 2026 14:58",
        createdAtIndonesia = "Kamis, 13 Agustus 2026 - 14:58",
    )

    @Test
    fun `candidates include new unread presensi notifications`() {
        val cached = listOf(NotifCacheEntity(idNotifikasi = "old-1", status = "2", lastSeenAt = 1L))
        val fetched = listOf(notif("old-1"), notif("new-1"))

        val result = coordinator.candidates(cached, fetched)

        assertThat(result).extracting { it.idNotifikasi }.isEqualTo(listOf("new-1"))
    }

    @Test
    fun `candidates exclude read notifications`() {
        val result = coordinator.candidates(emptyList(), listOf(notif("read-1", status = "2")))

        assertThat(result).hasSize(0)
    }

    @Test
    fun `candidates exclude non presensi notifications`() {
        val result = coordinator.candidates(emptyList(), listOf(notif("tugas-1", kode = "TUGAS-BARU")))

        assertThat(result).hasSize(0)
    }

    @Test
    fun `candidates exclude already cached notifications`() {
        val cached = listOf(NotifCacheEntity(idNotifikasi = "seen-1", status = "1", lastSeenAt = 1L))

        val result = coordinator.candidates(cached, listOf(notif("seen-1")))

        assertThat(result).hasSize(0)
    }

    @Test
    fun `toCacheEntries maps every fetched notification`() {
        val fetched = listOf(notif("a-1"), notif("b-1"))

        val entries = coordinator.toCacheEntries(fetched, now = 123L)

        assertThat(entries).hasSize(2)
        assertThat(entries.first()).isEqualTo(
            NotifCacheEntity(idNotifikasi = "a-1", status = "1", lastSeenAt = 123L),
        )
    }
}
