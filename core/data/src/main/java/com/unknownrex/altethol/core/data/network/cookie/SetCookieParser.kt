package com.unknownrex.altethol.core.data.network.cookie

object SetCookieParser {

    fun extractToken(setCookies: List<String>): String? = extract(setCookies, "token")

    fun extract(setCookies: List<String>, key: String): String? {
        for (setCookie in setCookies) {
            val pair = setCookie.substringBefore(";").trim()
            if (pair.isEmpty()) continue
            val separator = pair.indexOf('=')
            if (separator <= 0) continue
            val name = pair.substring(0, separator).trim()
            if (name.equals(key, ignoreCase = true)) {
                return pair.substring(separator + 1).trim().ifBlank { null }
            }
        }
        return null
    }
}