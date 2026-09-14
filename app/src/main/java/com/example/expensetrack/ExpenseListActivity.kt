package com.example.expensetrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetrack.adapter.ExpenseAdapter
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.databinding.ActivityExpenseListBinding
import kotlinx.coroutines.launch

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseListBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupRecyclerView()

        binding.tvBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(emptyList()) { expense ->
            val intent = Intent(this, ExpenseDetailActivity::class.java)
            intent.putExtra("EXPENSE_ID", expense.id)
            startActivity(intent)
        }

        binding.recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(this@ExpenseListActivity)
            adapter = this@ExpenseListActivity.adapter
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val expenses = database.expenseDao().getAllExpenses()
            
            runOnUiThread {
                if (expenses.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewExpenses.visibility = View.GONE
                    binding.tvExpenseCount.text = "No expenses recorded"
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewExpenses.visibility = View.VISIBLE
                    binding.tvExpenseCount.text = "${expenses.size} expenses"
                    adapter.updateList(expenses)
                }
            }
        }
    }
}
