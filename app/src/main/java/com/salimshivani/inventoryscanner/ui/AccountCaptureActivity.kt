package com.salimshivani.inventoryscanner.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.data.Account
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.DIRECTION_INWARD
import com.salimshivani.inventoryscanner.data.Transaction
import com.salimshivani.inventoryscanner.databinding.ActivityAccountCaptureBinding
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
        title = "Customer Details"

        db = AppDatabase.getInstance(this)
        direction = intent.getStringExtra(EXTRA_DIRECTION) ?: DIRECTION_INWARD
        batch = parsePendingLineItems(intent.getStringExtra(EXTRA_BATCH_JSON).orEmpty())

        val isInward = direction == DIRECTION_INWARD
        binding.bannerText.text = if (isInward) "INWARD — customer details" else "OUTWARD — customer details"
        binding.bannerText.setBackgroundColor(
            ContextCompat.getColor(this, if (isInward) R.color.inward_green else R.color.outward_red)
        )

        renderBatch()

        binding.inputMobile.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) lookupAccountByMobile()
        }
        binding.btnComplete.setOnClickListener { complete() }
    }

    private fun renderBatch() {
        binding.batchContainer.removeAllViews()
        for (line in batch) {
            val row = TextView(this).apply {
                text = "${line.itemName} — ${formatQuantity(line.quantity)} unit(s)" +
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
            binding.textLookupStatus.visibility = android.view.View.GONE
            return
        }
        lifecycleScope.launch {
            val account = db.accountDao().getByMobile(mobile)
            binding.textLookupStatus.visibility = android.view.View.VISIBLE
            if (account != null) {
                binding.inputName.setText(account.name)
                binding.inputAddress.setText(account.address ?: "")
                binding.textLookupStatus.text = "Existing customer found and loaded."
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
            Toast.makeText(this, "Enter the customer's name.", Toast.LENGTH_SHORT).show()
            return
        }
        val mobile = binding.inputMobile.text.toString().trim().ifEmpty { null }
        val address = binding.inputAddress.text.toString().trim().ifEmpty { null }

        saving = true
        lifecycleScope.launch {
            try {
                val accountId = upsertAccount(mobile, name, address)
                val timestamp = Instant.now().toString()
                for (line in batch) {
                    db.transactionDao().insert(
                        Transaction(
                            itemId = line.itemId,
                            direction = direction,
                            quantity = line.quantity,
                            timestamp = timestamp,
                            note = line.note,
                            accountId = accountId
                        )
                    )
                }
                Toast.makeText(this@AccountCaptureActivity, "${batch.size} item(s) logged for $name.", Toast.LENGTH_LONG).show()
                val homeIntent = Intent(this@AccountCaptureActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            } catch (e: Exception) {
                saving = false
                Toast.makeText(this@AccountCaptureActivity, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun upsertAccount(mobile: String?, name: String, address: String?): Long? {
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
