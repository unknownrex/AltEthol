package com.unknownrex.altethol.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit) {
    LaunchedEffect(flow) {
        flow.collect { onEvent(it) }
    }
}
