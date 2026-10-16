package com.salimshivani.inventoryscanner.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.data.BILL_STATUS_PAID
import com.salimshivani.inventoryscanner.data.Bill
import com.salimshivani.inventoryscanner.data.DIRECTION_INWARD
import com.salimshivani.inventoryscanner.databinding.RowBillBinding
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

class BillAdapter(
    private val selectedIds: MutableSet<Long>,
    private val onSelectionChanged: () -> Unit,
    private val onPdfClick: (Bill) -> Unit
) : RecyclerView.Adapter<BillAdapter.ViewHolder>() {

    private var bills: List<Bill> = emptyList()
    private val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun submitList(newBills: List<Bill>) {
        bills = newBills
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bill = bills[position]
        val context = holder.binding.root.context
        val isUnpaid = bill.status != BILL_STATUS_PAID

        holder.binding.billLabel.text = if (bill.direction == DIRECTION_INWARD) "Payable" else "Receivable"
        holder.binding.billDate.text = try {
            formatter.format(Date.from(Instant.parse(bill.billDate)))
        } catch (e: Exception) {
            bill.billDate
        }
        holder.binding.billAmount.text = formatMoney(bill.totalAmount)
        holder.binding.billAmount.setTextColor(
            ContextCompat.getColor(context, if (bill.direction == DIRECTION_INWARD) R.color.outward_red else R.color.inward_green)
        )
        holder.binding.billStatus.text = if (isUnpaid) "UNPAID" else "PAID"
        holder.binding.billStatus.setTextColor(
            ContextCompat.getColor(context, if (isUnpaid) R.color.outward_red else R.color.inward_green)
        )

        holder.binding.checkbox.visibility = if (isUnpaid) View.VISIBLE else View.INVISIBLE
        holder.binding.checkbox.setOnCheckedChangeListener(null)
        holder.binding.checkbox.isChecked = selectedIds.contains(bill.id)
        holder.binding.checkbox.setOnCheckedChangeListener { _, checked ->
            if (checked) selectedIds.add(bill.id) else selectedIds.remove(bill.id)
            onSelectionChanged()
        }

        holder.binding.btnBillPdf.setOnClickListener { onPdfClick(bill) }
    }

    override fun getItemCount(): Int = bills.size

    class ViewHolder(val binding: RowBillBinding) : RecyclerView.ViewHolder(binding.root)
}
