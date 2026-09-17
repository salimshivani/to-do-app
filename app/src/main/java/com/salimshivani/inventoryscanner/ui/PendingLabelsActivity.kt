package com.salimshivani.inventoryscanner.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.Item
import com.salimshivani.inventoryscanner.databinding.ActivityPendingLabelsBinding
import com.salimshivani.inventoryscanner.databinding.RowPendingLabelBinding
import kotlinx.coroutines.launch

class PendingLabelsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPendingLabelsBinding
    private lateinit var db: AppDatabase
    private val adapter = Adapter { item -> startActivity(PrintLabelActivity.intent(this, item.id)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingLabelsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Pending Labels"

        db = AppDatabase.getInstance(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val items = db.itemDao().listPendingLabels()
            adapter.submitList(items)
            binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private class Adapter(private val onClick: (Item) -> Unit) : RecyclerView.Adapter<Adapter.ViewHolder>() {
        private var items: List<Item> = emptyList()
        fun submitList(newItems: List<Item>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = RowPendingLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.name.text = item.name
            holder.binding.meta.text = item.barcode
            holder.binding.root.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
        class ViewHolder(val binding: RowPendingLabelBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
