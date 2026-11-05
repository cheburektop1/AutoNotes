package com.example.autonotes.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.example.autonotes.ui.formatDuration
import com.example.autonotes.ui.label
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/** Собирает PDF встроенным в Android PdfDocument, без сторонних библиотек. */
object PdfExporter {

    private const val PAGE_W = 595 // A4 в пунктах
    private const val PAGE_H = 842
    private const val MARGIN = 40
    private const val CONTENT_W = PAGE_W - 2 * MARGIN
    private const val PHOTO_MAX_H = 360

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = Color.rgb(15, 23, 42)) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    fun export(report: Report, outFile: File) {
        val titlePaint = textPaint(22f, bold = true)
        val headingPaint = textPaint(15f, bold = true)
        val metaPaint = textPaint(11f, color = Color.rgb(71, 85, 105))
        val bodyPaint = textPaint(12f)

        val document = PdfDocument()
        try {
            val writer = PageWriter(document)
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(Date(report.createdAt))

            writer.paragraph(report.title, titlePaint, spaceAfter = 6)
            writer.paragraph(
                "Дата: $date · Длительность: ${formatDuration(report.durationMs)}",
                metaPaint, spaceAfter = 18
            )
            writer.paragraph("Хронология", headingPaint, spaceAfter = 10)

            if (report.entries.isEmpty()) {
                writer.paragraph("В записи нет отметок.", bodyPaint)
            }
            report.entries.forEach { entry ->
                val time = formatDuration(entry.timeMs)
                when (entry) {
                    is Report.Entry.Marker ->
                        writer.paragraph(line(time, entry.type.label()), bodyPaint)
                    is Report.Entry.Note ->
                        writer.paragraph(line(time, "Заметка: ${entry.text}"), bodyPaint)
                    is Report.Entry.Photo -> {
                        writer.paragraph(line(time, "Фото доски"), bodyPaint, spaceAfter = 4)
                        writer.photo(entry.filePath, metaPaint)
                    }
                }
            }

            writer.finish()
            FileOutputStream(outFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private fun line(time: String, text: String): CharSequence = buildSpannedString {
        bold { append(time) }
        append(" — $text")
    }

    private class PageWriter(private val document: PdfDocument) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGIN
        private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        private fun newPage() {
            page?.let { document.finishPage(it) }
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create()
            val newPage = document.startPage(info)
            page = newPage
            canvas = newPage.canvas
            y = MARGIN
        }

        private fun ensureSpace(height: Int) {
            if (page == null || y + height > PAGE_H - MARGIN) newPage()
        }

        fun paragraph(text: CharSequence, paint: TextPaint, spaceAfter: Int = 8) {
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, CONTENT_W)
                .build()
            ensureSpace(min(layout.height, PAGE_H - 2 * MARGIN))
            val c = canvas ?: return
            c.save()
            c.translate(MARGIN.toFloat(), y.toFloat())
            layout.draw(c)
            c.restore()
            y += layout.height + spaceAfter
        }

        fun photo(path: String, fallbackPaint: TextPaint) {
            val bitmap = loadBitmap(path)
            if (bitmap == null) {
                paragraph("(фото недоступно)", fallbackPaint)
                return
            }
            val scale = min(
                CONTENT_W.toFloat() / bitmap.width,
                PHOTO_MAX_H.toFloat() / bitmap.height
            )
            val w = bitmap.width * scale
            val h = bitmap.height * scale
            ensureSpace(h.toInt() + 14)
            val c = canvas ?: return
            c.drawBitmap(
                bitmap, null,
                RectF(MARGIN.toFloat(), y.toFloat(), MARGIN + w, y + h),
                bitmapPaint
            )
            y += h.toInt() + 14
            bitmap.recycle()
        }

        fun finish() {
            page?.let { document.finishPage(it) }
            page = null
            canvas = null
        }

        private fun loadBitmap(path: String): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // Уменьшаем при чтении, чтобы большие фото не съедали память
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= CONTENT_W * 2 &&
                bounds.outHeight / (sample * 2) >= PHOTO_MAX_H * 2
            ) sample *= 2

            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = BitmapFactory.decodeFile(path, options) ?: return null

            // Камера хранит поворот в EXIF, а декодер его не применяет
            val orientation = runCatching {
                ExifInterface(path).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) {
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height,
                    Matrix().apply { postRotate(degrees) }, true
                )
                if (rotated != bitmap) bitmap.recycle()
                bitmap = rotated
            }
            return bitmap
        }
    }
}