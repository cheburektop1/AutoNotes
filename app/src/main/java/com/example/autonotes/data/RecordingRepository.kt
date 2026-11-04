package com.example.autonotes.data

import com.example.autonotes.data.db.RecordingDao
import com.example.autonotes.data.db.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val dao: RecordingDao
) {
    fun observeRecordings(): Flow<List<RecordingEntity>> = dao.observeAll()

    suspend fun getRecording(id: Long): RecordingEntity? = dao.getById(id)

    suspend fun saveFinished(filePath: String, durationMs: Long): Long {
        val now = System.currentTimeMillis()
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(now))
        return dao.insert(
            RecordingEntity(
                title = "Запись $date",
                filePath = filePath,
                durationMs = durationMs,
                createdAt = now
            )
        )
    }

    suspend fun rename(id: Long, title: String) = dao.rename(id, title)

    suspend fun delete(recording: RecordingEntity) {
        withContext(Dispatchers.IO) { File(recording.filePath).delete() }
        dao.delete(recording) // маркеры, фото и заметки удалятся каскадом
    }
}