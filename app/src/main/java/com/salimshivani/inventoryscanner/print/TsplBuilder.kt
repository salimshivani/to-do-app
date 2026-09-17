package com.salimshivani.inventoryscanner.print

import kotlin.math.max
import kotlin.math.roundToInt

// Builds TSPL (TSC Printer Language) commands for a Code128 item label.
// TSPL is the command set most generic Bluetooth thermal label printers speak
// (as opposed to Zebra's ZPL) — sizes are parameterized in millimeters, which
// is what makes "dynamic sticker size" possible: same template, different numbers.

data class LabelInput(
    val widthMm: Double,
    val heightMm: Double,
    val barcodeValue: String,
    val itemName: String,
    val hsnCode: String?,
    val unit: String?,
    val dpi: Int = 203, // most Bluetooth label printers are 203 DPI
    val gapMm: Double = 2.0 // gap between die-cut labels; use 0 for continuous/black-mark rolls
)

private fun escapeTspl(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

private fun truncate(value: String, max: Int): String =
    if (value.length > max) value.substring(0, max - 1) + "…" else value

fun buildTsplLabel(input: LabelInput): String {
    val dotsPerMm = input.dpi / 25.4
    val widthDots = (input.widthMm * dotsPerMm).roundToInt()
    val heightDots = (input.heightMm * dotsPerMm).roundToInt()
    val margin = max(4, (widthDots * 0.05).roundToInt())
    val usableWidth = widthDots - margin * 2

    val nameFontHeight = 24
    val barcodeHeight = max(30, (heightDots * 0.4).roundToInt())
    val barcodeY = nameFontHeight + 12
    val detailY = barcodeY + barcodeHeight + 20

    // ~11px-wide glyphs at TSPL font "3"; keeps the name from overflowing narrow labels.
    val maxNameChars = max(6, usableWidth / 11)
    val name = escapeTspl(truncate(input.itemName, maxNameChars))

    val detailParts = listOfNotNull(input.hsnCode?.let { "HSN $it" }, input.unit)
    val detailLine = if (detailParts.isNotEmpty()) {
        escapeTspl(truncate(detailParts.joinToString(" · "), maxNameChars))
    } else null

    val lines = mutableListOf(
        "SIZE ${input.widthMm} mm,${input.heightMm} mm",
        "GAP ${input.gapMm} mm,0 mm",
        "DIRECTION 1",
        "CLS",
        "TEXT $margin,8,\"3\",0,1,1,\"$name\"",
        "BARCODE $margin,$barcodeY,\"128\",$barcodeHeight,1,0,2,2,\"${escapeTspl(input.barcodeValue)}\""
    )
    if (detailLine != null) {
        lines.add("TEXT $margin,$detailY,\"2\",0,1,1,\"$detailLine\"")
    }
    lines.add("PRINT 1,1")

    return lines.joinToString("\r\n") + "\r\n"
}
