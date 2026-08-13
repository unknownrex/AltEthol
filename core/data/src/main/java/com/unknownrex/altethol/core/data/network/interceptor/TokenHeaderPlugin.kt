package com.unknownrex.altethol.core.data.network.interceptor

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin

internal class TokenHeaderPluginConfig {
    var tokenProvider: suspend () -> String? = { null }
}

internal val TokenHeaderPlugin: ClientPlugin<TokenHeaderPluginConfig> =
    createClientPlugin("TokenHeaderPlugin", ::TokenHeaderPluginConfig) {
        val tokenProvider = pluginConfig.tokenProvider
        onRequest { request, _ ->
            val token = tokenProvider()
            if (!token.isNullOrBlank()) {
                request.headers.append("Token", token)
            }
        }
    }
