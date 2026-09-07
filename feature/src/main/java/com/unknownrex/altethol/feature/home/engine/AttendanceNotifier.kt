package com.unknownrex.altethol.feature.home.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.unknownrex.altethol.feature.R

class AttendanceNotifier(
    private val context: Context,
) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.engine_result_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun notifyResult(result: AttendanceFlowResult) {
        when (result) {
            is AttendanceFlowResult.Submitted -> notify(
                id = result.idNotifikasi,
                title = result.matakuliah.ifBlank {
                    context.getString(R.string.result_success_title)
                },
                text = context.getString(R.string.result_success_text),
            )

            is AttendanceFlowResult.Failed -> notify(
                id = result.idNotifikasi,
                title = result.matakuliah.ifBlank {
                    context.getString(R.string.result_failed_title)
                },
                text = context.getString(R.string.result_failed_text, result.message),
            )

            is AttendanceFlowResult.Skipped -> Unit
        }
    }

    private fun notify(id: String, title: String, text: String) {
        manager.notify(
            id.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build(),
        )
    }

    private companion object {
        const val CHANNEL_ID = "altethol_results"
    }
}
