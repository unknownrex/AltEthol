package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.data.network.cookie.AuthCookieBuilder
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull

class AuthCookieBuilderTest {

    @Test
    fun `builds combined cookie with refresh token first`() {
        val cookie = AuthCookieBuilder.build(token = "tok", refreshToken = "rt")

        assertThat(cookie).isEqualTo("refresh_token=rt; token=tok")
    }

    @Test
    fun `builds cookie with only token`() {
        val cookie = AuthCookieBuilder.build(token = "tok", refreshToken = null)

        assertThat(cookie).isEqualTo("token=tok")
    }

    @Test
    fun `builds cookie with only refresh token`() {
        val cookie = AuthCookieBuilder.build(token = null, refreshToken = "rt")

        assertThat(cookie).isEqualTo("refresh_token=rt")
    }

    @Test
    fun `returns null when both values are blank`() {
        assertThat(AuthCookieBuilder.build(null, null)).isNull()
        assertThat(AuthCookieBuilder.build("", "  ")).isNull()
    }
}