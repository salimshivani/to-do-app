package com.salimshivani.inventoryscanner.barcode

import com.salimshivani.inventoryscanner.data.ItemDao

// Internally generated barcodes use this prefix so they're easy to tell apart
// from real, scanned manufacturer barcodes at a glance (in lists, reports, exports).
const val GENERATED_BARCODE_PREFIX = "INT-"

// Sequential rather than random so printed labels stay short and easy to read/type as a fallback.
suspend fun generateInternalBarcode(itemDao: ItemDao): String {
    val maxSeq = itemDao.maxGeneratedBarcodeSequence(
        prefixLength = GENERATED_BARCODE_PREFIX.length + 1,
        likePattern = "$GENERATED_BARCODE_PREFIX%"
    ) ?: 0
    val next = maxSeq + 1
    return GENERATED_BARCODE_PREFIX + next.toString().padStart(6, '0')
}
