package com.salimshivani.inventoryscanner.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.salimshivani.inventoryscanner.data.Item
import com.salimshivani.inventoryscanner.databinding.ItemRowBinding

class ItemsAdapter(private val onClick: (Item) -> Unit) :
    RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

    private var items: List<Item> = emptyList()

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.name.text = item.name
        val metaParts = mutableListOf(item.barcode)
        item.hsnCode?.let { metaParts.add("HSN $it") }
        item.unit?.let { metaParts.add(it) }
        holder.binding.meta.text = metaParts.joinToString(" · ")
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root)
}
