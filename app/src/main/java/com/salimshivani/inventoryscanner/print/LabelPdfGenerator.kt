package com.salimshivani.inventoryscanner.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import kotlin.math.roundToInt

private const val POINTS_PER_MM = 72.0 / 25.4

// Renders the label preview bitmap onto a PDF page sized to the exact sticker
// dimensions (1mm = 72/25.4pt) — so viewing it at 100% shows true size.
fun generateLabelPdf(
    context: Context,
    previewBitmap: Bitmap,
    widthMm: Double,
    heightMm: Double
): Uri {
    val pageWidth = (widthMm * POINTS_PER_MM).roundToInt().coerceAtLeast(1)
    val pageHeight = (heightMm * POINTS_PER_MM).roundToInt().coerceAtLeast(1)

    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = document.startPage(pageInfo)

    val scale = minOf(
        pageWidth.toFloat() / previewBitmap.width,
        pageHeight.toFloat() / previewBitmap.height
    )
    val drawWidth = previewBitmap.width * scale
    val drawHeight = previewBitmap.height * scale
    val left = (pageWidth - drawWidth) / 2f
    val top = (pageHeight - drawHeight) / 2f

    val destRect = android.graphics.RectF(left, top, left + drawWidth, top + drawHeight)
    page.canvas.drawBitmap(previewBitmap, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG))
    document.finishPage(page)

    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "label-preview-${Instant.now().toString().replace(Regex("[:.]"), "-")}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
