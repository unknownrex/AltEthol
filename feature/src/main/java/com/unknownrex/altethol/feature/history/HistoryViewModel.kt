package com.unknownrex.altethol.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknownrex.altethol.core.data.local.db.dao.AbsensiHistoriDao
import com.unknownrex.altethol.core.data.local.db.entity.AbsensiHistoriEntity
import com.unknownrex.altethol.core.data.model.AttendanceStatus
import com.unknownrex.altethol.core.data.model.AttendanceStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryItem(
    val id: Long,
    val matakuliah: String,
    val kuliahId: Int,
    val waktuDeteksi: Long,
    val waktuAbsenDieksekusi: Long,
    val status: AttendanceStatus,
    val failedStep: AttendanceStep?,
    val pesanServer: String,
)

data class HistoryState(
    val items: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = true,
)

class HistoryViewModel(
    private val dao: AbsensiHistoriDao,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            dao.observeAll().collect { entities ->
                _state.update {
                    HistoryState(
                        items = entities.map { it.toHistoryItem() },
                        isLoading = false,
                    )
                }
            }
        }
    }
}

private fun AbsensiHistoriEntity.toHistoryItem() = HistoryItem(
    id = id,
    matakuliah = matakuliah,
    kuliahId = kuliahId,
    waktuDeteksi = waktuDeteksi,
    waktuAbsenDieksekusi = waktuAbsenDieksekusi,
    status = status,
    failedStep = failedStep,
    pesanServer = pesanServer,
)
