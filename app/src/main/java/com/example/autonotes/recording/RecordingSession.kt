package com.example.autonotes.recording

import com.example.autonotes.data.db.MarkerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object RecordingSession {

    /** Отметка, сделанная во время записи в базу попадёт при нажатии «Стоп». */
    sealed interface PendingEvent {
        val timeMs: Long
        data class Marker(override val timeMs: Long, val type: MarkerType) : PendingEvent
        data class Note(override val timeMs: Long, val text: String) : PendingEvent
        data class Photo(override val timeMs: Long, val filePath: String) : PendingEvent
    }

    data class State(
        val isRecording: Boolean = false,
        val isPaused: Boolean = false,
        val events: List<PendingEvent> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile internal var recorder: AudioRecorder? = null

    fun positionMs(): Long = recorder?.elapsedMs ?: 0L
    fun level(): Float = recorder?.level ?: 0f
    internal fun update(transform: (State) -> State) = _state.update(transform)

    fun addMarker(type: MarkerType) = addEvent(PendingEvent.Marker(positionMs(), type))

    fun addNote(text: String, timeMs: Long) = addEvent(PendingEvent.Note(timeMs, text))

    fun addPhoto(filePath: String, timeMs: Long) = addEvent(PendingEvent.Photo(timeMs, filePath))

    private fun addEvent(event: PendingEvent) {
        _state.update { if (it.isRecording) it.copy(events = it.events + event) else it }
    }
}