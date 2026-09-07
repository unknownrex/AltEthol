package com.unknownrex.altethol.core.data.jwt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Base64

object JwtExpiration {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Decodes the JWT payload and returns the `exp` claim (epoch seconds, UTC).
     * Returns null when the token is absent, malformed, or missing `exp`.
     */
    fun expEpochSeconds(token: String?): Long? {
        if (token.isNullOrBlank()) return null
        val segments = token.split('.')
        if (segments.size < 2) return null

        val payload = runCatching { decodeBase64Url(segments[1]) }.getOrNull() ?: return null
        val jsonElement = runCatching { json.parseToJsonElement(payload) }.getOrNull() ?: return null
        return jsonElement.jsonObject["exp"]?.jsonPrimitive?.longOrNull
    }

    private fun decodeBase64Url(input: String): String {
        val padded = input + when (input.length % 4) {
            2 -> "=="
            3 -> "="
            else -> ""
        }
        return String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
    }
}