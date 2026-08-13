package com.unknownrex.altethol.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KuliahDto(
    val nomor: Int,
    @SerialName("kuliah_asal")
    val kuliahAsal: Int,
    @SerialName("jenisSchema")
    val jenisSchema: Int,
    val matakuliah: MatakuliahDto,
    val dosen: String? = null,
    @SerialName("gelar_dpn")
    val gelarDpn: String? = null,
    @SerialName("gelar_blk")
    val gelarBlk: String? = null,
    @SerialName("nip_dosen")
    val nipDosen: String? = null,
    @SerialName("nomor_dosen")
    val nomorDosen: Int? = null,
    @SerialName("kode_kelas")
    val kodeKelas: String? = null,
    val pararel: String? = null,
)

@Serializable
data class MatakuliahDto(
    val nomor: Int,
    val nama: String,
    @SerialName("jenisSchemaMk")
    val jenisSchemaMk: Int? = null,
)
