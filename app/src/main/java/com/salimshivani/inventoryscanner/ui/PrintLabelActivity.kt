package com.salimshivani.inventoryscanner.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.salimshivani.inventoryscanner.barcode.renderCode128Bitmap
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.Item
import com.salimshivani.inventoryscanner.databinding.ActivityPrintLabelBinding
import com.salimshivani.inventoryscanner.print.LabelInput
import com.salimshivani.inventoryscanner.print.PrinterDevice
import com.salimshivani.inventoryscanner.print.buildTsplLabel
import com.salimshivani.inventoryscanner.print.bluetoothConnectPermission
import com.salimshivani.inventoryscanner.print.generateLabelPdf
import com.salimshivani.inventoryscanner.print.hasBluetoothConnectPermission
import com.salimshivani.inventoryscanner.print.listPairedPrinters
import com.salimshivani.inventoryscanner.print.printRawToDevice
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

class PrintLabelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrintLabelBinding
    private lateinit var db: AppDatabase
    private var item: Item? = null

    private val requestBluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showPrinterPicker() else {
                Toast.makeText(this, "Bluetooth permission is needed to find your printer.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrintLabelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Print Label"

        db = AppDatabase.getInstance(this)

        binding.btnTestPrint.setOnClickListener { testPrint() }
        binding.btnPrintBluetooth.setOnClickListener { onPrintBluetoothClicked() }
    }

    override fun onResume() {
        super.onResume()
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        lifecycleScope.launch {
            val loaded = db.itemDao().getById(itemId) ?: return@launch
            item = loaded
            bindItem(loaded)
        }
    }

    private fun bindItem(item: Item) {
        binding.textItemName.text = item.name
        binding.textItemMeta.text = item.barcode + if (item.barcodeSource == "generated") " · generated" else ""
        if (item.labelPrintedAt != null) {
            binding.textLastPrinted.visibility = View.VISIBLE
            binding.textLastPrinted.text = "Last printed ${formatTimestamp(item.labelPrintedAt)}"
        } else {
            binding.textLastPrinted.visibility = View.GONE
        }

        binding.previewName.text = item.name
        binding.previewMeta.text = item.hsnCode?.let { "HSN $it" } ?: ""
        val bitmap = renderCode128Bitmap(item.barcode, 600, 200)
        binding.imageBarcode.setImageBitmap(bitmap)
    }

    private fun parsedDimensions(): Pair<Double, Double>? {
        val width = binding.inputWidth.text.toString().toDoubleOrNull()
        val height = binding.inputHeight.text.toString().toDoubleOrNull()
        return if (width != null && width > 0 && height != null && height > 0) width to height else null
    }

    private fun testPrint() {
        val dims = parsedDimensions()
        if (dims == null) {
            binding.textError.visibility = View.VISIBLE
            binding.textError.text = "Enter a valid width and height."
            return
        }
        binding.textError.visibility = View.GONE
        val (widthMm, heightMm) = dims
        val view = binding.stickerPreview
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        try {
            val uri = generateLabelPdf(this, bitmap, widthMm, heightMm)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Label preview"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not generate the PDF preview: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun onPrintBluetoothClicked() {
        if (parsedDimensions() == null) {
            binding.textError.visibility = View.VISIBLE
            binding.textError.text = "Enter a valid width and height."
            return
        }
        binding.textError.visibility = View.GONE
        val permission = bluetoothConnectPermission()
        if (permission != null && !hasBluetoothConnectPermission(this)) {
            requestBluetoothPermission.launch(permission)
        } else {
            showPrinterPicker()
        }
    }

    private fun showPrinterPicker() {
        lifecycleScope.launch {
            val printers = try {
                listPairedPrinters(this@PrintLabelActivity)
            } catch (e: Exception) {
                Toast.makeText(this@PrintLabelActivity, "Failed to list paired devices: ${e.message}", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (printers.isEmpty()) {
                AlertDialog.Builder(this@PrintLabelActivity)
                    .setTitle("No paired printers")
                    .setMessage("No paired Bluetooth devices found. Pair your label printer in Android Bluetooth settings first.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }
            val names = printers.map { it.name }.toTypedArray()
            AlertDialog.Builder(this@PrintLabelActivity)
                .setTitle("Select a paired printer")
                .setItems(names) { _, which -> printToDevice(printers[which]) }
                .show()
        }
    }

    private fun printToDevice(printer: PrinterDevice) {
        val currentItem = item ?: return
        val dims = parsedDimensions() ?: return
        val (widthMm, heightMm) = dims
        lifecycleScope.launch {
            try {
                val tspl = buildTsplLabel(
                    LabelInput(
                        widthMm = widthMm,
                        heightMm = heightMm,
                        barcodeValue = currentItem.barcode,
                        itemName = currentItem.name,
                        hsnCode = currentItem.hsnCode,
                        unit = currentItem.unit
                    )
                )
                printRawToDevice(this@PrintLabelActivity, printer.address, tspl)
                db.itemDao().markLabelPrinted(currentItem.id, Instant.now().toString())
                val refreshed = db.itemDao().getById(currentItem.id)
                if (refreshed != null) {
                    item = refreshed
                    bindItem(refreshed)
                }
                Toast.makeText(this@PrintLabelActivity, "Label sent to ${printer.name}.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@PrintLabelActivity, "Print failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatTimestamp(value: String): String = try {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date.from(Instant.parse(value)))
    } catch (e: Exception) {
        value
    }

    companion object {
        private const val EXTRA_ITEM_ID = "item_id"
        fun intent(context: Context, itemId: Long): Intent =
            Intent(context, PrintLabelActivity::class.java).putExtra(EXTRA_ITEM_ID, itemId)
    }
}
