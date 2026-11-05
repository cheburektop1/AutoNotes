package com.example.autonotes.export

import com.example.autonotes.ui.formatDuration
import com.example.autonotes.ui.label
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MarkdownExporter {

    fun build(report: Report): String = buildString {
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))

        appendLine("# ${report.title}")
        appendLine()
        appendLine("*Дата: $date · Длительность: ${formatDuration(report.durationMs)}*")
        appendLine()
        appendLine("## Хронология")
        appendLine()

        if (report.entries.isEmpty()) {
            appendLine("В записи нет отметок.")
        } else {
            report.entries.forEach { entry ->
                val time = formatDuration(entry.timeMs)
                when (entry) {
                    is Report.Entry.Marker -> appendLine("- **$time** — ${entry.type.label()}")
                    is Report.Entry.Note -> appendLine("- **$time** — Заметка: ${entry.text}")
                    is Report.Entry.Photo -> appendLine("- **$time** — Фото доски (изображение есть в PDF-версии отчёта)")
                }
            }
        }
    }
}