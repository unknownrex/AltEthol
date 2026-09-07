package com.unknownrex.altethol.core.data

import com.unknownrex.altethol.core.data.network.cookie.SetCookieParser
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull

class SetCookieParserTest {

    @Test
    fun `extracts token from set-cookie with attributes`() {
        val setCookie = listOf("token=eyJhbGciOiJIUzI1NiJ9; Path=/; HttpOnly")

        assertThat(SetCookieParser.extractToken(setCookie)).isEqualTo("eyJhbGciOiJIUzI1NiJ9")
    }

    @Test
    fun `extracts token from multiple set-cookie headers`() {
        val setCookies = listOf(
            "refresh_token=rt; Path=/; HttpOnly",
            "token=new-token; Path=/",
        )

        assertThat(SetCookieParser.extractToken(setCookies)).isEqualTo("new-token")
    }

    @Test
    fun `returns null when no matching cookie`() {
        assertThat(SetCookieParser.extractToken(emptyList())).isNull()
        assertThat(SetCookieParser.extractToken(listOf("PHPSESSID=abc; Path=/"))).isNull()
    }

    @Test
    fun `cookie names are case insensitive`() {
        val setCookie = listOf("Token=Value=WithEquals; Path=/")

        assertThat(SetCookieParser.extractToken(setCookie)).isEqualTo("Value=WithEquals")
    }
}