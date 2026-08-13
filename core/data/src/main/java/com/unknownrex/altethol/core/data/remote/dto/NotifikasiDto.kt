package com.unknownrex.altethol.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotifikasiDto(
    @SerialName("idNotifikasi")
    val idNotifikasi: String,
    val keterangan: String,
    val status: String,
    @SerialName("urlWeb")
    val urlWeb: String,
    @SerialName("kodeNotifikasi")
    val kodeNotifikasi: String,
    @SerialName("dataTerkait")
    val dataTerkait: String? = null,
    @SerialName("createdAt")
    val createdAt: String,
    @SerialName("waktuNotifikasi")
    val waktuNotifikasi: String,
    @SerialName("createdAtIndonesia")
    val createdAtIndonesia: String,
)
