package com.salimshivani.inventoryscanner.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.Item
import com.salimshivani.inventoryscanner.databinding.ActivityItemsBinding
import kotlinx.coroutines.launch

class ItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityItemsBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: ItemsAdapter
    private var allItems: List<Item> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Items"

        db = AppDatabase.getInstance(this)
        adapter = ItemsAdapter { item ->
            startActivity(ItemFormActivity.editIntent(this, item.id))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabAddItem.setOnClickListener {
            startActivity(ItemFormActivity.newIntent(this))
        }

        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = applyFilter(s.toString())
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun loadItems() {
        lifecycleScope.launch {
            allItems = db.itemDao().listAll()
            applyFilter(binding.inputSearch.text.toString())
        }
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) allItems else allItems.filter {
            it.name.lowercase().contains(q) || it.barcode.lowercase().contains(q) ||
                (it.hsnCode?.lowercase()?.contains(q) == true)
        }
        adapter.submitList(filtered)
        binding.textEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
