package com.salimshivani.inventoryscanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.barcode.generateInternalBarcode
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.BARCODE_SOURCE_GENERATED
import com.salimshivani.inventoryscanner.data.BARCODE_SOURCE_SCANNED
import com.salimshivani.inventoryscanner.data.DIRECTION_INWARD
import com.salimshivani.inventoryscanner.data.Item
import com.salimshivani.inventoryscanner.databinding.ActivityScanBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private lateinit var db: AppDatabase
    private lateinit var direction: String
    private lateinit var cameraExecutor: ExecutorService

    private var scannedBarcode: String? = null
    private var isGeneratedBarcode = false
    private var matchedItemId: Long? = null
    private val batch = mutableListOf<PendingLineItem>()

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else {
                Toast.makeText(this, "Camera permission is needed to scan barcodes.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)
        direction = intent.getStringExtra(EXTRA_DIRECTION) ?: DIRECTION_INWARD
        cameraExecutor = Executors.newSingleThreadExecutor()

        val isInward = direction == DIRECTION_INWARD
        binding.bannerText.text = if (isInward) "INWARD — point camera at barcode" else "OUTWARD — point camera at barcode"
        binding.bannerText.setBackgroundColor(
            ContextCompat.getColor(this, if (isInward) R.color.inward_green else R.color.outward_red)
        )

        binding.btnNoBarcode.setOnClickListener { generateBarcodeForNewItem() }
        binding.btnCancelScan.setOnClickListener { resetScan() }
        binding.btnConfirmExisting.setOnClickListener { confirmTransaction() }
        binding.btnCreateAndConfirm.setOnClickListener { createAndConfirm() }
        binding.btnFinishBatch.setOnClickListener { finishBatch() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.formScroll.visibility == View.VISIBLE -> resetScan()
                    batch.isNotEmpty() -> confirmDiscardBatch()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val scannerOptions = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_CODE_128, Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93, Barcode.FORMAT_CODABAR, Barcode.FORMAT_ITF, Barcode.FORMAT_QR_CODE
                )
                .build()
            val scanner = BarcodeScanning.getClient(scannerOptions)
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { imageProxy -> analyzeFrame(imageProxy, scanner) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not start camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun analyzeFrame(imageProxy: ImageProxy, scanner: com.google.mlkit.vision.barcode.BarcodeScanner) {
        if (scannedBarcode != null) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull()?.rawValue
                if (value != null && scannedBarcode == null) {
                    onBarcodeScanned(value)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun onBarcodeScanned(value: String) {
        scannedBarcode = value
        isGeneratedBarcode = false
        runOnUiThread {
            binding.textBarcodeLabel.text = "Barcode: $value"
            showForm()
        }
        lifecycleScope.launch {
            val item = db.itemDao().getByBarcode(value)
            runOnUiThread { showLookupResult(item) }
        }
    }

    private fun generateBarcodeForNewItem() {
        lifecycleScope.launch {
            val generated = generateInternalBarcode(db.itemDao())
            scannedBarcode = generated
            isGeneratedBarcode = true
            runOnUiThread {
                binding.textBarcodeLabel.text = "Generated barcode: $generated"
                showForm()
                showLookupResult(null)
            }
        }
    }

    private fun showForm() {
        binding.cameraContainer.visibility = View.GONE
        binding.formScroll.visibility = View.VISIBLE
    }

    private fun showLookupResult(item: Item?) {
        matchedItemId = item?.id
        if (item != null) {
            binding.groupExistingItem.visibility = View.VISIBLE
            binding.groupNewItem.visibility = View.GONE
            binding.textItemName.text = item.name
            binding.textItemHsn.visibility = if (item.hsnCode != null) View.VISIBLE else View.GONE
            binding.textItemHsn.text = "HSN: ${item.hsnCode}"
        } else {
            binding.groupExistingItem.visibility = View.GONE
            binding.groupNewItem.visibility = View.VISIBLE
            binding.textNewItemHint.text = if (isGeneratedBarcode) {
                "New item with a generated barcode. Fill in its details:"
            } else {
                "No item found for this barcode. Create it:"
            }
        }
    }

    private fun resetScan() {
        scannedBarcode = null
        isGeneratedBarcode = false
        matchedItemId = null
        binding.inputQuantityExisting.setText("1")
        binding.inputNoteExisting.setText("")
        binding.inputNewName.setText("")
        binding.inputNewHsn.setText("")
        binding.inputNewUnit.setText("")
        binding.inputQuantityNew.setText("1")
        binding.formScroll.visibility = View.GONE
        binding.cameraContainer.visibility = View.VISIBLE
    }

    private fun addToBatch(itemId: Long, itemName: String, quantity: Double, note: String?) {
        batch.add(PendingLineItem(itemId, itemName, quantity, note))
        updateBatchSummary()
    }

    private fun updateBatchSummary() {
        if (batch.isEmpty()) {
            binding.batchSummaryBar.visibility = View.GONE
        } else {
            binding.batchSummaryBar.visibility = View.VISIBLE
            binding.textBatchCount.text = "${batch.size} item(s) scanned"
        }
    }

    private fun confirmTransaction() {
        val itemId = matchedItemId ?: return
        val qty = binding.inputQuantityExisting.text.toString().toDoubleOrNull()
        if (qty == null || qty <= 0) {
            Toast.makeText(this, "Enter a quantity greater than zero.", Toast.LENGTH_SHORT).show()
            return
        }
        val note = binding.inputNoteExisting.text.toString().trim().ifEmpty { null }
        val name = binding.textItemName.text.toString()
        addToBatch(itemId, name, qty, note)
        Toast.makeText(this, "$qty unit(s) added.", Toast.LENGTH_SHORT).show()
        resetScan()
    }

    private fun createAndConfirm() {
        val barcode = scannedBarcode ?: return
        val name = binding.inputNewName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a name for this new item.", Toast.LENGTH_SHORT).show()
            return
        }
        val qty = binding.inputQuantityNew.text.toString().toDoubleOrNull()
        if (qty == null || qty <= 0) {
            Toast.makeText(this, "Enter a quantity greater than zero.", Toast.LENGTH_SHORT).show()
            return
        }
        val hsn = binding.inputNewHsn.text.toString().trim().ifEmpty { null }
        val unit = binding.inputNewUnit.text.toString().trim().ifEmpty { null }
        val wasGenerated = isGeneratedBarcode
        lifecycleScope.launch {
            val itemId = db.itemDao().insert(
                Item(
                    barcode = barcode,
                    name = name,
                    hsnCode = hsn,
                    unit = unit,
                    createdAt = Instant.now().toString(),
                    barcodeSource = if (wasGenerated) BARCODE_SOURCE_GENERATED else BARCODE_SOURCE_SCANNED
                )
            )
            runOnUiThread {
                addToBatch(itemId, name, qty, null)
                if (wasGenerated) {
                    AlertDialog.Builder(this@ScanActivity)
                        .setTitle("Item created")
                        .setMessage("$qty unit(s) added for $name.\n\nA barcode was generated for this item — print a sticker for it now?")
                        .setNegativeButton("Later") { _, _ -> resetScan() }
                        .setPositiveButton("Print Label") { _, _ ->
                            startActivity(PrintLabelActivity.intent(this@ScanActivity, itemId))
                            resetScan()
                        }
                        .setOnCancelListener { resetScan() }
                        .show()
                } else {
                    Toast.makeText(this@ScanActivity, "$qty unit(s) added for $name.", Toast.LENGTH_SHORT).show()
                    resetScan()
                }
            }
        }
    }

    private fun finishBatch() {
        if (batch.isEmpty()) return
        startActivity(AccountCaptureActivity.intent(this, direction, batch.toJson()))
        finish()
    }

    private fun confirmDiscardBatch() {
        AlertDialog.Builder(this)
            .setTitle("Discard scanned items?")
            .setMessage("${batch.size} item(s) scanned so far will be lost.")
            .setNegativeButton("Keep scanning", null)
            .setPositiveButton("Discard") { _, _ -> finish() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val EXTRA_DIRECTION = "direction"
    }
}
