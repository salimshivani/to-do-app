package com.salimshivani.inventoryscanner.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.DatewiseRow
import com.salimshivani.inventoryscanner.databinding.ActivityReportDatewiseBinding
import com.salimshivani.inventoryscanner.databinding.RowDatewiseBinding
import kotlinx.coroutines.launch

class DatewiseReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportDatewiseBinding
    private lateinit var db: AppDatabase
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDatewiseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Datewise Report"

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

        load()
    }

    private fun load() {
        val from = binding.dateFilter.inputFrom.text.toString().trim().ifEmpty { null }
        val to = binding.dateFilter.inputTo.text.toString().trim().ifEmpty { null }
        lifecycleScope.launch {
            val rows = db.transactionDao().datewiseReport(from, to)
            adapter.submitList(rows)
            binding.textEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        private var rows: List<DatewiseRow> = emptyList()
        fun submitList(newRows: List<DatewiseRow>) {
            rows = newRows
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = RowDatewiseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val row = rows[position]
            holder.binding.date.text = row.date
            holder.binding.direction.text = if (row.direction == "inward") "IN" else "OUT"
            holder.binding.direction.setTextColor(
                ContextCompat.getColor(this@DatewiseReportActivity, if (row.direction == "inward") R.color.inward_green else R.color.outward_red)
            )
            holder.binding.quantity.text = formatQuantity(row.total_quantity)
            holder.binding.count.text = "${row.transaction_count} txn"
        }

        override fun getItemCount() = rows.size
        inner class ViewHolder(val binding: RowDatewiseBinding) : RecyclerView.ViewHolder(binding.root)
    }
}

fun formatQuantity(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
