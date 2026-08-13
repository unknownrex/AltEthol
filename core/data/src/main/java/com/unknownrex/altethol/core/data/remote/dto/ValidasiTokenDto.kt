package com.unknownrex.altethol.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidasiTokenDto(
    val nomor: Int,
    val nipnrp: String,
    val nama: String,
    @SerialName("hakAkses")
    val hakAkses: List<String> = emptyList(),
    val iat: Long? = null,
)
