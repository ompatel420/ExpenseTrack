package com.example.expensetrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetrack.adapter.ExpenseAdapter
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.database.Expense
import com.example.expensetrack.databinding.ActivityExpenseListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseListBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: ExpenseAdapter
    private var allExpenses: List<Expense> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        setupRecyclerView()
        binding.tvBack.setOnClickListener { finish() }
        binding.btnShareAll.setOnClickListener { shareAllExpenses() }
        
        loadExpenses()
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(emptyList()) { expense ->
            startActivity(Intent(this, ExpenseDetailActivity::class.java).apply { putExtra("EXPENSE_ID", expense.id) })
        }
        binding.recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(this@ExpenseListActivity)
            adapter = this@ExpenseListActivity.adapter
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch(Dispatchers.IO) {
            allExpenses = database.expenseDao().getAllExpenses()
            withContext(Dispatchers.Main) {
                if (allExpenses.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewExpenses.visibility = View.GONE
                    binding.btnShareAll.visibility = View.GONE
                    binding.tvExpenseCount.text = "No expenses recorded"
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewExpenses.visibility = View.VISIBLE
                    binding.btnShareAll.visibility = View.VISIBLE
                    binding.tvExpenseCount.text = "${allExpenses.size} expenses"
                    adapter.updateList(allExpenses)
                }
            }
        }
    }

    private fun shareAllExpenses() {
        if (allExpenses.isEmpty()) return

        val shareText = StringBuilder("ExpenseTrack - All Expenses\n\n")
        var total = 0.0
        
        allExpenses.forEach { exp ->
            shareText.append("${exp.date} | ${exp.category}: ${formatCurrency(exp.amount)}\n")
            if (exp.note.isNotEmpty()) shareText.append("Note: ${exp.note}\n")
            shareText.append("-----------------\n")
            total += exp.amount
        }
        
        shareText.append("\nTotal Expenses: ${formatCurrency(total)}")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText.toString())
        }
        startActivity(Intent.createChooser(intent, "Share All via"))
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }
}
