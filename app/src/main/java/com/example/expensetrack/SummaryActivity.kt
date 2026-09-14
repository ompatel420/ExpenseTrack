package com.example.expensetrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.databinding.ActivitySummaryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        binding.tvSummaryMonth.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        binding.tvBack.setOnClickListener { finish() }

        loadSummary()
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            val monthFilter = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
            val monthlyExpenses = database.expenseDao().getAllExpenses().filter { it.date.contains(monthFilter) }

            runOnUiThread {
                binding.apply {
                    tvSummaryTotal.text = Utils.formatCurrency(monthlyExpenses.sumOf { it.amount })
                    tvFoodTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Food" }.sumOf { it.amount })
                    tvTravelTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Travel" }.sumOf { it.amount })
                    tvShoppingTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Shopping" }.sumOf { it.amount })
                    tvBillsTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Bills" }.sumOf { it.amount })
                    tvEntertainmentTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Entertainment" }.sumOf { it.amount })
                    tvOtherTotal.text = Utils.formatCurrency(monthlyExpenses.filter { it.category == "Other" }.sumOf { it.amount })
                }
            }
        }
    }
}
