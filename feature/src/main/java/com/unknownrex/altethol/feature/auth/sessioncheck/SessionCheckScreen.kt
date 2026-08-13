package com.unknownrex.altethol.feature.auth.sessioncheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unknownrex.altethol.core.ui.ObserveAsEvents
import com.unknownrex.altethol.core.ui.components.ErrorState
import com.unknownrex.altethol.core.ui.components.LoadingIndicator
import org.koin.androidx.compose.koinViewModel

@Composable
fun SessionCheckRoot(
    onLoginRequired: () -> Unit,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionCheckViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SessionCheckEvent.NavigateToLogin -> onLoginRequired()
            SessionCheckEvent.NavigateToHome -> onAuthenticated()
        }
    }

    SessionCheckScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun SessionCheckScreen(
    state: SessionCheckState,
    onAction: (SessionCheckAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        SessionCheckState.Checking -> LoadingIndicator(modifier)

        is SessionCheckState.Error -> ErrorState(
            message = state.message,
            modifier = modifier,
            onRetry = { onAction(SessionCheckAction.Retry) },
        )
    }
}
