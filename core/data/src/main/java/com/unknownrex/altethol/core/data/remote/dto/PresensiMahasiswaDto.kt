package com.unknownrex.altethol.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresensiMahasiswaRequest(
    val kuliah: Int,
    val mahasiswa: Int,
    @SerialName("jenis_schema")
    val jenisSchema: Int,
    @SerialName("kuliah_asal")
    val kuliahAsal: Int,
    val key: String,
)

@Serializable
data class PresensiMahasiswaDto(
    val sukses: Boolean? = null,
    val pesan: String? = null,
)
