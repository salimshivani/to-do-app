package com.salimshivani.inventoryscanner.print

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.salimshivani.inventoryscanner.data.Account
import com.salimshivani.inventoryscanner.data.Bill
import com.salimshivani.inventoryscanner.data.DIRECTION_INWARD
import com.salimshivani.inventoryscanner.data.TransactionWithItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val ROW_HEIGHT = 22f

private data class Column(val title: String, val width: Float)

private val COLUMNS = listOf(
    Column("Item", 160f),
    Column("HSN", 55f),
    Column("Qty", 55f),
    Column("Rate", 65f),
    Column("GST%", 50f),
    Column("Tax", 60f),
    Column("Total", 70f)
)

// Renders a bill (purchase/sales) with its line items and GST breakdown to an
// A4 PDF file in the cache, ready for the system print dialog or the share sheet.
fun generateBillPdf(
    context: Context,
    bill: Bill,
    account: Account,
    lines: List<TransactionWithItem>
): File {
    val document = PdfDocument()
    var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, document.pages.size + 1).create())
    var canvas = page.canvas
    var y = MARGIN

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.DKGRAY }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.BLACK }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; isFakeBoldText = true; color = Color.BLACK }
    val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
    val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; isFakeBoldText = true; color = Color.BLACK }

    val isInward = bill.direction == DIRECTION_INWARD
    val title = if (isInward) "PURCHASE BILL" else "SALES INVOICE"
    val partyLabel = if (isInward) "Supplier" else "Customer"

    canvas.drawText(title, MARGIN, y + 16f, titlePaint)
    canvas.drawText(formatDate(bill.billDate), PAGE_WIDTH - MARGIN - 150f, y + 16f, bodyPaint)
    y += 28f
    canvas.drawText("Bill #${bill.id}", MARGIN, y, bodyPaint)
    y += 20f
    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
    y += 18f

    canvas.drawText("$partyLabel: ${account.name}", MARGIN, y, bodyPaint)
    y += 16f
    account.mobileNumber?.let {
        canvas.drawText("Mobile: $it", MARGIN, y, labelPaint)
        y += 16f
    }
    account.address?.let {
        canvas.drawText(it, MARGIN, y, labelPaint)
        y += 16f
    }
    y += 10f

    fun drawHeaderRow() {
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, Paint().apply { color = Color.parseColor("#EFF6FF") })
        var x = MARGIN
        for (col in COLUMNS) {
            canvas.drawText(col.title, x + 4f, y + ROW_HEIGHT - 6f, headerPaint)
            x += col.width
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

    for (line in lines) {
        if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN - 90f) {
            newPage()
        }
        val qtyText = formatQty(line.quantity) + ((line.unit_label ?: line.unit)?.let { " $it" } ?: "")
        val values = listOf(
            line.item_name,
            line.hsn_code ?: "-",
            qtyText,
            formatMoneyValue(line.rate),
            "${formatQty(line.gst_percent)}%",
            formatMoneyValue(line.tax_amount),
            formatMoneyValue(line.line_total)
        )
        var x = MARGIN
        for ((i, col) in COLUMNS.withIndex()) {
            canvas.drawText(truncate(values[i], col.width, bodyPaint), x + 4f, y + ROW_HEIGHT - 6f, bodyPaint)
            x += col.width
        }
        y += ROW_HEIGHT
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
    }

    y += 14f
    if (y + 80f > PAGE_HEIGHT - MARGIN) newPage()

    val totalsLabelX = PAGE_WIDTH - MARGIN - 180f
    val totalsValueX = PAGE_WIDTH - MARGIN - 60f
    canvas.drawText("Subtotal:", totalsLabelX, y, bodyPaint)
    canvas.drawText(formatMoneyValue(bill.subtotal), totalsValueX, y, bodyPaint)
    y += 18f
    canvas.drawText("Tax:", totalsLabelX, y, bodyPaint)
    canvas.drawText(formatMoneyValue(bill.taxAmount), totalsValueX, y, bodyPaint)
    y += 14f
    canvas.drawLine(totalsLabelX, y, PAGE_WIDTH - MARGIN, y, linePaint)
    y += 20f
    canvas.drawText("Total:", totalsLabelX, y, totalPaint)
    canvas.drawText(formatMoneyValue(bill.totalAmount), totalsValueX, y, totalPaint)

    document.finishPage(page)

    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "bill-${bill.id}-${Instant.now().toString().replace(Regex("[:.]"), "-")}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}

private fun formatDate(iso: String): String = try {
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date.from(Instant.parse(iso)))
} catch (e: Exception) {
    iso
}

private fun formatQty(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private fun formatMoneyValue(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)

private fun truncate(text: String, columnWidth: Float, paint: Paint): String {
    if (paint.measureText(text) <= columnWidth - 6f) return text
    var result = text
    while (result.isNotEmpty() && paint.measureText("$result…") > columnWidth - 6f) {
        result = result.dropLast(1)
    }
    return if (result.isEmpty()) text else "$result…"
}
