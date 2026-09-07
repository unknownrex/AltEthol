package com.unknownrex.altethol.core.data.network.cookie

object AuthCookieBuilder {

    fun build(token: String?, refreshToken: String?): String? {
        val parts = listOfNotNull(
            refreshToken?.takeIf { it.isNotBlank() }?.let { "refresh_token=$it" },
            token?.takeIf { it.isNotBlank() }?.let { "token=$it" },
        )
        return parts.joinToString("; ").ifBlank { null }
    }
}