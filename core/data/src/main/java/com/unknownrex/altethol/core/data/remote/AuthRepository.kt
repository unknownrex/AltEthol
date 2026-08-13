package com.unknownrex.altethol.core.data.remote

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.network.safeCall.get
import com.unknownrex.altethol.core.data.remote.dto.ValidasiTokenDto
import io.ktor.client.HttpClient

interface AuthRepository {
    suspend fun validateToken(): Result<ValidasiTokenDto, DataError.Network>
}

class DefaultAuthRepository(
    private val client: HttpClient,
) : AuthRepository {

    override suspend fun validateToken(): Result<ValidasiTokenDto, DataError.Network> =
        client.get("/api/auth/validasi-token")
}
