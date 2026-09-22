package com.salimshivani.inventoryscanner.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.csv.exportBackupJson
import com.salimshivani.inventoryscanner.csv.exportItemsCsv
import com.salimshivani.inventoryscanner.csv.exportTransactionsCsv
import com.salimshivani.inventoryscanner.csv.importBackupJson
import com.salimshivani.inventoryscanner.csv.importItemsCsv
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.databinding.ActivityDataBinding
import kotlinx.coroutines.launch

class DataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDataBinding
    private lateinit var db: AppDatabase

    private val pickItemsCsv = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runImportItems(uri)
    }
    private val pickBackupJson = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestoreBackup(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Import / Export"

        db = AppDatabase.getInstance(this)

        binding.btnExportItems.title.text = "Export Items (CSV)"
        binding.btnExportItems.subtitle.text = "Item master data: barcode, name, HSN, unit"
        binding.btnExportItems.root.setOnClickListener {
            lifecycleScope.launch {
                val uri = exportItemsCsv(this@DataActivity, db.itemDao())
                shareFile(uri, "text/csv")
            }
        }

        binding.btnExportTransactions.title.text = "Export Transactions (CSV)"
        binding.btnExportTransactions.subtitle.text = "Full inward/outward log for spreadsheets"
        binding.btnExportTransactions.root.setOnClickListener {
            lifecycleScope.launch {
                val uri = exportTransactionsCsv(this@DataActivity, db.transactionDao())
                shareFile(uri, "text/csv")
            }
        }

        binding.btnExportBackup.title.text = "Export Full Backup (JSON)"
        binding.btnExportBackup.subtitle.text = "Everything, for restore on this or another device"
        binding.btnExportBackup.dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_purple))
        binding.btnExportBackup.root.setOnClickListener {
            lifecycleScope.launch {
                val uri = exportBackupJson(this@DataActivity, db.itemDao(), db.transactionDao(), db.accountDao())
                shareFile(uri, "application/json")
            }
        }

        binding.btnImportItems.title.text = "Import Items (CSV)"
        binding.btnImportItems.subtitle.text = "Columns: barcode, name, hsn_code, unit — updates existing barcodes"
        binding.btnImportItems.dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.inward_green))
        binding.btnImportItems.root.setOnClickListener { pickItemsCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) }

        binding.btnRestoreBackup.title.text = "Restore Full Backup (JSON)"
        binding.btnRestoreBackup.subtitle.text = "Replaces ALL current items and transactions"
        binding.btnRestoreBackup.dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.outward_red))
        binding.btnRestoreBackup.root.setOnClickListener { pickBackupJson.launch(arrayOf("application/json", "*/*")) }
    }

    private fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share export"))
    }

    private fun runImportItems(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = importItemsCsv(this@DataActivity, db.itemDao(), uri)
                Toast.makeText(
                    this@DataActivity,
                    "Created ${result.created}, updated ${result.updated}, skipped ${result.skipped}.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@DataActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmRestoreBackup(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Replace all data?")
            .setMessage("This overwrites everything currently on this device.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore") { _, _ ->
                lifecycleScope.launch {
                    try {
                        importBackupJson(this@DataActivity, db, uri)
                        Toast.makeText(this@DataActivity, "Restore complete. All data has been replaced.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@DataActivity, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }
}
