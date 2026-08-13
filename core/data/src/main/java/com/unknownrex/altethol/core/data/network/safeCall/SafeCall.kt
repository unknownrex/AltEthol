package com.unknownrex.altethol.core.data.network.safeCall

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse,
): Result<T, DataError.Network> {
    val response = try {
        execute()
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

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse,
): Result<T, DataError.Network> {
    return when {
        response.status.isSuccess() -> try {
            Result.Success(response.body<T>())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Network.SERIALIZATION)
        }

        response.status.value == 400 -> Result.Error(DataError.Network.BAD_REQUEST)
        response.status.value == 401 -> Result.Error(DataError.Network.UNAUTHORIZED)
        response.status.value == 403 -> Result.Error(DataError.Network.FORBIDDEN)
        response.status.value == 404 -> Result.Error(DataError.Network.NOT_FOUND)
        response.status.value == 408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        response.status.value == 409 -> Result.Error(DataError.Network.CONFLICT)
        response.status.value == 413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
        response.status.value == 429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
        response.status.value == 503 -> Result.Error(DataError.Network.SERVICE_UNAVAILABLE)
        response.status.value in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}
