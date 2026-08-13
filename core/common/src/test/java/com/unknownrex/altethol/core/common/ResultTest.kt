package com.unknownrex.altethol.core.common

import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.common.result.Result
import com.unknownrex.altethol.core.common.result.asEmptyResult
import com.unknownrex.altethol.core.common.result.map
import com.unknownrex.altethol.core.common.result.onFailure
import com.unknownrex.altethol.core.common.result.onSuccess
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo

class ResultTest {

    @Test
    fun `map transforms success data`() {
        val result: Result<Int, DataError.Network> = Result.Success(2)

        val mapped = result.map { it * 3 }

        assertThat(mapped).isEqualTo(Result.Success(6))
    }

    @Test
    fun `map preserves error unchanged`() {
        val result: Result<Int, DataError.Network> = Result.Error(DataError.Network.NO_INTERNET)

        val mapped = result.map { it * 3 }

        assertThat(mapped).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    @Test
    fun `onSuccess runs action only on success`() {
        val called = mutableListOf<Int>()
        val success: Result<Int, DataError.Network> = Result.Success(4)
        val failure: Result<Int, DataError.Network> = Result.Error(DataError.Network.UNKNOWN)

        success.onSuccess { called.add(it) }
        failure.onSuccess { called.add(it) }

        assertThat(called).isEqualTo(listOf(4))
    }

    @Test
    fun `onFailure runs action only on failure`() {
        val errors = mutableListOf<DataError.Network>()

        Result.Success(4).onFailure { errors.add(it) }
        Result.Error(DataError.Network.FORBIDDEN).onFailure { errors.add(it) }

        assertThat(errors).isEqualTo(listOf(DataError.Network.FORBIDDEN))
    }

    @Test
    fun `asEmptyResult converts success to unit`() {
        val result: Result<Int, DataError.Network> = Result.Success(5)

        assertThat(result.asEmptyResult()).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun `asEmptyResult keeps error type`() {
        val result: Result<Int, DataError.Network> = Result.Error(DataError.Network.SERVER_ERROR)

        assertThat(result.asEmptyResult()).isEqualTo(Result.Error(DataError.Network.SERVER_ERROR))
    }
}
