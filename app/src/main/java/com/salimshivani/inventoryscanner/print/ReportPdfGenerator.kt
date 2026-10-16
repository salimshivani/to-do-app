package com.salimshivani.inventoryscanner.print

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 36f
private const val ROW_HEIGHT = 22f

// Renders any of the app's report tables (title, columns, string rows) to an
// A4 PDF file in the cache, ready for the system print dialog or the share sheet.
fun generateReportPdf(
    context: Context,
    title: String,
    subtitle: String?,
    columns: List<String>,
    rows: List<List<String>>
): File {
    val document = PdfDocument()
    val usableWidth = PAGE_WIDTH - 2 * MARGIN
    val colWidth = usableWidth / columns.size

    var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
    var canvas = page.canvas
    var y = MARGIN

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f; isFakeBoldText = true; color = Color.BLACK }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.DKGRAY }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; isFakeBoldText = true; color = Color.BLACK }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.BLACK }
    val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

    canvas.drawText(title, MARGIN, y + 14f, titlePaint)
    y += 20f
    val generatedAt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date.from(Instant.now()))
    canvas.drawText(subtitle?.let { "$it  ·  Generated $generatedAt" } ?: "Generated $generatedAt", MARGIN, y, subtitlePaint)
    y += 18f

    fun drawHeaderRow() {
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, Paint().apply { color = Color.parseColor("#EFF6FF") })
        var x = MARGIN
        for (col in columns) {
            canvas.drawText(truncate(col, colWidth, headerPaint), x + 4f, y + ROW_HEIGHT - 6f, headerPaint)
            x += colWidth
        }
        y += ROW_HEIGHT
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
    }

    fun newPage() {
        document.finishPage(page)
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
        canvas = page.canvas
        y = MARGIN
        drawHeaderRow()
    }

    drawHeaderRow()

    for (row in rows) {
        if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
            newPage()
        }
        var x = MARGIN
        for (cell in row) {
            canvas.drawText(truncate(cell, colWidth, bodyPaint), x + 4f, y + ROW_HEIGHT - 6f, bodyPaint)
            x += colWidth
        }
        y += ROW_HEIGHT
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
    }

    document.finishPage(page)

    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safeTitle = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    val file = File(dir, "$safeTitle-${Instant.now().toString().replace(Regex("[:.]"), "-")}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}

private fun truncate(text: String, columnWidth: Float, paint: Paint): String {
    if (paint.measureText(text) <= columnWidth - 6f) return text
    var result = text
    while (result.isNotEmpty() && paint.measureText("$result…") > columnWidth - 6f) {
        result = result.dropLast(1)
    }
    return if (result.isEmpty()) text else "$result…"
}
