package com.unknownrex.altethol.core.common.result

import com.unknownrex.altethol.core.common.error.Error as DomainError

sealed interface Result<out D, out E : DomainError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : DomainError>(val error: E) : Result<Nothing, E>
}

typealias EmptyResult<E> = Result<Unit, E>
