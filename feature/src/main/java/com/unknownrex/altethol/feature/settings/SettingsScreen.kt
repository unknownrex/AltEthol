package com.unknownrex.altethol.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unknownrex.altethol.core.ui.ObserveAsEvents
import com.unknownrex.altethol.core.ui.theme.BorderPrimary
import com.unknownrex.altethol.core.ui.theme.BrandBlue
import com.unknownrex.altethol.core.ui.theme.ErrorRed
import com.unknownrex.altethol.core.ui.theme.SurfaceSubtle
import com.unknownrex.altethol.core.ui.theme.SurfaceWhite
import com.unknownrex.altethol.core.ui.theme.TextMuted
import com.unknownrex.altethol.core.ui.theme.TextPrimary
import com.unknownrex.altethol.core.ui.theme.TextSecondary
import com.unknownrex.altethol.feature.R
import org.koin.androidx.compose.koinViewModel

private val INTERVAL_OPTIONS = listOf(3, 5, 10, 15, 30)

@Composable
fun SettingsRoot(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateToLogin -> onLoggedOut()
        }
    }

    SettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isBatteryExempt = rememberBatteryExemption(context)

    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(onBack = onBack)
        Spacer(Modifier.height(8.dp))

        SettingsSectionTitle(
            icon = Icons.Outlined.Schedule,
            title = stringResource(R.string.settings_interval_label),
            description = stringResource(R.string.settings_interval_desc),
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            INTERVAL_OPTIONS.forEach { minutes ->
                FilterChip(
                    selected = state.pollIntervalMinutes == minutes,
                    onClick = { onAction(SettingsAction.OnIntervalChanged(minutes)) },
                    label = {
                        Text(stringResource(R.string.interval_option_minutes, minutes))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlue,
                        selectedLabelColor = Color.White,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = state.pollIntervalMinutes == minutes,
                        borderColor = BorderPrimary,
                        selectedBorderColor = BrandBlue,
                    ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle(
            icon = Icons.Outlined.BatteryAlert,
            title = stringResource(R.string.settings_battery_label),
            description = if (isBatteryExempt) {
                stringResource(R.string.settings_battery_exempted)
            } else {
                stringResource(R.string.settings_battery_not_exempted)
            },
        )
        if (!isBatteryExempt) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { context.requestBatteryExemption() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                ),
                elevation = null,
            ) {
                Text(
                    text = stringResource(R.string.settings_battery_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.settings_about_label),
            description = stringResource(R.string.settings_disclaimer),
        )

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { showLogoutConfirm = true },
            enabled = !state.isLoggingOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorRed,
                contentColor = Color.White,
                disabledContainerColor = ErrorRed.copy(alpha = 0.5f),
            ),
            elevation = null,
        ) {
            Icon(
                imageVector = Icons.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_logout),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.settings_logout_confirm_title)) },
            text = { Text(stringResource(R.string.settings_logout_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onAction(SettingsAction.OnLogout)
                }) {
                    Text(
                        text = stringResource(R.string.settings_logout_confirm),
                        color = ErrorRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.settings_logout_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = TextSecondary,
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
        }
        HorizontalDivider(color = BorderPrimary, thickness = 1.dp)
    }
}

@Composable
private fun SettingsSectionTitle(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(cardShape)
            .background(SurfaceWhite)
            .border(1.dp, BorderPrimary, cardShape)
            .padding(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun rememberBatteryExemption(context: Context): Boolean {
    var isExempt by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isExempt = context.isIgnoringBatteryOptimizations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return isExempt
}

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(packageName)
}

private fun Context.requestBatteryExemption() {
    val intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:$packageName"),
    )
    runCatching { startActivity(intent) }
}
