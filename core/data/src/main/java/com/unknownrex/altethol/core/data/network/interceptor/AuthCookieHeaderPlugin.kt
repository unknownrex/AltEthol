package com.unknownrex.altethol.core.data.network.interceptor

import android.util.Log
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin

internal class AuthCookieHeaderPluginConfig {
    var authCookieProvider: suspend () -> String? = { null }
}

internal val AuthCookieHeaderPlugin: ClientPlugin<AuthCookieHeaderPluginConfig> =
    createClientPlugin("AuthCookieHeaderPlugin", ::AuthCookieHeaderPluginConfig) {
        val authCookieProvider = pluginConfig.authCookieProvider
        onRequest { request, _ ->
            try {
                val cookie = authCookieProvider()
                if (!cookie.isNullOrBlank()) {
                    request.headers.append("Cookie", cookie)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get auth cookie: ${e::class.simpleName}: ${e.message}", e)
            }
        }
    }

private const val TAG = "AltEtholAuthCookie"