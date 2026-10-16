package com.salimshivani.inventoryscanner.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.data.Account
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.Bill
import com.salimshivani.inventoryscanner.data.DIRECTION_INWARD
import com.salimshivani.inventoryscanner.data.Transaction
import com.salimshivani.inventoryscanner.databinding.ActivityAccountCaptureBinding
import com.salimshivani.inventoryscanner.print.generateBillPdf
import com.salimshivani.inventoryscanner.print.printPdf
import com.salimshivani.inventoryscanner.print.sharePdf
import kotlinx.coroutines.launch
import java.time.Instant

class AccountCaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountCaptureBinding
    private lateinit var db: AppDatabase
    private lateinit var direction: String
    private lateinit var batch: List<PendingLineItem>
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)
        direction = intent.getStringExtra(EXTRA_DIRECTION) ?: DIRECTION_INWARD
        batch = parsePendingLineItems(intent.getStringExtra(EXTRA_BATCH_JSON).orEmpty())

        val isInward = direction == DIRECTION_INWARD
        val partyLabel = if (isInward) "Supplier" else "Customer"
        title = "$partyLabel Details"
        binding.bannerText.text = if (isInward) "INWARD — supplier & bill details" else "OUTWARD — customer & bill details"
        binding.bannerText.setBackgroundColor(
            ContextCompat.getColor(this, if (isInward) R.color.inward_green else R.color.outward_red)
        )
        binding.inputName.hint = "$partyLabel name"
        binding.inputMobile.hint = "$partyLabel mobile number"
        binding.textPartySectionTitle.text = partyLabel

        renderBatch()
        val subtotal = batch.sumOf { it.subtotal }
        val taxTotal = batch.sumOf { it.taxAmount }
        binding.textSubtotal.text = "Subtotal: ${formatMoney(subtotal)}"
        binding.textTax.text = "Tax: ${formatMoney(taxTotal)}"
        binding.inputBillTotal.setText(formatMoney(subtotal + taxTotal))

        binding.inputMobile.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) lookupAccountByMobile()
        }
        binding.btnComplete.setOnClickListener { complete() }
    }

    private fun renderBatch() {
        binding.batchContainer.removeAllViews()
        for (line in batch) {
            val row = TextView(this).apply {
                val qtyText = formatQuantity(line.quantity) + (line.unitLabel?.let { " $it" } ?: "")
                text = "${line.itemName} — $qtyText × ${formatMoney(line.unitPrice)}" +
                    (if (line.gstPercent > 0) " + ${formatQuantity(line.gstPercent)}% GST" else "") +
                    " = ${formatMoney(line.lineTotal)}" +
                    if (line.note != null) " (${line.note})" else ""
                setTextColor(ContextCompat.getColor(this@AccountCaptureActivity, R.color.text_primary))
                textSize = 14f
                setPadding(0, 8, 0, 8)
            }
            binding.batchContainer.addView(row)
        }
    }

    private fun lookupAccountByMobile() {
        val mobile = binding.inputMobile.text.toString().trim()
        if (mobile.isEmpty()) {
            binding.textLookupStatus.visibility = View.GONE
            return
        }
        lifecycleScope.launch {
            val account = db.accountDao().getByMobile(mobile)
            binding.textLookupStatus.visibility = View.VISIBLE
            if (account != null) {
                binding.inputName.setText(account.name)
                binding.inputAddress.setText(account.address ?: "")
                binding.textLookupStatus.text = "Existing account found and loaded."
                binding.textLookupStatus.setTextColor(ContextCompat.getColor(this@AccountCaptureActivity, R.color.inward_green))
            } else {
                binding.textLookupStatus.text = "No account found for this number — a new one will be created."
                binding.textLookupStatus.setTextColor(ContextCompat.getColor(this@AccountCaptureActivity, R.color.text_muted))
            }
        }
    }

    private fun complete() {
        if (saving) return
        val name = binding.inputName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a name.", Toast.LENGTH_SHORT).show()
            return
        }
        val billTotal = binding.inputBillTotal.text.toString().trim().toDoubleOrNull()
        if (billTotal == null || billTotal < 0) {
            Toast.makeText(this, "Enter a valid bill total.", Toast.LENGTH_SHORT).show()
            return
        }
        val mobile = binding.inputMobile.text.toString().trim().ifEmpty { null }
        val address = binding.inputAddress.text.toString().trim().ifEmpty { null }
        val subtotal = batch.sumOf { it.subtotal }
        val taxTotal = batch.sumOf { it.taxAmount }

        saving = true
        lifecycleScope.launch {
            try {
                val accountId = upsertAccount(mobile, name, address)
                val timestamp = Instant.now().toString()
                val billId = db.billDao().insert(
                    Bill(
                        accountId = accountId,
                        direction = direction,
                        totalAmount = billTotal,
                        billDate = timestamp,
                        subtotal = subtotal,
                        taxAmount = taxTotal
                    )
                )
                for (line in batch) {
                    db.transactionDao().insert(
                        Transaction(
                            itemId = line.itemId,
                            direction = direction,
                            quantity = line.quantity,
                            timestamp = timestamp,
                            note = line.note,
                            accountId = accountId,
                            billId = billId,
                            rate = line.unitPrice,
                            gstPercent = line.gstPercent,
                            taxAmount = line.taxAmount,
                            lineTotal = line.lineTotal,
                            unitLabel = line.unitLabel,
                            quantityBase = line.quantityBase
                        )
                    )
                }
                offerBillActions(billId, name)
            } catch (e: Exception) {
                saving = false
                Toast.makeText(this@AccountCaptureActivity, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun offerBillActions(billId: Long, accountName: String) {
        AlertDialog.Builder(this)
            .setTitle("Bill saved")
            .setMessage("${batch.size} item(s) logged for $accountName.")
            .setNegativeButton("Done") { _, _ -> goHome() }
            .setNeutralButton("Share PDF") { _, _ ->
                withBillPdf(billId) { file -> sharePdf(this, file, "Share bill") }
            }
            .setPositiveButton("Print") { _, _ ->
                withBillPdf(billId) { file -> printPdf(this, file, "Bill #$billId") }
            }
            .setOnCancelListener { goHome() }
            .show()
    }

    private fun withBillPdf(billId: Long, action: (java.io.File) -> Unit) {
        lifecycleScope.launch {
            try {
                val bill = db.billDao().getById(billId) ?: return@launch
                val account = db.accountDao().getById(bill.accountId) ?: return@launch
                val lines = db.transactionDao().listForBill(billId)
                val file = generateBillPdf(this@AccountCaptureActivity, bill, account, lines)
                action(file)
            } catch (e: Exception) {
                Toast.makeText(this@AccountCaptureActivity, "Could not generate the bill PDF: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                goHome()
            }
        }
    }

    private fun goHome() {
        val homeIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private suspend fun upsertAccount(mobile: String?, name: String, address: String?): Long {
        if (mobile == null) {
            return db.accountDao().insert(Account(mobileNumber = null, name = name, address = address, createdAt = Instant.now().toString()))
        }
        val existing = db.accountDao().getByMobile(mobile)
        return if (existing != null) {
            db.accountDao().update(existing.copy(name = name, address = address))
            existing.id
        } else {
            db.accountDao().insert(Account(mobileNumber = mobile, name = name, address = address, createdAt = Instant.now().toString()))
        }
    }

    companion object {
        private const val EXTRA_DIRECTION = "direction"
        private const val EXTRA_BATCH_JSON = "batch_json"

        fun intent(context: Context, direction: String, batchJson: String): Intent =
            Intent(context, AccountCaptureActivity::class.java)
                .putExtra(EXTRA_DIRECTION, direction)
                .putExtra(EXTRA_BATCH_JSON, batchJson)
    }
}
