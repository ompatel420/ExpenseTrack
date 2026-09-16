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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

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
        binding.btnShareExpense.setOnClickListener { shareExpense() }

        loadExpenseDetails()
    }

    override fun onResume() {
        super.onResume()
        loadExpenseDetails()
    }

    private fun loadExpenseDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            val expense = database.expenseDao().getExpenseById(expenseId)
            currentExpense = expense
            withContext(Dispatchers.Main) {
                if (expense != null) {
                    binding.apply {
                        tvDetailAmount.text = formatCurrency(expense.amount)
                        tvDetailCategory.text = expense.category
                        tvDetailDate.text = expense.date
                        tvDetailNote.text = expense.note.ifEmpty { "No note" }
                        
                        if (!expense.receiptUri.isNullOrEmpty()) {
                            ivDetailReceipt.setImageURI(Uri.parse(expense.receiptUri))
                            layoutReceipt.visibility = View.VISIBLE
                        } else {
                            layoutReceipt.visibility = View.GONE
                        }
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
        lifecycleScope.launch(Dispatchers.IO) {
            currentExpense?.let {
                database.expenseDao().deleteExpense(it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExpenseDetailActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun shareExpense() {
        currentExpense?.let { expense ->
            val shareText = """
                ExpenseTrack
                
                Expense Details
                
                Amount: ${formatCurrency(expense.amount)}
                Category: ${expense.category}
                Date: ${expense.date}
                Note: ${expense.note.ifEmpty { "None" }}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Expense Detail")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }
}
