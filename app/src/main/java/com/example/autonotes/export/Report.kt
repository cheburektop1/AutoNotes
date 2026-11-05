package com.example.autonotes.export

import com.example.autonotes.data.db.MarkerType

data class Report(
    val title: String,
    val createdAt: Long,
    val durationMs: Long,
    val entries: List<Entry>
) {
    sealed interface Entry {
        val timeMs: Long
        data class Marker(override val timeMs: Long, val type: MarkerType) : Entry
        data class Note(override val timeMs: Long, val text: String) : Entry
        data class Photo(override val timeMs: Long, val filePath: String) : Entry
    }
}