package com.unknownrex.altethol.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unknownrex.altethol.core.data.settings.SettingsStorage
import com.unknownrex.altethol.core.ui.ObserveAsEvents
import com.unknownrex.altethol.core.ui.text.UiText
import com.unknownrex.altethol.core.ui.theme.BorderPrimary
import com.unknownrex.altethol.core.ui.theme.BrandBlue
import com.unknownrex.altethol.core.ui.theme.SuccessGreen
import com.unknownrex.altethol.core.ui.theme.SurfaceSubtle
import com.unknownrex.altethol.core.ui.theme.SurfaceWhite
import com.unknownrex.altethol.core.ui.theme.TextMuted
import com.unknownrex.altethol.core.ui.theme.TextPrimary
import com.unknownrex.altethol.core.ui.theme.TextSecondary
import com.unknownrex.altethol.feature.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun HomeRoot(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSessionExpiredDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onAction(HomeAction.OnToggleEngine(true))
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.notification_permission_required))
            }
        }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.ShowMessage -> {
                val message = when (val text = event.message) {
                    is UiText.StringResource -> context.getString(text.id, *text.args)
                    is UiText.DynamicString -> text.value
                }
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }

            HomeEvent.ShowSessionExpired -> showSessionExpiredDialog = true
        }
    }

    if (showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { showSessionExpiredDialog = false },
            title = {
                Text(stringResource(R.string.engine_session_expired_title))
            },
            text = {
                Text(stringResource(R.string.engine_session_expired_dialog_text))
            },
            confirmButton = {
                TextButton(onClick = onNavigateToLogin) {
                    Text(stringResource(R.string.engine_session_expired_login_again))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionExpiredDialog = false }) {
                    Text(stringResource(R.string.engine_session_expired_later))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { HomeHeader(
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )}
    ) { innerPadding ->
        HomeScreen(
            state = state,
            onAction = { action ->
                when (action) {
                    is HomeAction.OnToggleEngine -> {
                        val needsPermission = action.enabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onAction(action)
                        }
                    }

                    else -> viewModel.onAction(action)
                }
            },
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {

        Spacer(Modifier.height(24.dp))
        StatusCard(
            enabled = state.engineEnabled,
            onToggle = { onAction(HomeAction.OnToggleEngine(it)) },
            intervalSeconds = state.pollIntervalMinutes * 60L,
            nextSyncAtEpochMillis = state.nextSyncAtEpochMillis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(HomeAction.OnAbsenNow) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandBlue,
                contentColor = Color.White,
                disabledContainerColor = BrandBlue.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
            elevation = null,
            enabled = !state.isAbsenNowRunning,
        ) {
            if (state.isAbsenNowRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.absen_now),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MetricCard(
                icon = Icons.Outlined.Sync,
                value = stringResource(R.string.interval_value_minutes, state.pollIntervalMinutes),
                label = stringResource(R.string.metric_interval_label),
                iconTint = BrandBlue,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                icon = Icons.Outlined.CheckCircle,
                value = state.totalAttendanceSuccess.toString(),
                label = stringResource(R.string.metric_attendance_label),
                iconTint = SuccessGreen,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeHeader(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            /*Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )*/
            Image(painter = painterResource(id = R.drawable.altethol_logo), contentDescription = "App Logo", modifier = Modifier.size(40.dp))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenHistory, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = stringResource(R.string.cd_history),
                    tint = TextSecondary,
                )
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = TextSecondary,
                )
            }
        }
        //Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = BorderPrimary, thickness = 1.dp)
    }
}

@Composable
private fun StatusCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    intervalSeconds: Long,
    nextSyncAtEpochMillis: Long?,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(24.dp)
    val remainingSeconds = rememberRemainingSeconds(
        enabled = enabled,
        nextSyncAtEpochMillis = nextSyncAtEpochMillis,
        fallbackTotalSeconds = intervalSeconds,
    )
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(SurfaceWhite)
            .border(1.dp, BorderPrimary, cardShape)
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (enabled) R.string.engine_active else R.string.engine_inactive,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) BrandBlue else TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (enabled) R.string.engine_active_desc else R.string.engine_inactive_desc,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.width(16.dp))
            EngineSwitch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = BorderPrimary, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.request_next_label),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (enabled) {
                    formatCountdown(remainingSeconds)
                } else {
                    stringResource(R.string.request_next_paused)
                },
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun EngineSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) (112.dp - 52.dp - 12.dp) else 0.dp,
    )
    Box(
        modifier = modifier
            .width(112.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) BrandBlue else BorderPrimary)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = 6.dp + knobOffset)
                .size(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White),
        )
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(SurfaceWhite)
            .border(1.dp, BorderPrimary, cardShape)
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceSubtle),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun rememberRemainingSeconds(
    enabled: Boolean,
    nextSyncAtEpochMillis: Long?,
    fallbackTotalSeconds: Long,
): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(enabled, nextSyncAtEpochMillis) {
        if (!enabled || nextSyncAtEpochMillis == null) {
            now = System.currentTimeMillis()
            return@LaunchedEffect
        }
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    if (!enabled) return 0L
    val deadline = nextSyncAtEpochMillis ?: (System.currentTimeMillis() + fallbackTotalSeconds * 1000L)
    return maxOf(0L, (deadline - now + 999L) / 1000L)
}

private fun formatCountdown(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(state = HomeState(false, SettingsStorage.DEFAULT_POLL_INTERVAL_MINUTES), onAction = {}, onOpenHistory = { /*TODO*/ }, onOpenSettings = { /*TODO*/ })

}