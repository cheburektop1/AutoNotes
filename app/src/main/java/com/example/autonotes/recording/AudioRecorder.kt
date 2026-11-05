package com.example.autonotes.recording

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Пишет звук с микрофона в WAV: 16 кГц, моно, 16 бит.
 * Этот формат напрямую принимают Vosk и большинство STT-сервисов.
 */
class AudioRecorder {

    @Volatile private var isRecording = false
    @Volatile private var isPaused = false
    @Volatile private var bytesWritten = 0L

    /** Громкость последнего фрагмента от 0 до 1 (для индикатора уровня). */
    @Volatile var level: Float = 0f
        private set

    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null
    private var outputFile: File? = null

    /** Позиция записи в мс по реально записанным данным (пауза не считается). */
    val elapsedMs: Long get() = bytesWritten * 1000L / BYTES_PER_SECOND

    @SuppressLint("MissingPermission") // разрешение проверяется в Activity до запуска сервиса
    fun start(file: File) {
        check(!isRecording) { "Запись уже идёт" }

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, SAMPLE_RATE) // не меньше ~0,5 с звука

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            error("Не удалось инициализировать микрофон")
        }

        outputFile = file
        bytesWritten = 0L
        isPaused = false
        isRecording = true
        audioRecord = record
        record.startRecording()

        worker = thread(name = "AudioRecorder") {
            FileOutputStream(file).use { out ->
                out.write(ByteArray(WAV_HEADER_SIZE)) // место под заголовок, заполним при остановке
                val buffer = ByteArray(READ_CHUNK_BYTES)
                while (isRecording) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read < 0) break
                    if (read > 0) {
                        level = if (isPaused) 0f else peakLevel(buffer, read)
                        if (!isPaused) { // на паузе читаем и выбрасываем
                            out.write(buffer, 0, read)
                            bytesWritten += read
                        }
                    }
                }
                level = 0f
            }
        }
    }

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }

    /** Останавливает запись, дописывает заголовок WAV и возвращает длительность в мс. */
    fun stop(): Long {
        if (!isRecording) return elapsedMs
        isRecording = false
        worker?.join()
        worker = null
        audioRecord?.let {
            runCatching { it.stop() }
            it.release()
        }
        audioRecord = null
        outputFile?.let { writeWavHeader(it, bytesWritten) }
        return elapsedMs
    }

    /** Пиковая амплитуда фрагмента; корень делает тихую речь заметной на индикаторе. */
    private fun peakLevel(buffer: ByteArray, length: Int): Float {
        var peak = 0
        var i = 0
        while (i + 1 < length) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toInt()
            val magnitude = if (sample < 0) -sample else sample
            if (magnitude > peak) peak = magnitude
            i += 2
        }
        return sqrt(peak / 32768f).coerceIn(0f, 1f)
    }

    private fun writeWavHeader(file: File, pcmBytes: Long) {
        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt((36 + pcmBytes).toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)                                   // размер блока fmt
            putShort(1)                                  // формат PCM
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(BYTES_PER_SECOND)
            putShort((CHANNELS * BITS_PER_SAMPLE / 8).toShort())
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray())
            putInt(pcmBytes.toInt())
        }
        RandomAccessFile(file, "rw").use {
            it.seek(0)
            it.write(header.array())
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val BYTES_PER_SECOND = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8 // 32 000
        private const val READ_CHUNK_BYTES = SAMPLE_RATE / 10 * BITS_PER_SAMPLE / 8      // 100 мс
        private const val WAV_HEADER_SIZE = 44
    }
}