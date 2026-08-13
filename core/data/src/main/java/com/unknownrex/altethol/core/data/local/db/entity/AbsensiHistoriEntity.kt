package com.unknownrex.altethol.core.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.unknownrex.altethol.core.data.model.AttendanceStatus
import com.unknownrex.altethol.core.data.model.AttendanceStep

@Entity(tableName = "absensi_histori")
data class AbsensiHistoriEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matakuliah: String,
    val kuliahId: Int,
    val waktuDeteksi: Long,
    val waktuAbsenDieksekusi: Long,
    val status: AttendanceStatus,
    val pesanServer: String,
    val failedStep: AttendanceStep? = null,
)
