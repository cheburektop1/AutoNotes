package com.example.autonotes.ui.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.autonotes.data.RecordingRepository
import com.example.autonotes.data.db.MarkerType
import com.example.autonotes.data.db.RecordingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface TimelineEvent {
    val timeMs: Long
    data class Marker(override val timeMs: Long, val type: MarkerType) : TimelineEvent
    data class Note(override val timeMs: Long, val text: String) : TimelineEvent
    data class Photo(override val timeMs: Long, val filePath: String) : TimelineEvent
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository
) : ViewModel() {

    private val recordingId: Long = checkNotNull(savedStateHandle.get<Long>("recordingId"))

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true // управлять аудиофокусом
        )
        .setHandleAudioBecomingNoisy(true) // пауза при отключении наушников
        .build()

    private val _recording = MutableStateFlow<RecordingEntity?>(null)
    val recording: StateFlow<RecordingEntity?> = _recording.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val events: StateFlow<List<TimelineEvent>> = combine(
        repository.observeMarkers(recordingId),
        repository.observeNotes(recordingId),
        repository.observePhotos(recordingId)
    ) { markers, notes, photos ->
        (markers.map { TimelineEvent.Marker(it.timeMs, it.type) } +
                notes.map { TimelineEvent.Note(it.timeMs, it.text) } +
                photos.map { TimelineEvent.Photo(it.timeMs, it.filePath) })
            .sortedBy { it.timeMs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Ошибка воспроизведения", error)
                _error.value = "Ошибка воспроизведения (${error.errorCodeName})"
            }
        })

        viewModelScope.launch {
            val rec = repository.getRecording(recordingId) ?: return@launch
            _recording.value = rec

            val file = File(rec.filePath)
            Log.d(TAG, "Файл: ${file.absolutePath}, существует=${file.exists()}, размер=${file.length()} байт")
            if (!file.exists() || file.length() <= 44) {
                _error.value = "Файл записи пуст или не найден"
                return@launch
            }


            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.prepare()
        }

        viewModelScope.launch {
            while (isActive) {
                _positionMs.value = player.currentPosition
                delay(200)
            }
        }
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun pause() = player.pause()

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        _positionMs.value = ms
    }

    fun seekBy(deltaMs: Long) {
        val duration = _recording.value?.durationMs ?: return
        seekTo((player.currentPosition + deltaMs).coerceIn(0L, duration))
    }

    fun rename(title: String) {
        val rec = _recording.value ?: return
        viewModelScope.launch {
            repository.rename(rec.id, title)
            _recording.value = rec.copy(title = title)
        }
    }

    override fun onCleared() {
        player.release()
    }

    private companion object {
        const val TAG = "AutoNotesPlayer"
    }
}