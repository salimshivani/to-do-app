package com.salimshivani.inventoryscanner.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.Code128Writer

// Renders a Code128 barcode as a Bitmap for on-screen preview, print snapshots, and PDFs.
fun renderCode128Bitmap(value: String, widthPx: Int, heightPx: Int): Bitmap {
    val matrix: BitMatrix = Code128Writer().encode(value, BarcodeFormat.CODE_128, widthPx, heightPx)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    for (x in 0 until widthPx) {
        for (y in 0 until heightPx) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}
