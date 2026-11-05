package com.example.autonotes.export

import android.content.Context
import com.example.autonotes.data.RecordingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportFormat { PDF, MARKDOWN }

/** Готовый файл отчёта и данные для окна «Поделиться». */
data class ShareRequest(val file: File, val mimeType: String, val subject: String)

@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository
) {
    suspend fun export(recordingId: Long, format: ExportFormat): ShareRequest? =
        withContext(Dispatchers.IO) {
            val report = repository.buildReport(recordingId) ?: return@withContext null

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // чистим старые отчёты

            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(report.createdAt))
            val baseName = "AutoNotes_${recordingId}_$stamp"

            when (format) {
                ExportFormat.PDF -> {
                    val file = File(dir, "$baseName.pdf")
                    PdfExporter.export(report, file)
                    ShareRequest(file, "application/pdf", report.title)
                }
                ExportFormat.MARKDOWN -> {
                    val file = File(dir, "$baseName.md")
                    file.writeText(MarkdownExporter.build(report))
                    ShareRequest(file, "text/plain", report.title)
                }
            }
        }
}