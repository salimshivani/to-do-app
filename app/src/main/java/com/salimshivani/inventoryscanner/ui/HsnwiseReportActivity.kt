package com.salimshivani.inventoryscanner.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.HsnwiseRow
import com.salimshivani.inventoryscanner.databinding.ActivityReportHsnwiseBinding
import com.salimshivani.inventoryscanner.databinding.RowHsnwiseBinding
import kotlinx.coroutines.launch

class HsnwiseReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportHsnwiseBinding
    private lateinit var db: AppDatabase
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportHsnwiseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "HSN Code Report"

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
            val rows = db.transactionDao().hsnwiseReport(from, to)
            adapter.submitList(rows)
            binding.textEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        private var rows: List<HsnwiseRow> = emptyList()
        fun submitList(newRows: List<HsnwiseRow>) {
            rows = newRows
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = RowHsnwiseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val row = rows[position]
            holder.binding.name.text = row.hsn_code
            holder.binding.meta.text = "${row.item_count} item(s)"
            holder.binding.inward.text = "IN ${formatQuantity(row.inward_total)}"
            holder.binding.outward.text = "OUT ${formatQuantity(row.outward_total)}"
            holder.binding.net.text = "Net ${formatQuantity(row.net_stock)}"
        }

        override fun getItemCount() = rows.size
        class ViewHolder(val binding: RowHsnwiseBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
