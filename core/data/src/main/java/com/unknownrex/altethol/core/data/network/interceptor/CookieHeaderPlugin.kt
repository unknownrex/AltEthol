package com.unknownrex.altethol.core.data.network.interceptor

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin

internal class CookieHeaderPluginConfig {
    var cookieProvider: suspend () -> String? = { null }
}

internal val CookieHeaderPlugin: ClientPlugin<CookieHeaderPluginConfig> =
    createClientPlugin("CookieHeaderPlugin", ::CookieHeaderPluginConfig) {
        val cookieProvider = pluginConfig.cookieProvider
        onRequest { request, _ ->
            val cookie = cookieProvider()
            if (!cookie.isNullOrBlank()) {
                request.headers.append("Cookie", cookie)
            }
        }
    }
