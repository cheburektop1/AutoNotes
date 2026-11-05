package com.example.autonotes.ui.recordings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autonotes.data.RecordingRepository
import com.example.autonotes.data.db.MarkerType
import com.example.autonotes.data.db.RecordingEntity
import com.example.autonotes.recording.RecordingService
import com.example.autonotes.recording.RecordingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val repository: RecordingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val recordings: StateFlow<List<RecordingEntity>> = repository.observeRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recordingState: StateFlow<RecordingSession.State> = RecordingSession.state

    fun startRecording() = RecordingService.start(context)

    fun stopRecording() = RecordingService.stop(context)

    fun togglePause() {
        if (RecordingSession.state.value.isPaused) RecordingService.resume(context)
        else RecordingService.pause(context)
    }

    fun addMarker(type: MarkerType) = RecordingSession.addMarker(type)

    fun addNote(text: String, timeMs: Long) = RecordingSession.addNote(text, timeMs)

    fun delete(recording: RecordingEntity) {
        viewModelScope.launch { repository.delete(recording) }
    }

    fun rename(recording: RecordingEntity, title: String) {
        viewModelScope.launch { repository.rename(recording.id, title) }
    }
}