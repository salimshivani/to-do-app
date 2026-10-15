package com.salimshivani.inventoryscanner.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.salimshivani.inventoryscanner.R
import com.salimshivani.inventoryscanner.data.Account
import com.salimshivani.inventoryscanner.databinding.RowAccountBinding

data class AccountDues(val receivable: Double, val payable: Double)

class AccountsAdapter(private val onClick: (Account) -> Unit) :
    RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {

    private var items: List<Account> = emptyList()
    private var dues: Map<Long, AccountDues> = emptyMap()

    fun submitList(newItems: List<Account>, newDues: Map<Long, AccountDues>) {
        items = newItems
        dues = newDues
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = items[position]
        holder.binding.name.text = account.name
        holder.binding.meta.text = listOfNotNull(account.mobileNumber, account.address).joinToString(" · ")

        val due = dues[account.id]
        if (due != null && (due.receivable > 0 || due.payable > 0)) {
            holder.binding.dues.visibility = View.VISIBLE
            val parts = mutableListOf<String>()
            if (due.receivable > 0) parts.add("Receivable ${formatQuantity(due.receivable)}")
            if (due.payable > 0) parts.add("Payable ${formatQuantity(due.payable)}")
            holder.binding.dues.text = parts.joinToString(" · ")
            holder.binding.dues.setTextColor(
                ContextCompat.getColor(holder.binding.root.context, if (due.receivable > 0) R.color.inward_green else R.color.outward_red)
            )
        } else {
            holder.binding.dues.visibility = View.GONE
        }
        holder.binding.root.setOnClickListener { onClick(account) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(val binding: RowAccountBinding) : RecyclerView.ViewHolder(binding.root)
}
