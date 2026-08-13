package com.unknownrex.altethol.core.data.remote

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.common.result.map
import com.unknownrex.altethol.core.data.network.safeCall.get
import com.unknownrex.altethol.core.data.network.safeCall.post
import com.unknownrex.altethol.core.data.network.safeCall.put
import com.unknownrex.altethol.core.data.remote.dto.BacaNotifDto
import com.unknownrex.altethol.core.data.remote.dto.BacaNotifRequest
import com.unknownrex.altethol.core.data.remote.dto.KuliahDto
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaRequest
import com.unknownrex.altethol.core.data.remote.dto.TerakhirKuliahDto
import io.ktor.client.HttpClient

interface AttendanceRepository {
    suspend fun fetchPresensiNotifications(): Result<List<NotifikasiDto>, DataError.Network>

    suspend fun markAsRead(idNotifikasi: String): Result<BacaNotifDto, DataError.Network>

    suspend fun getAttendanceKey(
        kuliah: Int,
        jenisSchema: Int,
    ): Result<TerakhirKuliahDto, DataError.Network>

    suspend fun submitAttendance(
        request: PresensiMahasiswaRequest,
    ): Result<PresensiMahasiswaDto, DataError.Network>

    suspend fun getCourseDetail(
        kuliah: Int,
        jenisSchema: Int,
    ): Result<KuliahDto?, DataError.Network>
}

class DefaultAttendanceRepository(
    private val client: HttpClient,
) : AttendanceRepository {

    override suspend fun fetchPresensiNotifications(): Result<List<NotifikasiDto>, DataError.Network> =
        client.get(
            route = "/api/notifikasi/mahasiswa",
            queryParameters = mapOf("filterNotif" to "PRESENSI"),
        )

    override suspend fun markAsRead(idNotifikasi: String): Result<BacaNotifDto, DataError.Network> =
        client.put(
            route = "/api/notifikasi/mahasiswa-baca-notif",
            body = BacaNotifRequest(idNotifikasi),
        )

    override suspend fun getAttendanceKey(
        kuliah: Int,
        jenisSchema: Int,
    ): Result<TerakhirKuliahDto, DataError.Network> =
        client.get(
            route = "/api/presensi/terakhir-kuliah",
            queryParameters = mapOf(
                "kuliah" to kuliah,
                "jenis_schema" to jenisSchema,
            ),
        )

    override suspend fun submitAttendance(
        request: PresensiMahasiswaRequest,
    ): Result<PresensiMahasiswaDto, DataError.Network> =
        client.post(
            route = "/api/presensi/mahasiswa",
            body = request,
        )

    override suspend fun getCourseDetail(
        kuliah: Int,
        jenisSchema: Int,
    ): Result<KuliahDto?, DataError.Network> =
        client.get<List<KuliahDto>>(
            route = "/api/kuliah/by-kuliah-js",
            queryParameters = mapOf(
                "kuliah" to kuliah,
                "jenisSchema" to jenisSchema,
            ),
        ).map { list -> list.firstOrNull { it.nomor == kuliah } ?: list.firstOrNull() }
}
