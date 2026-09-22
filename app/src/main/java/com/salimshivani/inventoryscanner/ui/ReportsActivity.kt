package com.salimshivani.inventoryscanner.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.salimshivani.inventoryscanner.databinding.ActivityReportsBinding

class ReportsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Reports"

        binding.btnDatewise.title.text = "Datewise Report"
        binding.btnDatewise.subtitle.text = "Inward / outward totals per day"
        binding.btnDatewise.root.setOnClickListener { startActivity(Intent(this, DatewiseReportActivity::class.java)) }

        binding.btnItemwise.title.text = "Itemwise Report"
        binding.btnItemwise.subtitle.text = "Stock movement and balance per item"
        binding.btnItemwise.root.setOnClickListener { startActivity(Intent(this, ItemwiseReportActivity::class.java)) }

        binding.btnHsnwise.title.text = "HSN Code Report"
        binding.btnHsnwise.subtitle.text = "Stock movement grouped by HSN code"
        binding.btnHsnwise.root.setOnClickListener { startActivity(Intent(this, HsnwiseReportActivity::class.java)) }

        binding.btnAccountwise.title.text = "Customer-wise Report"
        binding.btnAccountwise.subtitle.text = "Stock movement grouped by customer"
        binding.btnAccountwise.root.setOnClickListener { startActivity(Intent(this, AccountwiseReportActivity::class.java)) }
    }
}
