package com.unknownrex.altethol.feature.home.engine

import android.util.Log
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.local.db.dao.NotifCacheDao
import com.unknownrex.altethol.core.data.remote.AttendanceRepository

enum class SyncOutcome {
    OK,
    SESSION_EXPIRED,
    RETRYABLE_ERROR,
}

class AttendanceSyncEngine(
    private val repository: AttendanceRepository,
    private val cacheDao: NotifCacheDao,
    private val coordinator: NotifDiffCoordinator,
    private val flowRunner: AttendanceFlowRunner,
    private val log: (String) -> Unit = { Log.d("AltEtholEngine", it) },
) {

    suspend fun syncOnce(): SyncOutcome {
        when (val result = repository.fetchPresensiNotifications()) {
            is Result.Error -> {
                log("Poll gagal: ${result.error}")
                return if (result.error.isSessionExpired()) {
                    SyncOutcome.SESSION_EXPIRED
                } else {
                    SyncOutcome.RETRYABLE_ERROR
                }
            }

            is Result.Success -> {
                val fetched = result.data
                val candidates = coordinator.candidates(cacheDao.getAll(), fetched)
                if (candidates.isNotEmpty()) {
                    log("Menemukan ${candidates.size} notifikasi presensi baru")
                }
                var sessionExpired = false
                for (notif in candidates) {
                    val outcome = flowRunner.run(notif)
                    log("Hasil ${notif.idNotifikasi}: $outcome")
                    if (outcome.networkError.isSessionExpired()) {
                        sessionExpired = true
                    }
                }
                cacheDao.upsertAll(coordinator.toCacheEntries(fetched))
                log("Cache diperbarui: ${fetched.size} notifikasi")
                return if (sessionExpired) SyncOutcome.SESSION_EXPIRED else SyncOutcome.OK
            }
        }
    }

    private fun DataError.Network?.isSessionExpired(): Boolean =
        this == DataError.Network.UNAUTHORIZED || this == DataError.Network.FORBIDDEN
}
