package com.salimshivani.inventoryscanner.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.data.DIRECTION_INWARD
import com.salimshivani.inventoryscanner.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.app_name)

        binding.btnScanInward.title.text = "Scan Inward"
        binding.btnScanInward.subtitle.text = "Receive stock into inventory"
        binding.btnScanInward.dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.inward_green))
        binding.btnScanInward.root.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java).putExtra(ScanActivity.EXTRA_DIRECTION, DIRECTION_INWARD))
        }

        binding.btnScanOutward.title.text = "Scan Outward"
        binding.btnScanOutward.subtitle.text = "Dispatch stock out of inventory"
        binding.btnScanOutward.dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.outward_red))
        binding.btnScanOutward.root.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java).putExtra(ScanActivity.EXTRA_DIRECTION, "outward"))
        }

        binding.btnItems.title.text = "Items"
        binding.btnItems.subtitle.text = "View and edit item master data"
        binding.btnItems.root.setOnClickListener { startActivity(Intent(this, ItemsActivity::class.java)) }

        binding.btnReports.title.text = "Reports"
        binding.btnReports.subtitle.text = "Datewise, itemwise, HSN-wise"
        binding.btnReports.root.setOnClickListener { startActivity(Intent(this, ReportsActivity::class.java)) }

        binding.btnPendingLabels.title.text = "Pending Labels"
        binding.btnPendingLabels.subtitle.text = "Generated barcodes waiting to be printed"
        binding.btnPendingLabels.dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_purple))
        binding.btnPendingLabels.root.setOnClickListener { startActivity(Intent(this, PendingLabelsActivity::class.java)) }

        binding.btnData.title.text = "Import / Export"
        binding.btnData.subtitle.text = "Backup, restore, CSV"
        binding.btnData.root.setOnClickListener { startActivity(Intent(this, DataActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun loadStats() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val rows = db.transactionDao().itemwiseReport(null, null)
            binding.textItemCount.text = rows.size.toString()
            val net = rows.sumOf { it.net_stock }
            binding.textNetStock.text = if (net == net.toLong().toDouble()) net.toLong().toString() else net.toString()
        }
    }
}
