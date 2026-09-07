package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.remote.DefaultAuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo

class AuthRepositoryRefreshTest {

    @Test
    fun `refresh posts to auth refresh and parses token from set-cookie`() = runTest {
        var method: HttpMethod? = null
        var url: String = ""
        val client = HttpClient(MockEngine { request ->
            method = request.method
            url = request.url.encodedPath
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.SetCookie,
                    "token=new-jwt-token; Path=/; HttpOnly",
                ),
            )
        })
        val repository: AuthRepository = DefaultAuthRepository(client)

        val result = repository.refreshToken()

        assertThat(method).isEqualTo(HttpMethod.Post)
        assertThat(url).isEqualTo("/api/auth/refresh")
        assertThat(result).isEqualTo(Result.Success("new-jwt-token"))
    }

    @Test
    fun `refresh returns unauthorized on 401`() = runTest {
        val client = HttpClient(MockEngine {
            respond(content = "", status = HttpStatusCode.Unauthorized)
        })
        val repository: AuthRepository = DefaultAuthRepository(client)

        val result = repository.refreshToken()

        assertThat(result).isEqualTo(Result.Error(DataError.Network.UNAUTHORIZED))
    }

    @Test
    fun `refresh returns serialization error when set-cookie missing`() = runTest {
        val client = HttpClient(MockEngine {
            respond(content = "", status = HttpStatusCode.OK)
        })
        val repository: AuthRepository = DefaultAuthRepository(client)

        val result = repository.refreshToken()

        assertThat(result).isEqualTo(Result.Error(DataError.Network.SERIALIZATION))
    }
}