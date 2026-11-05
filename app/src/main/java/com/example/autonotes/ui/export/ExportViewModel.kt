package com.example.autonotes.ui.export

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autonotes.export.ExportFormat
import com.example.autonotes.export.ReportExporter
import com.example.autonotes.export.ShareRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exporter: ReportExporter
) : ViewModel() {

    private val recordingId: Long = checkNotNull(savedStateHandle.get<Long>("recordingId"))

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _shareRequests = Channel<ShareRequest>(Channel.BUFFERED)
    val shareRequests = _shareRequests.receiveAsFlow()

    fun export(format: ExportFormat) {
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            _error.value = null
            try {
                val request = exporter.export(recordingId, format)
                if (request == null) _error.value = "Запись не найдена"
                else _shareRequests.send(request)
            } catch (e: Exception) {
                Log.e("AutoNotesExport", "Не удалось создать отчёт", e)
                _error.value = "Не удалось создать отчёт: ${e.message}"
            } finally {
                _exporting.value = false
            }
        }
    }
}