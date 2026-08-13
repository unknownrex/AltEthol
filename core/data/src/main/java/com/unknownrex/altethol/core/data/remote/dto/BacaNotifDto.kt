package com.unknownrex.altethol.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BacaNotifRequest(
    @SerialName("idNotifikasi")
    val idNotifikasi: String,
)

@Serializable
data class BacaNotifDto(
    val success: Boolean = false,
    val message: String? = null,
)
