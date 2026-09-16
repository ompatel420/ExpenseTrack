package com.example.expensetrack

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private val PREFS_NAME = "ExpenseTrackPrefs"
    private val KEY_LIMIT = "monthly_limit"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        binding.btnAddExpense.setOnClickListener { startActivity(Intent(this, AddExpenseActivity::class.java)) }
        binding.btnViewExpenses.setOnClickListener { startActivity(Intent(this, ExpenseListActivity::class.java)) }
        binding.btnSummary.setOnClickListener { startActivity(Intent(this, SummaryActivity::class.java)) }
        binding.btnSetLimit.setOnClickListener { showSetLimitDialog() }

        setupCategoryListeners()

        loadDashboard()
    }

    private fun setupCategoryListeners() {
        binding.apply {
            cardFood.setOnClickListener { showCategoryLimitDialog("Food") }
            cardTravel.setOnClickListener { showCategoryLimitDialog("Travel") }
            cardShopping.setOnClickListener { showCategoryLimitDialog("Shopping") }
            cardBills.setOnClickListener { showCategoryLimitDialog("Bills") }
            cardEntertainment.setOnClickListener { showCategoryLimitDialog("Entertainment") }
            cardOther.setOnClickListener { showCategoryLimitDialog("Other") }
        }
    }

    private fun showCategoryLimitDialog(category: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "limit_$category"
        val currentLimit = prefs.getFloat(key, 0f)

        val editText = EditText(this).apply {
            hint = "Enter limit for $category"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (currentLimit > 0) setText(currentLimit.toString())
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(60, 20, 60, 20)
            layoutParams = lp
            addView(editText)
        }

        AlertDialog.Builder(this)
            .setTitle("Set $category Limit")
            .setMessage("Set your spending limit for $category")
            .setView(container)
            .setPositiveButton("Set") { _, _ ->
                val limit = editText.text.toString().toFloatOrNull() ?: 0f
                prefs.edit().putFloat(key, limit).apply()
                loadDashboard()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSetLimitDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLimit = prefs.getFloat(KEY_LIMIT, 0f)

        val editText = EditText(this).apply {
            hint = "Enter monthly limit"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (currentLimit > 0) setText(currentLimit.toString())
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(60, 20, 60, 20)
            layoutParams = lp
            addView(editText)
        }

        AlertDialog.Builder(this)
            .setTitle("Set Monthly Limit")
            .setMessage("Set your spending limit for this month")
            .setView(container)
            .setPositiveButton("Set") { _, _ ->
                val limit = editText.text.toString().toFloatOrNull() ?: 0f
                prefs.edit().putFloat(KEY_LIMIT, limit).apply()
                loadDashboard()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        lifecycleScope.launch(Dispatchers.IO) {
            val expenses = database.expenseDao().getAllExpenses()
            val monthFilter = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
            val monthlyExpenses = expenses.filter { it.date.contains(monthFilter) }
            val totalSpent = monthlyExpenses.sumOf { it.amount }

            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val limit = prefs.getFloat(KEY_LIMIT, 0f)

            withContext(Dispatchers.Main) {
                binding.apply {
                    tvTotalExpense.text = Utils.formatCurrency(totalSpent)
                    
                    updateLimitDisplay(limit, totalSpent)

                    // Update category displays
                    updateCategoryDisplay("Food", monthlyExpenses, prefs)
                    updateCategoryDisplay("Travel", monthlyExpenses, prefs)
                    updateCategoryDisplay("Shopping", monthlyExpenses, prefs)
                    updateCategoryDisplay("Bills", monthlyExpenses, prefs)
                    updateCategoryDisplay("Entertainment", monthlyExpenses, prefs)
                    updateCategoryDisplay("Other", monthlyExpenses, prefs)

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

    private fun ActivityMainBinding.updateLimitDisplay(limit: Float, totalSpent: Double) {
        if (limit > 0) {
            val remaining = limit - totalSpent
            tvRemainingLimit.text = if (remaining >= 0) {
                "₹${String.format(Locale.getDefault(), "%.0f", remaining)} remaining"
            } else {
                "₹${String.format(Locale.getDefault(), "%.0f", -remaining)} over limit"
            }
            tvRemainingLimit.setTextColor(if (remaining >= 0) 0xFFFFFFFF.toInt() else 0xFFFEE2E2.toInt())
            tvMonthlyLimit.text = "Limit: ${Utils.formatCurrency(limit.toDouble())}"
        } else {
            tvRemainingLimit.text = "Set Limit"
            tvMonthlyLimit.text = "Limit: Not set"
            tvRemainingLimit.setTextColor(0xFFFFFFFF.toInt())
        }
    }

    private fun ActivityMainBinding.updateCategoryDisplay(category: String, expenses: List<com.example.expensetrack.database.Expense>, prefs: android.content.SharedPreferences) {
        val total = expenses.filter { it.category == category }.sumOf { it.amount }
        val limit = prefs.getFloat("limit_$category", 0f)

        when (category) {
            "Food" -> {
                tvFoodTotal.text = Utils.formatCurrency(total)
                updateCategoryLimitUI(tvFoodRemaining, tvFoodLimit, total, limit)
            }
            "Travel" -> {
                tvTravelTotal.text = Utils.formatCurrency(total)
                updateCategoryLimitUI(tvTravelRemaining, tvTravelLimit, total, limit)
            }
            "Shopping" -> {
                tvShoppingTotal.text = Utils.formatCurrency(total)
                updateCategoryLimitUI(tvShoppingRemaining, tvShoppingLimit, total, limit)
            }
            "Bills" -> {
                tvBillsTotal.text = Utils.formatCurrency(total)
                updateCategoryLimitUI(tvBillsRemaining, tvBillsLimit, total, limit)
            }
            "Entertainment" -> {
                tvEntertainmentTotal.text = Utils.formatCurrency(total)
                updateCategoryLimitUI(tvEntertainmentRemaining, tvEntertainmentLimit, total, limit)
            }
            "Other" -> {
                tvOtherTotal.text = Utils.formatCurrency(total)
                updateCategoryLimitUI(tvOtherRemaining, tvOtherLimit, total, limit)
            }
        }
    }

    private fun updateCategoryLimitUI(tvRemaining: android.widget.TextView, tvLimit: android.widget.TextView, total: Double, limit: Float) {
        if (limit > 0) {
            val remaining = limit - total
            tvRemaining.text = if (remaining >= 0) {
                "₹${String.format(Locale.getDefault(), "%.0f", remaining)} remaining"
            } else {
                "₹${String.format(Locale.getDefault(), "%.0f", -remaining)} over limit"
            }
            tvRemaining.setTextColor(if (remaining >= 0) 0xFF0B6B4F.toInt() else 0xFFB91C1C.toInt())
            tvLimit.text = "Limit: ${Utils.formatCurrency(limit.toDouble())}"
            tvLimit.visibility = android.view.View.VISIBLE
        } else {
            tvRemaining.text = "Set Limit"
            tvRemaining.setTextColor(0xFF0B6B4F.toInt())
            tvLimit.visibility = android.view.View.GONE
        }
    }
}
