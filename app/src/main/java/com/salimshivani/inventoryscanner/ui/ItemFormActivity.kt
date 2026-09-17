package com.salimshivani.inventoryscanner.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.barcode.generateInternalBarcode
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.BARCODE_SOURCE_GENERATED
import com.salimshivani.inventoryscanner.data.BARCODE_SOURCE_SCANNED
import com.salimshivani.inventoryscanner.data.Item
import com.salimshivani.inventoryscanner.data.Transaction
import com.salimshivani.inventoryscanner.databinding.ActivityItemFormBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

class ItemFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityItemFormBinding
    private lateinit var db: AppDatabase
    private var itemId: Long = -1L
    private var isGeneratedBarcode = false
    private var loadedItem: Item? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)
        itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        val isEdit = itemId != -1L
        title = if (isEdit) "Edit Item" else "Add Item"

        if (isEdit) {
            binding.inputBarcode.visibility = View.GONE
            binding.btnGenerateBarcode.visibility = View.GONE
            binding.textBarcodeReadonly.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.VISIBLE
            binding.textTransactionsTitle.visibility = View.VISIBLE
            binding.btnPrintLabel.visibility = View.VISIBLE
            binding.btnPrintLabel.setOnClickListener {
                startActivity(PrintLabelActivity.intent(this, itemId))
            }
        } else {
            binding.btnGenerateBarcode.setOnClickListener { generateBarcode() }
        }

        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    override fun onResume() {
        super.onResume()
        if (itemId != -1L) loadItem()
    }

    private fun loadItem() {
        lifecycleScope.launch {
            val item = db.itemDao().getById(itemId) ?: return@launch
            loadedItem = item
            binding.textBarcodeReadonly.text = item.barcode
            binding.inputName.setText(item.name)
            binding.inputHsn.setText(item.hsnCode ?: "")
            binding.inputUnit.setText(item.unit ?: "")

            val transactions = db.transactionDao().listForItem(itemId)
            renderTransactions(transactions)
        }
    }

    private fun renderTransactions(transactions: List<Transaction>) {
        binding.transactionsContainer.removeAllViews()
        if (transactions.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No transactions yet."
                setTextColor(ContextCompat.getColor(this@ItemFormActivity, R.color.text_faint))
            }
            binding.transactionsContainer.addView(empty)
            return
        }
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        for (t in transactions) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 20, 0, 20)
            }
            val direction = TextView(this).apply {
                text = if (t.direction == "inward") "IN" else "OUT"
                setTextColor(
                    ContextCompat.getColor(
                        this@ItemFormActivity,
                        if (t.direction == "inward") R.color.inward_green else R.color.outward_red
                    )
                )
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                width = 120
            }
            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val qtyText = TextView(this).apply {
                text = "${t.quantity} unit(s)"
                setTextColor(ContextCompat.getColor(this@ItemFormActivity, R.color.text_primary))
                textSize = 15f
            }
            val dateText = TextView(this).apply {
                text = try {
                    formatter.format(Date.from(Instant.parse(t.timestamp)))
                } catch (e: Exception) {
                    t.timestamp
                }
                setTextColor(ContextCompat.getColor(this@ItemFormActivity, R.color.text_muted))
                textSize = 12f
            }
            details.addView(qtyText)
            details.addView(dateText)
            t.note?.let {
                details.addView(TextView(this).apply {
                    text = it
                    setTextColor(ContextCompat.getColor(this@ItemFormActivity, R.color.text_muted))
                    textSize = 12f
                })
            }
            row.addView(direction)
            row.addView(details)
            binding.transactionsContainer.addView(row)
        }
    }

    private fun generateBarcode() {
        lifecycleScope.launch {
            val generated = generateInternalBarcode(db.itemDao())
            binding.inputBarcode.setText(generated)
            isGeneratedBarcode = true
        }
    }

    private fun save() {
        val name = binding.inputName.text.toString().trim()
        val hsn = binding.inputHsn.text.toString().trim().ifEmpty { null }
        val unit = binding.inputUnit.text.toString().trim().ifEmpty { null }

        if (name.isEmpty()) {
            Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
            return
        }

        if (itemId != -1L) {
            val existing = loadedItem ?: return
            lifecycleScope.launch {
                db.itemDao().update(existing.copy(name = name, hsnCode = hsn, unit = unit))
                finish()
            }
        } else {
            val barcode = binding.inputBarcode.text.toString().trim()
            if (barcode.isEmpty()) {
                Toast.makeText(this, "Barcode and name are required.", Toast.LENGTH_SHORT).show()
                return
            }
            lifecycleScope.launch {
                val existing = db.itemDao().getByBarcode(barcode)
                if (existing != null) {
                    Toast.makeText(this@ItemFormActivity, "An item with this barcode already exists.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val newId = db.itemDao().insert(
                    Item(
                        barcode = barcode,
                        name = name,
                        hsnCode = hsn,
                        unit = unit,
                        createdAt = Instant.now().toString(),
                        barcodeSource = if (isGeneratedBarcode) BARCODE_SOURCE_GENERATED else BARCODE_SOURCE_SCANNED
                    )
                )
                if (isGeneratedBarcode) {
                    AlertDialog.Builder(this@ItemFormActivity)
                        .setTitle("Item created")
                        .setMessage("A barcode was generated for this item — print a sticker for it now?")
                        .setNegativeButton("Later") { _, _ -> finish() }
                        .setPositiveButton("Print Label") { _, _ ->
                            startActivity(PrintLabelActivity.intent(this@ItemFormActivity, newId))
                            finish()
                        }
                        .setOnCancelListener { finish() }
                        .show()
                } else {
                    finish()
                }
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete item")
            .setMessage("This also deletes its transaction history. Continue?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                val item = loadedItem ?: return@setPositiveButton
                lifecycleScope.launch {
                    db.itemDao().delete(item)
                    finish()
                }
            }
            .show()
    }

    companion object {
        private const val EXTRA_ITEM_ID = "item_id"

        fun newIntent(context: Context): Intent = Intent(context, ItemFormActivity::class.java)

        fun editIntent(context: Context, itemId: Long): Intent =
            Intent(context, ItemFormActivity::class.java).putExtra(EXTRA_ITEM_ID, itemId)
    }
}
