package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.data.network.interceptor.CookieHeaderPlugin
import com.unknownrex.altethol.core.data.network.interceptor.TokenHeaderPlugin
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

class AuthHeaderPluginTest {

    @Test
    fun `attaches token header when token present`() = runTest {
        var capturedToken: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedToken = request.headers["Token"]
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json() }
            install(TokenHeaderPlugin) { tokenProvider = { "abc.def.ghi" } }
            defaultRequest { url("https://ethol.test/api") }
        }

        client.request("fakes/1")

        assertThat(capturedToken).isEqualTo("abc.def.ghi")
    }

    @Test
    fun `omits token header when token is null`() = runTest {
        var capturedToken: String? = "untouched"
        val client = HttpClient(MockEngine { request ->
            capturedToken = request.headers["Token"]
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json() }
            install(TokenHeaderPlugin) { tokenProvider = { null } }
            defaultRequest { url("https://ethol.test/api") }
        }

        client.request("fakes/1")

        assertThat(capturedToken).isNull()
    }

    @Test
    fun `attaches cookie header when cookie present`() = runTest {
        var capturedCookie: String? = null
        val client = HttpClient(MockEngine { request ->
            capturedCookie = request.headers["Cookie"]
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json() }
            install(CookieHeaderPlugin) { cookieProvider = { "PHPSESSID=xyz" } }
            defaultRequest { url("https://ethol.test/api") }
        }

        client.request("fakes/1")

        assertThat(capturedCookie).isEqualTo("PHPSESSID=xyz")
    }
}
