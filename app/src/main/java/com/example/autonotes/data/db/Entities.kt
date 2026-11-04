package com.example.autonotes.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long
)

enum class MarkerType { IMPORTANT, DEFINITION, EXAMPLE, HOMEWORK }

@Entity(
    tableName = "markers",
    foreignKeys = [ForeignKey(RecordingEntity::class, ["id"], ["recordingId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recordingId")]
)
data class MarkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val type: MarkerType,
    val timeMs: Long
)

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(RecordingEntity::class, ["id"], ["recordingId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recordingId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val filePath: String,
    val timeMs: Long
)

@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(RecordingEntity::class, ["id"], ["recordingId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recordingId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val text: String,
    val timeMs: Long
)

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [ForeignKey(RecordingEntity::class, ["id"], ["recordingId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recordingId")]
)
data class TranscriptSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String
)