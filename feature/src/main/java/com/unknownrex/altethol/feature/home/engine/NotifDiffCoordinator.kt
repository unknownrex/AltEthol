package com.unknownrex.altethol.feature.home.engine

import com.unknownrex.altethol.core.data.local.db.entity.NotifCacheEntity
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import com.unknownrex.altethol.core.data.remote.dto.toNotifCacheEntity

const val NOTIF_KODE_PRESENSI_KULIAH = "PRESENSI-KULIAH"
const val NOTIF_STATUS_UNREAD = "1"

class NotifDiffCoordinator {

    fun candidates(
        cached: List<NotifCacheEntity>,
        fetched: List<NotifikasiDto>,
    ): List<NotifikasiDto> {
        val seenIds = cached.mapTo(HashSet()) { it.idNotifikasi }
        return fetched.filter { notif ->
            notif.idNotifikasi !in seenIds &&
                notif.status == NOTIF_STATUS_UNREAD &&
                notif.kodeNotifikasi == NOTIF_KODE_PRESENSI_KULIAH
        }
    }

    fun toCacheEntries(fetched: List<NotifikasiDto>, now: Long = System.currentTimeMillis()): List<NotifCacheEntity> =
        fetched.map { it.toNotifCacheEntity(now) }
}
