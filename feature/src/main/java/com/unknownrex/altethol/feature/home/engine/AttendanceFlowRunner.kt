package com.unknownrex.altethol.feature.home.engine

import android.util.Log
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.model.AttendanceStep
import com.unknownrex.altethol.core.data.remote.AttendanceRepository
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaRequest

sealed interface AttendanceFlowResult {
    val idNotifikasi: String
    val matakuliah: String
    val kuliahId: Int
    val networkError: DataError.Network?

    data class Submitted(
        override val idNotifikasi: String,
        override val matakuliah: String,
        override val kuliahId: Int,
        val pesan: String?,
    ) : AttendanceFlowResult {
        override val networkError: DataError.Network? = null
    }

    data class Skipped(
        override val idNotifikasi: String,
        override val matakuliah: String,
        override val kuliahId: Int,
        val reason: String,
    ) : AttendanceFlowResult {
        override val networkError: DataError.Network? = null
    }

    data class Failed(
        override val idNotifikasi: String,
        override val matakuliah: String,
        override val kuliahId: Int,
        val failedStep: AttendanceStep?,
        val message: String,
        override val networkError: DataError.Network?,
    ) : AttendanceFlowResult
}

class AttendanceFlowRunner(
    private val repository: AttendanceRepository,
    private val contextResolver: PresensiContextResolver,
    private val log: (String) -> Unit = { Log.d("AltEtholEngine", it) },
) {

    suspend fun run(notif: NotifikasiDto): AttendanceFlowResult {
        log("Memproses notifikasi ${notif.idNotifikasi}")

        when (val result = repository.markAsRead(notif.idNotifikasi)) {
            is Result.Error -> {
                val message = "mark-as-read: ${result.error}"
                log("Step 1 gagal $message")
                return AttendanceFlowResult.Failed(
                    idNotifikasi = notif.idNotifikasi,
                    matakuliah = "",
                    kuliahId = parseKuliahId(notif),
                    failedStep = AttendanceStep.MARK_AS_READ,
                    message = message,
                    networkError = result.error,
                )
            }
            is Result.Success -> log("Step 1 mark-as-read sukses: ${result.data.message ?: "-"}")
        }

        val context = when (val result = contextResolver.resolve(notif)) {
            is Result.Error -> {
                val message = "resolve-context: ${result.error}"
                log("Resolusi konteks gagal $message")
                return AttendanceFlowResult.Failed(
                    idNotifikasi = notif.idNotifikasi,
                    matakuliah = "",
                    kuliahId = parseKuliahId(notif),
                    failedStep = null,
                    message = message,
                    networkError = result.error,
                )
            }
            is Result.Success -> result.data
        }

        val keyResult = repository.getAttendanceKey(context.kuliah, context.jenisSchema)
        when (keyResult) {
            is Result.Error -> {
                val message = "get-key: ${keyResult.error}"
                log("Step 2 gagal $message")
                return AttendanceFlowResult.Failed(
                    idNotifikasi = notif.idNotifikasi,
                    matakuliah = context.matakuliah,
                    kuliahId = context.kuliah,
                    failedStep = AttendanceStep.GET_KEY,
                    message = message,
                    networkError = keyResult.error,
                )
            }
            is Result.Success -> {
                val key = keyResult.data.key
                if (keyResult.data.ditemukan != true || keyResult.data.open != true || key.isNullOrBlank()) {
                    log("Skipped: tidak ada sesi presensi terbuka (${notif.idNotifikasi})")
                    return AttendanceFlowResult.Skipped(
                        idNotifikasi = notif.idNotifikasi,
                        matakuliah = context.matakuliah,
                        kuliahId = context.kuliah,
                        reason = "tidak ada sesi presensi terbuka",
                    )
                }
                log("Step 2 get-key sukses untuk ${context.matakuliah.ifBlank { context.kuliah.toString() }}")
                return submit(context, key, notif.idNotifikasi)
            }
        }
    }

    private fun parseKuliahId(notif: NotifikasiDto): Int =
        notif.dataTerkait?.substringBefore("-")?.toIntOrNull() ?: 0

    private suspend fun submit(
        context: AttendanceContext,
        key: String,
        idNotifikasi: String,
    ): AttendanceFlowResult {
        val result = repository.submitAttendance(
            PresensiMahasiswaRequest(
                kuliah = context.kuliah,
                mahasiswa = context.mahasiswa,
                jenisSchema = context.jenisSchema,
                kuliahAsal = context.kuliahAsal,
                key = key,
            ),
        )
        return when (result) {
            is Result.Error -> {
                val message = "submit: ${result.error}"
                log("Step 3 gagal $message")
                AttendanceFlowResult.Failed(
                    idNotifikasi = idNotifikasi,
                    matakuliah = context.matakuliah,
                    kuliahId = context.kuliah,
                    failedStep = AttendanceStep.SUBMIT,
                    message = message,
                    networkError = result.error,
                )
            }
            is Result.Success -> {
                log("Step 3 submit sukses: ${result.data.pesan ?: "-"}")
                AttendanceFlowResult.Submitted(
                    idNotifikasi = idNotifikasi,
                    matakuliah = context.matakuliah,
                    kuliahId = context.kuliah,
                    pesan = result.data.pesan,
                )
            }
        }
    }
}
