package com.unknownrex.altethol.feature.home.engine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidEngineController(
    private val appContext: Context,
) : EngineController {

    private val _enabled = MutableStateFlow(isEngineServiceRunning())
    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        val intent = Intent(appContext, AttendanceSyncService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(appContext, intent)
        } else {
            appContext.stopService(intent)
        }
        _enabled.value = enabled
    }

    private fun isEngineServiceRunning(): Boolean {
        val manager =
            appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == AttendanceSyncService::class.java.name }
    }
}
