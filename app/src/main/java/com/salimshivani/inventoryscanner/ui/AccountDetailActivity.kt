package com.salimshivani.inventoryscanner.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.Bill
import com.salimshivani.inventoryscanner.data.DIRECTION_INWARD
import com.salimshivani.inventoryscanner.data.DIRECTION_OUTWARD
import com.salimshivani.inventoryscanner.data.Payment
import com.salimshivani.inventoryscanner.databinding.ActivityAccountDetailBinding
import com.salimshivani.inventoryscanner.databinding.DialogRecordPaymentBinding
import com.salimshivani.inventoryscanner.print.generateBillPdf
import com.salimshivani.inventoryscanner.print.printPdf
import com.salimshivani.inventoryscanner.print.sharePdf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class AccountDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountDetailBinding
    private lateinit var db: AppDatabase
    private var accountId: Long = -1L
    private var bills: List<Bill> = emptyList()
    private val selectedIds = mutableSetOf<Long>()
    private lateinit var adapter: BillAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Account"

        db = AppDatabase.getInstance(this)
        accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1L)

        adapter = BillAdapter(selectedIds, { updateSelectionBars() }, { bill -> showBillPdfOptions(bill) })
        binding.billsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.billsRecyclerView.adapter = adapter

        binding.btnRecordReceived.setOnClickListener { showPaymentDialog(DIRECTION_OUTWARD) }
        binding.btnRecordPaid.setOnClickListener { showPaymentDialog(DIRECTION_INWARD) }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val account = db.accountDao().getById(accountId) ?: return@launch
            binding.textName.text = account.name
            binding.textMobile.text = account.mobileNumber ?: "No mobile number on file"
            binding.textAddress.text = account.address ?: ""

            bills = db.billDao().listForAccount(accountId)
            selectedIds.retainAll(bills.map { it.id }.toSet())
            renderBills()
            updateSelectionBars()

            val payments = db.paymentDao().listForAccount(accountId)
            renderPayments(payments)
        }
    }

    private fun renderBills() {
        binding.textNoBills.visibility = if (bills.isEmpty()) View.VISIBLE else View.GONE
        adapter.submitList(bills)
    }

    private fun renderPayments(payments: List<Payment>) {
        binding.paymentsContainer.removeAllViews()
        if (payments.isEmpty()) {
            binding.paymentsContainer.addView(
                TextView(this).apply {
                    text = "No payments recorded yet."
                    setTextColor(ContextCompat.getColor(this@AccountDetailActivity, R.color.text_faint))
                }
            )
            return
        }
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        for (p in payments) {
            val row = TextView(this).apply {
                val label = if (p.direction == DIRECTION_INWARD) "Paid" else "Received"
                val dateText = try {
                    formatter.format(Date.from(Instant.parse(p.paymentDate)))
                } catch (e: Exception) {
                    p.paymentDate
                }
                text = "$label ${formatMoney(p.amount)} on $dateText" + (p.note?.let { " — $it" } ?: "")
                setTextColor(ContextCompat.getColor(this@AccountDetailActivity, R.color.text_primary))
                textSize = 13f
                setPadding(0, 6, 0, 6)
            }
            binding.paymentsContainer.addView(row)
        }
    }

    private fun updateSelectionBars() {
        val receivableSum = bills.filter { it.id in selectedIds && it.direction == DIRECTION_OUTWARD }.sumOf { it.totalAmount }
        val payableSum = bills.filter { it.id in selectedIds && it.direction == DIRECTION_INWARD }.sumOf { it.totalAmount }

        binding.receivableBar.visibility = if (receivableSum > 0) View.VISIBLE else View.GONE
        binding.textReceivableSelected.text = "Selected receivable: ${formatMoney(receivableSum)}"

        binding.payableBar.visibility = if (payableSum > 0) View.VISIBLE else View.GONE
        binding.textPayableSelected.text = "Selected payable: ${formatMoney(payableSum)}"
    }

    private fun showBillPdfOptions(bill: Bill) {
        AlertDialog.Builder(this)
            .setTitle("Bill #${bill.id}")
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Share PDF") { _, _ ->
                withBillPdf(bill) { file -> sharePdf(this, file, "Share bill") }
            }
            .setPositiveButton("Print") { _, _ ->
                withBillPdf(bill) { file -> printPdf(this, file, "Bill #${bill.id}") }
            }
            .show()
    }

    private fun withBillPdf(bill: Bill, action: (java.io.File) -> Unit) {
        lifecycleScope.launch {
            try {
                val account = db.accountDao().getById(bill.accountId) ?: return@launch
                val lines = db.transactionDao().listForBill(bill.id)
                val file = generateBillPdf(this@AccountDetailActivity, bill, account, lines)
                action(file)
            } catch (e: Exception) {
                Toast.makeText(this@AccountDetailActivity, "Could not generate the bill PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPaymentDialog(direction: String) {
        val selectedBills = bills.filter { it.id in selectedIds && it.direction == direction }
        if (selectedBills.isEmpty()) return
        val sum = selectedBills.sumOf { it.totalAmount }

        val dialogBinding = DialogRecordPaymentBinding.inflate(LayoutInflater.from(this))
        dialogBinding.textBillCount.text = "${selectedBills.size} bill(s) selected, totaling ${formatMoney(sum)}"
        dialogBinding.inputAmount.setText(formatMoney(sum))
        dialogBinding.inputDate.setText(DateTimeFormatter.ISO_LOCAL_DATE.format(java.time.LocalDate.now()))

        val title = if (direction == DIRECTION_INWARD) "Record Payment Made" else "Record Payment Received"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val amount = dialogBinding.inputAmount.text.toString().trim().toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Enter a valid amount.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val date = dialogBinding.inputDate.text.toString().trim().ifEmpty {
                    DateTimeFormatter.ISO_LOCAL_DATE.format(java.time.LocalDate.now())
                }
                val note = dialogBinding.inputNote.text.toString().trim().ifEmpty { null }
                recordPayment(direction, selectedBills.map { it.id }, amount, date, note)
            }
            .show()
    }

    private fun recordPayment(direction: String, billIds: List<Long>, amount: Double, date: String, note: String?) {
        lifecycleScope.launch {
            val paymentId = db.paymentDao().insert(
                Payment(accountId = accountId, direction = direction, amount = amount, paymentDate = date, note = note)
            )
            db.billDao().markPaid(billIds, paymentId)
            selectedIds.removeAll(billIds.toSet())
            Toast.makeText(this@AccountDetailActivity, "Payment recorded for ${billIds.size} bill(s).", Toast.LENGTH_SHORT).show()
            load()
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT_ID = "account_id"
        fun intent(context: Context, accountId: Long): Intent =
            Intent(context, AccountDetailActivity::class.java).putExtra(EXTRA_ACCOUNT_ID, accountId)
    }
}
