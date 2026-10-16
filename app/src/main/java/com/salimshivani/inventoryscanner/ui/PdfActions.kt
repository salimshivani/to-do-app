package com.salimshivani.inventoryscanner.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.salimshivani.inventoryscanner.print.printPdf
import com.salimshivani.inventoryscanner.print.sharePdf
import java.io.File

fun offerPrintOrShare(context: Context, jobTitle: String, file: File) {
    AlertDialog.Builder(context)
        .setTitle(jobTitle)
        .setNegativeButton("Cancel", null)
        .setNeutralButton("Share PDF") { _, _ -> sharePdf(context, file, "Share $jobTitle") }
        .setPositiveButton("Print") { _, _ -> printPdf(context, file, jobTitle) }
        .show()
}
