package com.unknownrex.altethol.feature.home.engine

import com.unknownrex.altethol.core.data.local.db.entity.AbsensiHistoriEntity
import com.unknownrex.altethol.core.data.model.AttendanceStatus
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import java.time.OffsetDateTime

fun AttendanceFlowResult.toAbsensiHistoriEntity(
    notif: NotifikasiDto,
    now: Long = System.currentTimeMillis(),
): AbsensiHistoriEntity {
    val waktuDeteksi = parseIsoEpochMillis(notif.createdAt) ?: now
    val (status, failedStep, pesan) = when (this) {
        is AttendanceFlowResult.Submitted -> Triple(AttendanceStatus.SUCCESS, null, pesan.orEmpty())
        is AttendanceFlowResult.Failed -> Triple(AttendanceStatus.FAILED, failedStep, message)
        is AttendanceFlowResult.Skipped -> Triple(AttendanceStatus.FAILED, null, reason)
    }
    return AbsensiHistoriEntity(
        matakuliah = matakuliah,
        kuliahId = kuliahId,
        waktuDeteksi = waktuDeteksi,
        waktuAbsenDieksekusi = now,
        status = status,
        pesanServer = pesan,
        failedStep = failedStep,
    )
}

private fun parseIsoEpochMillis(iso: String): Long? =
    runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
