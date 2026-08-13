package com.unknownrex.altethol.feature.auth

object CookieParser {

    fun extractToken(cookie: String?): String? = parse(cookie)["token"]

    fun extractPhpSessId(cookie: String?): String? = parse(cookie)["phpsessid"]

    fun parse(cookie: String?): Map<String, String> {
        if (cookie.isNullOrBlank()) return emptyMap()
        return cookie.split(";")
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val key = pair.substring(0, separator).trim().lowercase()
                val value = pair.substring(separator + 1).trim()
                key to value
            }
            .toMap()
    }
}
