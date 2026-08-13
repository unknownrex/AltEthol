package com.unknownrex.altethol.core.data.remote

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.remote.dto.BacaNotifDto
import com.unknownrex.altethol.core.data.remote.dto.KuliahDto
import com.unknownrex.altethol.core.data.remote.dto.MatakuliahDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class DefaultAttendanceRepositoryTest {

    private fun buildClient(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (request: io.ktor.client.request.HttpRequestData) -> Unit = {},
    ): HttpClient = HttpClient(
        MockEngine { request ->
            onRequest(request)
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest {
            url("https://ethol.pens.ac.id")
            contentType(ContentType.Application.Json)
        }
    }

    private fun repo(client: HttpClient) = DefaultAttendanceRepository(client)

    @Test
    fun `fetch presensi notifications adds filter and parses list`() = runTest {
        val body = """[
            {
              "idNotifikasi": "n1-28801",
              "keterangan": "Dosen telah melakukan presensi",
              "status": "1",
              "urlWeb": "/notifikasi/presensi/n1-28801",
              "kodeNotifikasi": "PRESENSI-KULIAH",
              "dataTerkait": "219110-4",
              "createdAt": "2026-06-18T07:58:51.000Z",
              "waktuNotifikasi": "18 Juni 2026 14:58",
              "createdAtIndonesia": "Kamis, 18 Juni 2026 - 14:58"
            }
        ]"""
        var requestedUrl: String? = null
        val repository = repo(buildClient(body) { request -> requestedUrl = request.url.toString() })

        val result = repository.fetchPresensiNotifications()

        assertThat(requestedUrl).isEqualTo(
            "https://ethol.pens.ac.id/api/notifikasi/mahasiswa?filterNotif=PRESENSI",
        )
        val data = (result as Result.Success).data
        assertThat(data).hasSize(1)
        assertThat(data.first().idNotifikasi).isEqualTo("n1-28801")
        assertThat(data.first().kodeNotifikasi).isEqualTo("PRESENSI-KULIAH")
    }

    @Test
    fun `mark as read posts id and parses success`() = runTest {
        var requestedUrl: String? = null
        var requestBody: String? = null
        val repository = repo(
            buildClient(
                body = """{"success":true,"message":"Berhasil menyimpan data !"}""",
                status = HttpStatusCode.Created,
            ) { request ->
                requestedUrl = request.url.toString()
                requestBody = (request.body as TextContent).text
            },
        )

        val result = repository.markAsRead("n1-28801")

        assertThat(requestedUrl).isEqualTo(
            "https://ethol.pens.ac.id/api/notifikasi/mahasiswa-baca-notif",
        )
        assertThat(requestBody).isEqualTo("""{"idNotifikasi":"n1-28801"}""")
        assertThat(result).isEqualTo(
            Result.Success(BacaNotifDto(success = true, message = "Berhasil menyimpan data !")),
        )
    }

    @Test
    fun `get attendance key sends kuliah and jenis_schema`() = runTest {
        val body = """{"ditemukan":true,"tanggal":"2026-06-18 14:58:51","kuliah":219110,"jenisSchema":4,"key":"DDgE7XAvex","open":true}"""
        var requestedUrl: String? = null
        val repository = repo(buildClient(body) { request -> requestedUrl = request.url.toString() })

        val result = repository.getAttendanceKey(kuliah = 219110, jenisSchema = 4)

        assertThat(requestedUrl).isEqualTo(
            "https://ethol.pens.ac.id/api/presensi/terakhir-kuliah?kuliah=219110&jenis_schema=4",
        )
        assertThat((result as Result.Success).data.key).isEqualTo("DDgE7XAvex")
    }

    @Test
    fun `submit attendance sends numeric body with key`() = runTest {
        var requestBody: String? = null
        val repository = repo(
            buildClient(body = """{"sukses":true,"pesan":"Absen berhasil"}""") { request ->
                requestBody = (request.body as TextContent).text
            },
        )

        val result = repository.submitAttendance(
            PresensiMahasiswaRequest(
                kuliah = 219110,
                mahasiswa = 28801,
                jenisSchema = 4,
                kuliahAsal = 219105,
                key = "DDgE7XAvex",
            ),
        )

        assertThat(requestBody).isEqualTo(
            """{"kuliah":219110,"mahasiswa":28801,"jenis_schema":4,"kuliah_asal":219105,"key":"DDgE7XAvex"}""",
        )
        assertThat((result as Result.Success).data.pesan).isEqualTo("Absen berhasil")
    }

    @Test
    fun `get course detail returns matching course`() = runTest {
        val body = """[
            {
              "nomor": 219110,
              "kuliah_asal": 219105,
              "jenisSchema": 4,
              "matakuliah": {"nomor": 202470, "nama": "English for academic", "jenisSchemaMk": 4},
              "dosen": "Dosen A",
              "kode_kelas": "8 STr TK",
              "pararel": "A"
            }
        ]"""
        val repository = repo(buildClient(body))

        val result = repository.getCourseDetail(kuliah = 219110, jenisSchema = 4)

        assertThat(result).isEqualTo(
            Result.Success(
                KuliahDto(
                    nomor = 219110,
                    kuliahAsal = 219105,
                    jenisSchema = 4,
                    matakuliah = MatakuliahDto(nomor = 202470, nama = "English for academic", jenisSchemaMk = 4),
                    dosen = "Dosen A",
                    kodeKelas = "8 STr TK",
                    pararel = "A",
                ),
            ),
        )
    }

    @Test
    fun `course detail with no match returns null`() = runTest {
        val repository = repo(buildClient(body = "[]"))

        val result = repository.getCourseDetail(kuliah = 999, jenisSchema = 4)

        assertThat((result as Result.Success).data).isNull()
    }

    @Test
    fun `401 maps to unauthorized`() = runTest {
        val repository = repo(buildClient(body = "", status = HttpStatusCode.Unauthorized))

        val result = repository.fetchPresensiNotifications()

        assertThat(result).isEqualTo(Result.Error(DataError.Network.UNAUTHORIZED))
    }

    @Test
    fun `submit returns not-null payload`() = runTest {
        val repository = repo(buildClient(body = """{"sukses":true,"pesan":"ok"}"""))

        val result = repository.submitAttendance(
            PresensiMahasiswaRequest(1, 2, 3, 4, "KEY"),
        )

        assertThat((result as Result.Success).data).isNotNull()
    }
}
