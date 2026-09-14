package com.example.expensetrack

import android.app.DatePickerDialog
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.database.Expense
import com.example.expensetrack.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var database: AppDatabase
    private var expenseId: Int = -1
    private var isEditMode = false
    private var selectedDate = ""
    private var receiptUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && receiptUri != null) {
            binding.ivReceipt.setImageURI(receiptUri)
            binding.ivReceipt.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        expenseId = intent.getIntExtra("EXPENSE_ID", -1)
        isEditMode = expenseId != -1

        setupUI()
        if (isEditMode) loadExpense()
        binding.tvBack.setOnClickListener { finish() }
    }

    private fun setupUI() {
        val categories = resources.getStringArray(R.array.categories)
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        if (!isEditMode) {
            selectedDate = formatter.format(Calendar.getInstance().time)
            binding.tvDate.text = selectedDate
        }

        binding.tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = formatter.format(cal.time)
                binding.tvDate.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnCaptureReceipt.setOnClickListener {
            val cv = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "receipt_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ExpenseTrack")
            }
            receiptUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
            receiptUri?.let { takePictureLauncher.launch(it) }
        }

        binding.btnSaveExpense.setOnClickListener { saveExpense() }
    }

    private fun loadExpense() {
        lifecycleScope.launch {
            val exp = database.expenseDao().getExpenseById(expenseId)
            runOnUiThread {
                exp?.let {
                    binding.apply {
                        etAmount.setText(it.amount.toString())
                        etNote.setText(it.note)
                        selectedDate = it.date
                        tvDate.text = selectedDate
                        val idx = resources.getStringArray(R.array.categories).indexOf(it.category)
                        if (idx != -1) spinnerCategory.setSelection(idx)
                        if (it.receiptUri != null) {
                            receiptUri = Uri.parse(it.receiptUri)
                            ivReceipt.setImageURI(receiptUri)
                            ivReceipt.visibility = View.VISIBLE
                        }
                        btnSaveExpense.text = "Update Expense"
                    }
                }
            }
        }
    }

    private fun saveExpense() {
        val amtText = binding.etAmount.text.toString().trim()
        val amt = amtText.toDoubleOrNull() ?: 0.0
        if (amt <= 0) { binding.etAmount.error = "Invalid amount"; return }

        val expense = Expense(if (isEditMode) expenseId else 0, amt, binding.spinnerCategory.selectedItem.toString(), selectedDate, binding.etNote.text.toString().trim(), receiptUri?.toString())

        lifecycleScope.launch {
            if (isEditMode) database.expenseDao().updateExpense(expense) else database.expenseDao().insertExpense(expense)
            runOnUiThread {
                Toast.makeText(this@AddExpenseActivity, "Success", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
