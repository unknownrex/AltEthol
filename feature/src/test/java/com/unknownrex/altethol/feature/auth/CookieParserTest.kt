package com.unknownrex.altethol.feature.auth

import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull

class CookieParserTest {

    @Test
    fun `extracts token and phpsessid from cookie string`() {
        val cookie = "PHPSESSID=abc123; token=eyJhbGciOiJIUzI1NiJ9; some_other=value"

        assertThat(CookieParser.extractToken(cookie)).isEqualTo("eyJhbGciOiJIUzI1NiJ9")
        assertThat(CookieParser.extractPhpSessId(cookie)).isEqualTo("abc123")
    }

    @Test
    fun `cookie names are case insensitive`() {
        val cookie = "Token=jwt-token; PhpSessId=sess"

        assertThat(CookieParser.extractToken(cookie)).isEqualTo("jwt-token")
        assertThat(CookieParser.extractPhpSessId(cookie)).isEqualTo("sess")
    }

    @Test
    fun `values containing equals sign are kept intact`() {
        val cookie = "token=abc=def"

        assertThat(CookieParser.extractToken(cookie)).isEqualTo("abc=def")
    }

    @Test
    fun `spaces around separators are trimmed`() {
        val cookie = "  token = trimmed  ; PHPSESSID=ok"

        assertThat(CookieParser.extractToken(cookie)).isEqualTo("trimmed")
        assertThat(CookieParser.extractPhpSessId(cookie)).isEqualTo("ok")
    }

    @Test
    fun `blank or null cookie yields empty map`() {
        assertThat(CookieParser.parse(null)).isEmpty()
        assertThat(CookieParser.parse("")).isEmpty()
        assertThat(CookieParser.extractToken("   ")).isNull()
    }

    @Test
    fun `missing token returns null`() {
        assertThat(CookieParser.extractToken("PHPSESSID=only")).isNull()
    }
}
