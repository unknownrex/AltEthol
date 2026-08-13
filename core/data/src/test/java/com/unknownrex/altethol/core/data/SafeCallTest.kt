package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.network.safeCall.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.nio.channels.UnresolvedAddressException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo

class SafeCallTest {

    @Serializable
    private data class FakeDto(val id: String)

    private fun clientReturning(status: HttpStatusCode, body: String = ""): HttpClient =
        HttpClient(MockEngine { request ->
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            defaultRequest { url("https://ethol.test/api") }
        }

    @Test
    fun `2xx maps to success`() = runTest {
        val client = clientReturning(HttpStatusCode.OK, """{"id":"1"}""")

        val result = client.get<FakeDto>("fakes/1")

        assertThat(result).isEqualTo(Result.Success(FakeDto("1")))
    }

    @Test
    fun `401 maps to unauthorized`() = runTest {
        val client = clientReturning(HttpStatusCode.Unauthorized)

        val result = client.get<FakeDto>("fakes/1")

        assertThat(result).isEqualTo(Result.Error(DataError.Network.UNAUTHORIZED))
    }

    @Test
    fun `500 maps to server error`() = runTest {
        val client = clientReturning(HttpStatusCode.InternalServerError)

        val result = client.get<FakeDto>("fakes/1")

        assertThat(result).isEqualTo(Result.Error(DataError.Network.SERVER_ERROR))
    }

    @Test
    fun `root relative route keeps the default host`() = runTest {
        var capturedPath: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = """{"id":"1"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json() }
            defaultRequest { url("https://ethol.test/api") }
        }

        client.get<FakeDto>("/api/fakes/1")

        assertThat(capturedPath).isEqualTo("/api/fakes/1")
    }

    @Test
    fun `unresolved address maps to no internet`() = runTest {
        val client = HttpClient(MockEngine { request ->
            throw UnresolvedAddressException()
        }) {
            defaultRequest { url("https://ethol.test/api") }
        }

        val result = client.get<FakeDto>("fakes/1")

        assertThat(result).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }
}
