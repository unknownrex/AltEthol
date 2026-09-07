package com.unknownrex.altethol.feature.home.engine

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

class AttendanceSyncService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncEngine: AttendanceSyncEngine by lazy { get() }
    private val settings: SettingsStorage by lazy { get() }
    private val notifier: AttendanceNotifier by lazy { get() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        serviceScope.launch { runSyncLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startAsForeground() {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.engine_notification_title))
            .setContentText(getString(R.string.engine_notification_text))
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun CoroutineScope.runSyncLoop() {
        while (isActive) {
            val syncResult = syncEngine.syncOnce()
            syncResult.flowResults.forEach { notifier.notifyResult(it) }
            if (syncResult.outcome == SyncOutcome.SESSION_EXPIRED) {
                Log.d(TAG, "Sesi berakhir, menghentikan layanan")
                notifySessionExpired()
                stopSelf()
                return
            }
            delay(pollIntervalMs())
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
                .setAutoCancel(true)
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

    private companion object {
        const val TAG = "AltEtholEngine"
        const val CHANNEL_ID = "altethol_engine"
        const val NOTIFICATION_ID = 1001
        const val SESSION_EXPIRED_NOTIFICATION_ID = 1002
        const val MIN_POLL_INTERVAL_MS = 3 * 60 * 1000L
    }
}
