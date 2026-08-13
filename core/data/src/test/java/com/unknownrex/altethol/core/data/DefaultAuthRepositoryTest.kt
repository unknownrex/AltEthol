package com.unknownrex.altethol.core.data.remote

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.data.network.safeCall.get
import com.unknownrex.altethol.core.data.remote.dto.ValidasiTokenDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull

class DefaultAuthRepositoryTest {

    private fun clientReturning(
        status: HttpStatusCode,
        body: String = "",
    ): HttpClient = HttpClient(
        MockEngine { respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json")) },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest { url("https://ethol.pens.ac.id") }
    }

    private val repo = { client: HttpClient -> DefaultAuthRepository(client) }

    @Test
    fun `validasi token success returns token payload`() = runTest {
        val body = """{"nomor":28801,"nipnrp":"3122500019","nama":"Rey","hakAkses":["mahasiswa"]}"""
        val repository = repo(clientReturning(HttpStatusCode.OK, body))

        val result = repository.validateToken()

        assertThat(result).isEqualTo(
            Result.Success(
                ValidasiTokenDto(
                    nomor = 28801,
                    nipnrp = "3122500019",
                    nama = "Rey",
                    hakAkses = listOf("mahasiswa"),
                ),
            ),
        )
    }

    @Test
    fun `401 maps to unauthorized`() = runTest {
        val repository = repo(clientReturning(HttpStatusCode.Unauthorized))

        val result = repository.validateToken()

        assertThat(result).isEqualTo(Result.Error(DataError.Network.UNAUTHORIZED))
    }

    @Test
    fun `403 maps to forbidden`() = runTest {
        val repository = repo(clientReturning(HttpStatusCode.Forbidden))

        val result = repository.validateToken()

        assertThat(result).isEqualTo(Result.Error(DataError.Network.FORBIDDEN))
    }

    @Test
    fun `successful response carries a non-null payload`() = runTest {
        val body = """{"nomor":1,"nipnrp":"x","nama":"y"}"""
        val repository = repo(clientReturning(HttpStatusCode.OK, body))

        val result = repository.validateToken()

        assertThat((result as Result.Success).data).isNotNull()
    }
}
