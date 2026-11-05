package com.example.autonotes.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {
    @Insert
    suspend fun insert(marker: MarkerEntity): Long

    @Query("SELECT * FROM markers WHERE recordingId = :recordingId ORDER BY timeMs")
    fun observe(recordingId: Long): Flow<List<MarkerEntity>>

    @Query("SELECT * FROM markers WHERE recordingId = :recordingId ORDER BY timeMs")
    suspend fun getAll(recordingId: Long): List<MarkerEntity>
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("SELECT * FROM notes WHERE recordingId = :recordingId ORDER BY timeMs")
    fun observe(recordingId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE recordingId = :recordingId ORDER BY timeMs")
    suspend fun getAll(recordingId: Long): List<NoteEntity>
}

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos WHERE recordingId = :recordingId ORDER BY timeMs")
    fun observe(recordingId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE recordingId = :recordingId ORDER BY timeMs")
    suspend fun getAll(recordingId: Long): List<PhotoEntity>
}

