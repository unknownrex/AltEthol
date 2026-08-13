package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.data.remote.dto.BacaNotifDto
import com.unknownrex.altethol.core.data.remote.dto.KuliahDto
import com.unknownrex.altethol.core.data.remote.dto.NotifikasiDto
import com.unknownrex.altethol.core.data.remote.dto.PresensiMahasiswaRequest
import com.unknownrex.altethol.core.data.remote.dto.TerakhirKuliahDto
import com.unknownrex.altethol.core.data.remote.dto.ValidasiTokenDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue

class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes notifikasi response`() {
        val payload = """
            {
              "idNotifikasi": "7798461e-7c93-4923-920e-b4f68bd82f18-28801",
              "keterangan": "Dosen telah melakukan presensi untuk matakuliah English for academic",
              "status": "2",
              "urlWeb": "/notifikasi/presensi/7798461e-7c93-4923-920e-b4f68bd82f18-28801",
              "kodeNotifikasi": "PRESENSI-KULIAH",
              "dataTerkait": "219110-4",
              "createdAt": "2026-06-18T07:58:51.000Z",
              "waktuNotifikasi": "18 Juni 2026 14:58",
              "createdAtIndonesia": "Kamis, 18 Juni 2026 - 14:58"
            }
        """.trimIndent()

        val dto = json.decodeFromString<NotifikasiDto>(payload)

        assertThat(dto.idNotifikasi).isEqualTo("7798461e-7c93-4923-920e-b4f68bd82f18-28801")
        assertThat(dto.kodeNotifikasi).isEqualTo("PRESENSI-KULIAH")
        assertThat(dto.status).isEqualTo("2")
        assertThat(dto.dataTerkait).isEqualTo("219110-4")
    }

    @Test
    fun `decodes validasi token response`() {
        val payload = """
            {
              "nomor": 28801,
              "nipnrp": "3122500019",
              "nama": "Rey",
              "hakAkses": ["mahasiswa"],
              "iat": 1779964109
            }
        """.trimIndent()

        val dto = json.decodeFromString<ValidasiTokenDto>(payload)

        assertThat(dto.nomor).isEqualTo(28801)
        assertThat(dto.hakAkses).isEqualTo(listOf("mahasiswa"))
        assertThat(dto.iat).isEqualTo(1779964109L)
    }

    @Test
    fun `decodes kuliah response with nested matakuliah`() {
        val payload = """
            {
              "nomor": 219110,
              "kuliah_asal": 219105,
              "jenisSchema": 4,
              "matakuliah": {
                "nomor": 202470,
                "nama": "English for academic",
                "jenisSchemaMk": 4
              },
              "dosen": "Dr. X",
              "gelar_dpn": null,
              "gelar_blk": "S.T.",
              "nip_dosen": "12345",
              "nomor_dosen": 1263,
              "kode_kelas": "8 STr TK",
              "pararel": "A"
            }
        """.trimIndent()

        val dto = json.decodeFromString<KuliahDto>(payload)

        assertThat(dto.nomor).isEqualTo(219110)
        assertThat(dto.kuliahAsal).isEqualTo(219105)
        assertThat(dto.jenisSchema).isEqualTo(4)
        assertThat(dto.matakuliah.nama).isEqualTo("English for academic")
        assertThat(dto.pararel).isEqualTo("A")
    }

    @Test
    fun `decodes terakhir kuliah response`() {
        val payload = """
            {
              "ditemukan": true,
              "tanggal": "2026-06-18 14:58:51",
              "kuliah": 219110,
              "jenisSchema": 4,
              "key": "DDgE7XAvex",
              "open": false,
              "tanggal_format": "Kamis, 18 Juni 2026 - 14:58:51"
            }
        """.trimIndent()

        val dto = json.decodeFromString<TerakhirKuliahDto>(payload)

        assertThat(dto.ditemukan).isTrue()
        assertThat(dto.key).isEqualTo("DDgE7XAvex")
        assertThat(dto.kuliah).isEqualTo(219110)
    }

    @Test
    fun `decodes baca notif response`() {
        val payload = """
            {
              "success": true,
              "message": "Berhasil menyimpan data !"
            }
        """.trimIndent()

        val dto = json.decodeFromString<BacaNotifDto>(payload)

        assertThat(dto.success).isTrue()
        assertThat(dto.message).isEqualTo("Berhasil menyimpan data !")
    }

    @Test
    fun `serializes presensi request with snake case fields`() {
        val request = PresensiMahasiswaRequest(
            kuliah = 219110,
            mahasiswa = 28801,
            jenisSchema = 4,
            kuliahAsal = 219105,
            key = "DDgE7XAvex",
        )

        val encoded = json.encodeToString(request)

        assertThat(encoded).contains("\"kuliah\":219110")
        assertThat(encoded).contains("\"jenis_schema\":4")
        assertThat(encoded).contains("\"kuliah_asal\":219105")
        assertThat(encoded).contains("\"key\":\"DDgE7XAvex\"")
    }
}
