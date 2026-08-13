package com.unknownrex.altethol.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.unknownrex.altethol.core.common.error.DataError
import com.unknownrex.altethol.core.ui.R

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> stringResource(id, *args)
}

fun DataError.Network.toUiText(): UiText = when (this) {
    DataError.Network.BAD_REQUEST -> UiText.StringResource(R.string.error_bad_request)
    DataError.Network.REQUEST_TIMEOUT -> UiText.StringResource(R.string.error_request_timeout)
    DataError.Network.UNAUTHORIZED -> UiText.StringResource(R.string.error_unauthorized)
    DataError.Network.FORBIDDEN -> UiText.StringResource(R.string.error_forbidden)
    DataError.Network.NOT_FOUND -> UiText.StringResource(R.string.error_not_found)
    DataError.Network.CONFLICT -> UiText.StringResource(R.string.error_conflict)
    DataError.Network.TOO_MANY_REQUESTS -> UiText.StringResource(R.string.error_too_many_requests)
    DataError.Network.NO_INTERNET -> UiText.StringResource(R.string.error_no_internet)
    DataError.Network.PAYLOAD_TOO_LARGE -> UiText.StringResource(R.string.error_payload_too_large)
    DataError.Network.SERVER_ERROR -> UiText.StringResource(R.string.error_server)
    DataError.Network.SERVICE_UNAVAILABLE -> UiText.StringResource(R.string.error_service_unavailable)
    DataError.Network.SERIALIZATION -> UiText.StringResource(R.string.error_serialization)
    DataError.Network.UNKNOWN -> UiText.StringResource(R.string.error_unknown)
}

fun DataError.Local.toUiText(): UiText = when (this) {
    DataError.Local.DISK_FULL -> UiText.StringResource(R.string.error_disk_full)
    DataError.Local.NOT_FOUND -> UiText.StringResource(R.string.error_local_not_found)
    DataError.Local.UNKNOWN -> UiText.StringResource(R.string.error_unknown)
}
