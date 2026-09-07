package com.unknownrex.altethol.core.data.remote

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.network.cookie.SetCookieParser
import com.unknownrex.altethol.core.data.network.safeCall.get
import com.unknownrex.altethol.core.data.remote.dto.ValidasiTokenDto
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.cancellation.CancellationException

interface AuthRepository {
    suspend fun validateToken(): Result<ValidasiTokenDto, DataError.Network>

    /**
     * Exchanges the stored refresh_token for a fresh short-lived token.
     * Returns the new token parsed from the `Set-Cookie` response header.
     */
    suspend fun refreshToken(): Result<String, DataError.Network>
}

class DefaultAuthRepository(
    private val client: HttpClient,
) : AuthRepository {

    override suspend fun validateToken(): Result<ValidasiTokenDto, DataError.Network> =
        client.get("/api/auth/validasi-token")

    override suspend fun refreshToken(): Result<String, DataError.Network> {
        val response = try {
            client.post("/api/auth/refresh")
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnresolvedAddressException) {
            return Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: UnknownHostException) {
            return Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: ConnectTimeoutException) {
            return Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: SocketTimeoutException) {
            return Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: HttpRequestTimeoutException) {
            return Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: JsonConvertException) {
            return Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: Exception) {
            return Result.Error(DataError.Network.UNKNOWN)
        }
        return response.toTokenResult()
    }

    private suspend fun HttpResponse.toTokenResult(): Result<String, DataError.Network> {
        return when {
            status.isSuccess() -> {
                val token = SetCookieParser.extractToken(
                    headers.getAll(HttpHeaders.SetCookie).orEmpty(),
                )
                if (token != null) {
                    Result.Success(token)
                } else {
                    Result.Error(DataError.Network.SERIALIZATION)
                }
            }

            status.value == 401 -> Result.Error(DataError.Network.UNAUTHORIZED)
            status.value == 403 -> Result.Error(DataError.Network.FORBIDDEN)
            status.value == 400 -> Result.Error(DataError.Network.BAD_REQUEST)
            status.value == 408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
            status.value == 429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
            status.value in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
            else -> Result.Error(DataError.Network.UNKNOWN)
        }
    }
}