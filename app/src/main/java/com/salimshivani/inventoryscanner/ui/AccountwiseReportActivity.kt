package com.salimshivani.inventoryscanner.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salimshivani.inventoryscanner.data.AccountwiseRow
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.databinding.ActivityReportAccountwiseBinding
import com.salimshivani.inventoryscanner.databinding.RowAccountwiseBinding
import com.salimshivani.inventoryscanner.print.generateReportPdf
import kotlinx.coroutines.launch

class AccountwiseReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportAccountwiseBinding
    private lateinit var db: AppDatabase
    private val adapter = Adapter()
    private var currentRows: List<AccountwiseRow> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportAccountwiseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Customer-wise Report"

        db = AppDatabase.getInstance(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.dateFilter.btnClear.setOnClickListener {
            binding.dateFilter.inputFrom.setText("")
            binding.dateFilter.inputTo.setText("")
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = load()
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.dateFilter.inputFrom.addTextChangedListener(watcher)
        binding.dateFilter.inputTo.addTextChangedListener(watcher)
        binding.dateFilter.btnSharePdf.setOnClickListener { sharePdfReport() }

        load()
    }

    private fun load() {
        val from = binding.dateFilter.inputFrom.text.toString().trim().ifEmpty { null }
        val to = binding.dateFilter.inputTo.text.toString().trim().ifEmpty { null }
        lifecycleScope.launch {
            val rows = db.transactionDao().accountwiseReport(from, to)
            currentRows = rows
            adapter.submitList(rows)
            binding.textEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun sharePdfReport() {
        if (currentRows.isEmpty()) {
            Toast.makeText(this, "No data to export.", Toast.LENGTH_SHORT).show()
            return
        }
        val rows = currentRows.map {
            listOf(
                it.account_name, it.mobile_number ?: "-", formatQuantity(it.inward_total), formatQuantity(it.outward_total),
                formatQuantity(it.net_stock), formatMoney(it.unpaid_receivable), formatMoney(it.unpaid_payable)
            )
        }
        val file = generateReportPdf(
            this, "Customer-wise Report", null,
            listOf("Account", "Mobile", "Inward", "Outward", "Net", "Receivable Due", "Payable Due"), rows
        )
        offerPrintOrShare(this, "Customer-wise Report", file)
    }

    private class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        private var rows: List<AccountwiseRow> = emptyList()
        fun submitList(newRows: List<AccountwiseRow>) {
            rows = newRows
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = RowAccountwiseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val row = rows[position]
            holder.binding.name.text = row.account_name
            holder.binding.meta.text = row.mobile_number ?: "${row.transaction_count} transaction(s)"
            holder.binding.inward.text = "IN ${formatQuantity(row.inward_total)}"
            holder.binding.outward.text = "OUT ${formatQuantity(row.outward_total)}"
            holder.binding.net.text = "Net ${formatQuantity(row.net_stock)}"
        }

        override fun getItemCount() = rows.size
        class ViewHolder(val binding: RowAccountwiseBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
