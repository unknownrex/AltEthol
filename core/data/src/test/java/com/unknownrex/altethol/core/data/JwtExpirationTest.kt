package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.data.jwt.JwtExpiration
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.util.Base64

class JwtExpirationTest {

    private fun jwt(exp: Long): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"alg":"HS256"}""".toByteArray(),
        )
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"sub":"28801","exp":$exp}""".toByteArray(),
        )
        val signature = Base64.getUrlEncoder().withoutPadding().encodeToString("sig".toByteArray())
        return "$header.$payload.$signature"
    }

    @Test
    fun `decodes exp claim from valid jwt`() {
        assertThat(JwtExpiration.expEpochSeconds(jwt(1779964109L))).isEqualTo(1779964109L)
    }

    @Test
    fun `returns null for blank or null token`() {
        assertThat(JwtExpiration.expEpochSeconds(null)).isNull()
        assertThat(JwtExpiration.expEpochSeconds("  ")).isNull()
    }

    @Test
    fun `returns null for malformed token`() {
        assertThat(JwtExpiration.expEpochSeconds("not-a-jwt")).isNull()
        assertThat(JwtExpiration.expEpochSeconds("a.")).isNull()
        assertThat(JwtExpiration.expEpochSeconds("a.!not-base64!.c")).isNull()
    }

    @Test
    fun `returns null when exp claim is missing`() {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"sub":"28801"}""".toByteArray(),
        )
        assertThat(JwtExpiration.expEpochSeconds("header.$payload.sig")).isNull()
    }
}