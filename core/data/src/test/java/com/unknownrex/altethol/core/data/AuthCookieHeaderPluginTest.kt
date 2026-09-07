package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.data.network.interceptor.AuthCookieHeaderPlugin
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull

class AuthCookieHeaderPluginTest {

    private fun client(authCookie: String?): Pair<HttpClient, () -> String?> {
        var captured: String? = null
        val httpClient = HttpClient(MockEngine { request ->
            captured = request.headers[HttpHeaders.Cookie]
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json() }
            install(AuthCookieHeaderPlugin) {
                authCookieProvider = { authCookie }
            }
            defaultRequest { url("https://ethol.test/api") }
        }
        return httpClient to { captured }
    }

    @Test
    fun `attaches combined cookie header`() = runTest {
        val (client, captured) = client("refresh_token=rt-abc; token=abc.def.ghi")

        client.request("fakes/1")

        assertThat(captured()).isEqualTo("refresh_token=rt-abc; token=abc.def.ghi")
    }

    @Test
    fun `omits cookie header when provider returns null`() = runTest {
        val (client, captured) = client(null)

        client.request("fakes/1")

        assertThat(captured()).isNull()
    }
}