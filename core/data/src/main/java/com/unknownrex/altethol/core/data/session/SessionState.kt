package com.unknownrex.altethol.core.data.session

data class SessionState(
    val isLoggedIn: Boolean = false,
    val token: String? = null,
    val cookiePhpSessId: String? = null,
    val mahasiswaId: Int? = null,
) {
    val hasSession: Boolean get() = isLoggedIn && !token.isNullOrBlank()
}
