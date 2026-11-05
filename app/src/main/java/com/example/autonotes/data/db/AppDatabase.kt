package com.example.autonotes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecordingEntity::class,
        MarkerEntity::class,
        PhotoEntity::class,
        NoteEntity::class,
        TranscriptSegmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun markerDao(): MarkerDao
    abstract fun noteDao(): NoteDao
    abstract fun photoDao(): PhotoDao
}