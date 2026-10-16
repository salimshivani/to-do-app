package com.salimshivani.inventoryscanner.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.salimshivani.inventoryscanner.data.Account
import com.salimshivani.inventoryscanner.data.AppDatabase
import com.salimshivani.inventoryscanner.databinding.ActivityAccountsBinding
import kotlinx.coroutines.launch

class AccountsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountsBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: AccountsAdapter
    private var allAccounts: List<Account> = emptyList()
    private var duesByAccount: Map<Long, AccountDues> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Accounts"

        db = AppDatabase.getInstance(this)
        adapter = AccountsAdapter { account ->
            startActivity(AccountDetailActivity.intent(this, account.id))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = applyFilter(s.toString())
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadAccounts()
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            allAccounts = db.accountDao().listAll()
            val rows = db.transactionDao().accountwiseReport(null, null)
            duesByAccount = rows.filter { it.account_id != null && it.account_id != -1L }
                .associate { it.account_id!! to AccountDues(it.unpaid_receivable, it.unpaid_payable) }
            applyFilter(binding.inputSearch.text.toString())
        }
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) allAccounts else allAccounts.filter {
            it.name.lowercase().contains(q) || (it.mobileNumber?.contains(q) == true)
        }
        adapter.submitList(filtered, duesByAccount)
        binding.textEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
