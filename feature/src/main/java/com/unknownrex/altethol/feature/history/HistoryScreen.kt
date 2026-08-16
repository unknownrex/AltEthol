package com.unknownrex.altethol.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unknownrex.altethol.core.data.model.AttendanceStatus
import com.unknownrex.altethol.core.data.model.AttendanceStep
import com.unknownrex.altethol.core.ui.components.LoadingIndicator
import com.unknownrex.altethol.core.ui.theme.BorderPrimary
import com.unknownrex.altethol.core.ui.theme.ErrorRed
import com.unknownrex.altethol.core.ui.theme.SuccessGreen
import com.unknownrex.altethol.core.ui.theme.SurfaceWhite
import com.unknownrex.altethol.core.ui.theme.TextMuted
import com.unknownrex.altethol.core.ui.theme.TextPrimary
import com.unknownrex.altethol.core.ui.theme.TextSecondary
import com.unknownrex.altethol.feature.R
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryRoot(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun HistoryScreen(
    state: HistoryState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        HistoryHeader(onBack = onBack)
        when {
            state.isLoading -> LoadingIndicator()
            state.items.isEmpty() -> EmptyHistory()
            else -> HistoryList(state.items)
        }
    }
}

@Composable
private fun HistoryHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
                    contentDescription = stringResource(R.string.history_back),
                    tint = TextSecondary,
                )
            }
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
        }
        HorizontalDivider(color = BorderPrimary, thickness = 1.dp)
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}

@Composable
private fun HistoryList(items: List<HistoryItem>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items, key = { it.id }) { item ->
            HistoryCard(item)
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, modifier: Modifier = Modifier) {
    val cardShape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(SurfaceWhite)
            .border(1.dp, BorderPrimary, cardShape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.matakuliah.ifBlank { item.kuliahId.toString() },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            StatusPill(item)
        }
        Spacer(Modifier.height(16.dp))
        TimeRow(
            label = stringResource(R.string.history_label_detected),
            value = formatEpoch(item.waktuDeteksi),
        )
        Spacer(Modifier.height(8.dp))
        TimeRow(
            label = stringResource(R.string.history_label_executed),
            value = formatEpoch(item.waktuAbsenDieksekusi),
        )
        if (item.pesanServer.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.pesanServer,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun StatusPill(item: HistoryItem, modifier: Modifier = Modifier) {
    val isSuccess = item.status == AttendanceStatus.SUCCESS
    val tint = if (isSuccess) SuccessGreen else ErrorRed
    val text = buildString {
        append(stringResource(if (isSuccess) R.string.history_status_success else R.string.history_status_failed))
        item.failedStep?.let { step ->
            append(" · ")
            append(
                when (step) {
                    AttendanceStep.MARK_AS_READ -> stringResource(R.string.history_step_mark_as_read)
                    AttendanceStep.GET_KEY -> stringResource(R.string.history_step_get_key)
                    AttendanceStep.SUBMIT -> stringResource(R.string.history_step_submit)
                },
            )
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

@Composable
private fun TimeRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

private val historyTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("id", "ID"))

private fun formatEpoch(epoch: Long): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(historyTimeFormatter)
