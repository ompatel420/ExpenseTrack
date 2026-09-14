package com.example.expensetrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        binding.btnAddExpense.setOnClickListener { startActivity(Intent(this, AddExpenseActivity::class.java)) }
        binding.btnViewExpenses.setOnClickListener { startActivity(Intent(this, ExpenseListActivity::class.java)) }
        binding.btnSummary.setOnClickListener { startActivity(Intent(this, SummaryActivity::class.java)) }

        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            val expenses = database.expenseDao().getAllExpenses()
            val monthFilter = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
            val monthlyExpenses = expenses.filter { it.date.contains(monthFilter) }

            runOnUiThread {
                binding.apply {
                    tvTotalExpense.text = Utils.formatCurrency(monthlyExpenses.sumOf { it.amount })
                    tvFoodTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Food" }.sumOf { it.amount })
                    tvTravelTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Travel" }.sumOf { it.amount })
                    tvShoppingTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Shopping" }.sumOf { it.amount })
                    tvBillsTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Bills" }.sumOf { it.amount })
                    tvEntertainmentTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Entertainment" }.sumOf { it.amount })
                    tvOtherTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Other" }.sumOf { it.amount })

                    if (expenses.isEmpty()) {
                        tvExpenseMessage.text = "Start adding your expenses"
                        tvCategoryInfo.text = "Add expenses to see"
                        tvRecentInfo.text = "No expenses yet"
                        layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        tvExpenseMessage.text = "${expenses.size} expense(s) recorded"
                        tvCategoryInfo.text = "Updated automatically"
                        tvRecentInfo.text = "${expenses.size} recorded"
                        layoutEmptyState.visibility = View.GONE
                    }
                }
            }
        }
    }
}
