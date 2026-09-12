package com.unknownrex.altethol.feature.home.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.unknownrex.altethol.core.data.session.SessionEventBus
import com.unknownrex.altethol.core.data.settings.SettingsStorage
import com.unknownrex.altethol.feature.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import java.util.Locale

class AttendanceSyncService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncEngine: AttendanceSyncEngine by lazy { get() }
    private val settings: SettingsStorage by lazy { get() }
    private val notifier: AttendanceNotifier by lazy { get() }
    private val timeState: EngineTimeState by lazy { get() }
    private val sessionEventBus: SessionEventBus by lazy { get() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        serviceScope.launch { runSyncLoop() }
        serviceScope.launch { runCountdownTicker() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        serviceScope.cancel()
        timeState.updateNextSync(null)
        super.onDestroy()
    }

    private fun startAsForeground() {
        createNotificationChannel()
        val notification = buildForegroundNotification(
            contentText = getString(R.string.engine_notification_text),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.engine_notification_title))
            .setContentText(contentText)
            .setContentIntent(NotificationIntentHelper.openAppPendingIntent(this))
            .setOngoing(true)
            .build()

    private suspend fun CoroutineScope.runCountdownTicker() {
        while (isActive) {
            val contentText = timeState.nextSyncAtEpochMillis.value?.let { nextSync ->
                val remaining = maxOf(0L, nextSync - System.currentTimeMillis())
                getString(
                    R.string.engine_notification_next_sync,
                    formatCountdown(remaining),
                )
            } ?: getString(R.string.engine_notification_text)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, buildForegroundNotification(contentText))
            delay(TICKER_INTERVAL_MS)
        }
    }

    private suspend fun CoroutineScope.runSyncLoop() {
        while (isActive) {
            val intervalMs = pollIntervalMs()
            val scheduled = maxOf(
                timeState.nextSyncAtEpochMillis.value ?: 0L,
                System.currentTimeMillis(),
            ) + intervalMs
            timeState.updateNextSync(scheduled)

            val syncResult = runCatching { syncEngine.syncOnce() }
                .getOrElse {
                    Log.e(TAG, "Sinkronisasi gagal", it)
                    SyncResult(outcome = SyncOutcome.RETRYABLE_ERROR)
                }
            syncResult.flowResults.forEach { notifier.notifyResult(it) }

            when (syncResult.outcome) {
                SyncOutcome.SESSION_EXPIRED -> {
                    Log.d(TAG, "Sesi berakhir, menghentikan layanan")
                    timeState.updateNextSync(null)
                    sessionEventBus.emit()
                    notifySessionExpired()
                    stopSelf()
                    return
                }
                SyncOutcome.RETRYABLE_ERROR -> {
                    timeState.updateNextSync(System.currentTimeMillis() + RETRY_BACKOFF_MS)
                    delay(RETRY_BACKOFF_MS)
                }
                SyncOutcome.OK -> delay(maxOf(0L, scheduled - System.currentTimeMillis()))
            }
        }
    }

    private suspend fun pollIntervalMs(): Long {
        val minutes = settings.pollIntervalMinutes.first()
        return maxOf(minutes * 60_000L, MIN_POLL_INTERVAL_MS)
    }

    private fun notifySessionExpired() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            SESSION_EXPIRED_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.engine_session_expired_title))
                .setContentText(getString(R.string.engine_session_expired_text))
                .setContentIntent(NotificationIntentHelper.openAppPendingIntent(this))
                .setAutoCancel(true)
                .setOngoing(true)
                .build(),
        )
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.engine_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun formatCountdown(remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private companion object {
        const val TAG = "AltEtholEngine"
        const val CHANNEL_ID = "altethol_engine"
        const val NOTIFICATION_ID = 1001
        const val SESSION_EXPIRED_NOTIFICATION_ID = 1002
        const val MIN_POLL_INTERVAL_MS = 3 * 60 * 1000L
        const val RETRY_BACKOFF_MS = 30_000L
        const val TICKER_INTERVAL_MS = 1_000L
    }
}