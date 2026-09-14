package com.example.expensetrack

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.database.Expense
import com.example.expensetrack.databinding.ActivityExpenseDetailBinding
import kotlinx.coroutines.launch

class ExpenseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseDetailBinding
    private lateinit var database: AppDatabase
    private var expenseId: Int = -1
    private var currentExpense: Expense? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        expenseId = intent.getIntExtra("EXPENSE_ID", -1)

        if (expenseId == -1) { finish(); return }

        binding.tvBack.setOnClickListener { finish() }
        binding.btnEdit.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java).apply { putExtra("EXPENSE_ID", expenseId) })
        }
        binding.btnDelete.setOnClickListener { showDeleteDialog() }

        loadExpenseDetails()
    }

    override fun onResume() {
        super.onResume()
        loadExpenseDetails()
    }

    private fun loadExpenseDetails() {
        lifecycleScope.launch {
            val expense = database.expenseDao().getExpenseById(expenseId)
            currentExpense = expense
            runOnUiThread {
                if (expense != null) {
                    binding.apply {
                        tvDetailAmount.text = Utils.formatCurrency(expense.amount)
                        tvDetailCategory.text = expense.category
                        tvDetailDate.text = expense.date
                        tvDetailNote.text = expense.note.ifEmpty { "No note" }
                        if (expense.receiptUri != null) {
                            ivDetailReceipt.setImageURI(Uri.parse(expense.receiptUri))
                            ivDetailReceipt.visibility = View.VISIBLE
                        } else ivDetailReceipt.visibility = View.GONE
                    }
                } else finish()
            }
        }
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this).setTitle("Delete Expense").setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ -> deleteExpense() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun deleteExpense() {
        lifecycleScope.launch {
            currentExpense?.let {
                database.expenseDao().deleteExpense(it)
                runOnUiThread {
                    Toast.makeText(this@ExpenseDetailActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
