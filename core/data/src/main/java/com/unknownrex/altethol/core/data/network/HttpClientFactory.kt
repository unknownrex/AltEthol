package com.unknownrex.altethol.core.data.network

import com.unknownrex.altethol.core.data.network.interceptor.CookieHeaderPlugin
import com.unknownrex.altethol.core.data.network.interceptor.TokenHeaderPlugin
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    fun create(
        baseUrl: String,
        tokenProvider: suspend () -> String?,
        cookieProvider: suspend () -> String?,
        engine: HttpClientEngine = OkHttp.create(),
        loggingLevel: LogLevel = LogLevel.BODY,
    ): HttpClient = HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        install(Logging) {
            logger = Logger.ANDROID
            level = loggingLevel
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 15_000
        }

        install(TokenHeaderPlugin) {
            this.tokenProvider = tokenProvider
        }
        install(CookieHeaderPlugin) {
            this.cookieProvider = cookieProvider
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            url(baseUrl)
        }
    }
}
