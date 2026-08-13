package com.unknownrex.altethol.feature.home.engine

import android.util.Log
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.remote.AttendanceRepository
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.remote.dto.KuliahDto
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import com.unknownrex.altethol.core.data.session.SessionStorage
import kotlinx.coroutines.flow.first
data class AttendanceContext(
    val kuliah: Int,
    val jenisSchema: Int,
    val kuliahAsal: Int,
    val mahasiswa: Int,
    val matakuliah: String,
)

interface PresensiContextResolver {
    suspend fun resolve(notif: NotifikasiDto): Result<AttendanceContext, DataError.Network>
}

class DefaultPresensiContextResolver(
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val sessionStorage: SessionStorage,
) : PresensiContextResolver {

    override suspend fun resolve(notif: NotifikasiDto): Result<AttendanceContext, DataError.Network> {
        val parts = notif.dataTerkait?.split("-") ?: return Result.Error(DataError.Network.SERIALIZATION)
        val kuliah = parts.getOrNull(0)?.toIntOrNull()
            ?: return Result.Error(DataError.Network.SERIALIZATION)
        val jenisSchema = parts.getOrNull(1)?.toIntOrNull()
            ?: return Result.Error(DataError.Network.SERIALIZATION)

        val mahasiswa = sessionStorage.session.first().mahasiswaId
            ?: validateNomor()
            ?: return Result.Error(DataError.Network.UNKNOWN)

        val course = resolveCourseDetail(kuliah, jenisSchema)

        return Result.Success(
            AttendanceContext(
                kuliah = kuliah,
                jenisSchema = jenisSchema,
                kuliahAsal = course?.kuliahAsal ?: kuliah,
                mahasiswa = mahasiswa,
                matakuliah = course?.matakuliah?.nama.orEmpty(),
            ),
        )
    }

    private suspend fun validateNomor(): Int? = when (val result = authRepository.validateToken()) {
        is Result.Success -> result.data.nomor
        is Result.Error -> null
    }

    private suspend fun resolveCourseDetail(kuliah: Int, jenisSchema: Int): KuliahDto? =
        when (val result = attendanceRepository.getCourseDetail(kuliah, jenisSchema)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
}
