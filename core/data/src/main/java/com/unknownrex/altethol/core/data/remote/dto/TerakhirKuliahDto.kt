package com.unknownrex.altethol.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TerakhirKuliahDto(
    val ditemukan: Boolean,
    val tanggal: String? = null,
    val kuliah: Int? = null,
    @SerialName("jenisSchema")
    val jenisSchema: Int? = null,
    val key: String? = null,
    val open: Boolean? = null,
    @SerialName("tanggal_format")
    val tanggalFormat: String? = null,
)
