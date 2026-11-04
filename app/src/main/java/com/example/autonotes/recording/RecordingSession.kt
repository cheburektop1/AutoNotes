package com.example.autonotes.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object RecordingSession {

    data class State(
        val isRecording: Boolean = false,
        val isPaused: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile internal var recorder: AudioRecorder? = null

    /** Текущая позиция записи в мс. По ней ставим таймкоды маркеров, фото и заметок. */
    fun positionMs(): Long = recorder?.elapsedMs ?: 0L

    internal fun update(transform: (State) -> State) = _state.update(transform)
}