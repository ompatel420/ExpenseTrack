package com.example.expensetrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.databinding.ActivitySummaryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        lifecycleScope.launch(Dispatchers.IO) {
            val monthFilter = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
            val monthlyExpenses = database.expenseDao().getAllExpenses().filter { it.date.contains(monthFilter) }

            withContext(Dispatchers.Main) {
                binding.apply {
                    tvSummaryTotal.text = Utils.formatCurrency(monthlyExpenses.sumOf { it.amount })
                    
                    val categories = resources.getStringArray(R.array.categories)
                    categories.forEach { category ->
                        val total = monthlyExpenses.filter { it.category == category }.sumOf { it.amount }
                        val formattedTotal = Utils.formatCurrency(total)
                        
                        when (category) {
                            "Food" -> tvFoodTotal.text = formattedTotal
                            "Travel" -> tvTravelTotal.text = formattedTotal
                            "Shopping" -> tvShoppingTotal.text = formattedTotal
                            "Bills" -> tvBillsTotal.text = formattedTotal
                            "Entertainment" -> tvEntertainmentTotal.text = formattedTotal
                            "Other" -> tvOtherTotal.text = formattedTotal
                        }
                    }
                }
            }
        }
    }
}
