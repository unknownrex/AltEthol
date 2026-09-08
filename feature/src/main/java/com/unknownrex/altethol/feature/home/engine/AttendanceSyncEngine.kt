package com.unknownrex.altethol.feature.home.engine

import android.util.Log
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.local.db.dao.AbsensiHistoriDao
import com.unknownrex.altethol.core.data.local.db.dao.NotifCacheDao
import com.unknownrex.altethol.core.data.remote.AttendanceRepository
import com.unknownrex.altethol.core.data.session.SessionEventBus
import com.unknownrex.altethol.core.data.session.SessionRefreshResult
import com.unknownrex.altethol.core.data.session.TokenRefresher

enum class SyncOutcome {
    OK,
    SESSION_EXPIRED,
    RETRYABLE_ERROR,
}

data class SyncResult(
    val outcome: SyncOutcome,
    val flowResults: List<AttendanceFlowResult> = emptyList(),
)

class AttendanceSyncEngine(
    private val repository: AttendanceRepository,
    private val cacheDao: NotifCacheDao,
    private val historyDao: AbsensiHistoriDao,
    private val coordinator: NotifDiffCoordinator,
    private val flowRunner: AttendanceFlowRunner,
    private val tokenRefresher: TokenRefresher,
    private val sessionEventBus: SessionEventBus,
    private val log: (String) -> Unit = { Log.d("AltEtholEngine", it) },
) {

    suspend fun syncOnce(): SyncResult {
        when (tokenRefresher.refreshIfNeeded()) {
            SessionRefreshResult.SESSION_EXPIRED -> {
                log("Token refresh gagal, sesi berakhir")
                sessionEventBus.emit()
                return SyncResult(outcome = SyncOutcome.SESSION_EXPIRED)
            }

            SessionRefreshResult.ERROR -> {
                return SyncResult(outcome = SyncOutcome.RETRYABLE_ERROR)
            }

            SessionRefreshResult.REFRESHED,
            SessionRefreshResult.ALREADY_FRESH,
            -> Unit
        }

        when (val result = repository.fetchPresensiNotifications()) {
            is Result.Error -> {
                log("Poll gagal: ${result.error}")
                return SyncResult(
                    outcome = if (result.error.isSessionExpired()) {
                        SyncOutcome.SESSION_EXPIRED
                    } else {
                        SyncOutcome.RETRYABLE_ERROR
                    },
                )
            }

            is Result.Success -> {
                val fetched = result.data
                val candidates = coordinator.candidates(cacheDao.getAll(), fetched)
                if (candidates.isNotEmpty()) {
                    log("Menemukan ${candidates.size} notifikasi presensi baru")
                }
                var sessionExpired = false
                val flowResults = mutableListOf<AttendanceFlowResult>()
                for (notif in candidates) {
                    val outcome = flowRunner.run(notif)
                    log("Hasil ${notif.idNotifikasi}: $outcome")
                    flowResults += outcome
                    historyDao.insert(outcome.toAbsensiHistoriEntity(notif))
                    if (outcome.networkError.isSessionExpired()) {
                        sessionExpired = true
                    }
                }
                cacheDao.upsertAll(coordinator.toCacheEntries(fetched))
                log("Cache diperbarui: ${fetched.size} notifikasi")
                return SyncResult(
                    outcome = if (sessionExpired) SyncOutcome.SESSION_EXPIRED else SyncOutcome.OK,
                    flowResults = flowResults,
                )
            }
        }
    }

    private fun DataError.Network?.isSessionExpired(): Boolean =
        this == DataError.Network.UNAUTHORIZED || this == DataError.Network.FORBIDDEN
}
