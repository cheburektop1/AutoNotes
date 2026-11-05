package com.example.autonotes.data

import com.example.autonotes.data.db.MarkerDao
import com.example.autonotes.data.db.MarkerEntity
import com.example.autonotes.data.db.NoteDao
import com.example.autonotes.data.db.NoteEntity
import com.example.autonotes.data.db.PhotoDao
import com.example.autonotes.data.db.PhotoEntity
import com.example.autonotes.data.db.RecordingDao
import com.example.autonotes.data.db.RecordingEntity
import com.example.autonotes.recording.RecordingSession.PendingEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.example.autonotes.export.Report

@Singleton
class RecordingRepository @Inject constructor(
    private val recordingDao: RecordingDao,
    private val markerDao: MarkerDao,
    private val noteDao: NoteDao,
    private val photoDao: PhotoDao
) {
    suspend fun buildReport(recordingId: Long): Report? {
        val rec = recordingDao.getById(recordingId) ?: return null
        val entries: List<Report.Entry> =
            markerDao.getAll(recordingId).map { Report.Entry.Marker(it.timeMs, it.type) } +
                    noteDao.getAll(recordingId).map { Report.Entry.Note(it.timeMs, it.text) } +
                    photoDao.getAll(recordingId).map { Report.Entry.Photo(it.timeMs, it.filePath) }
        return Report(rec.title, rec.createdAt, rec.durationMs, entries.sortedBy { it.timeMs })
    }
    fun observeRecordings(): Flow<List<RecordingEntity>> = recordingDao.observeAll()

    suspend fun getRecording(id: Long): RecordingEntity? = recordingDao.getById(id)

    fun observeMarkers(recordingId: Long) = markerDao.observe(recordingId)
    fun observeNotes(recordingId: Long) = noteDao.observe(recordingId)
    fun observePhotos(recordingId: Long) = photoDao.observe(recordingId)

    suspend fun getMarkers(recordingId: Long) = markerDao.getAll(recordingId)
    suspend fun getNotes(recordingId: Long) = noteDao.getAll(recordingId)
    suspend fun getPhotos(recordingId: Long) = photoDao.getAll(recordingId)

    suspend fun saveFinished(
        filePath: String,
        durationMs: Long,
        events: List<PendingEvent>
    ): Long {
        val now = System.currentTimeMillis()
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(now))
        val id = recordingDao.insert(
            RecordingEntity(
                title = "Запись $date",
                filePath = filePath,
                durationMs = durationMs,
                createdAt = now
            )
        )
        events.forEach { event ->
            when (event) {
                is PendingEvent.Marker ->
                    markerDao.insert(MarkerEntity(recordingId = id, type = event.type, timeMs = event.timeMs))
                is PendingEvent.Note ->
                    noteDao.insert(NoteEntity(recordingId = id, text = event.text, timeMs = event.timeMs))
                is PendingEvent.Photo ->
                    photoDao.insert(PhotoEntity(recordingId = id, filePath = event.filePath, timeMs = event.timeMs))
            }
        }
        return id
    }

    suspend fun rename(id: Long, title: String) = recordingDao.rename(id, title)

    suspend fun delete(recording: RecordingEntity) {
        val photos = photoDao.getAll(recording.id)
        withContext(Dispatchers.IO) {
            File(recording.filePath).delete()
            photos.forEach { File(it.filePath).delete() }
        }
        recordingDao.delete(recording) // маркеры, фото и заметки удалятся каскадом
    }
}