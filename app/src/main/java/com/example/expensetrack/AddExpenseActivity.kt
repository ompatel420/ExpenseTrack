package com.example.expensetrack

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.database.Expense
import com.example.expensetrack.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val PREFS_NAME = "ExpenseTrackPrefs"
    private val CHANNEL_ID = "expense_alerts"

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && receiptUri != null) {
            binding.ivReceipt.setImageURI(receiptUri)
            binding.ivReceipt.visibility = View.VISIBLE
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            receiptUri = it
            binding.ivReceipt.setImageURI(it)
            binding.ivReceipt.visibility = View.VISIBLE
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            captureReceipt()
        } else {
            Toast.makeText(this, "Camera permission required to capture receipt", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is recommended for limit alerts", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        expenseId = intent.getIntExtra("EXPENSE_ID", -1)
        isEditMode = expenseId != -1

        createNotificationChannel()
        requestNotificationPermission()
        setupUI()
        if (isEditMode) loadExpense()
        binding.tvBack.setOnClickListener { finish() }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Expense Alerts"
            val descriptionText = "Notifications for when you exceed your spending limits"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                captureReceipt()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnUploadReceipt.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveExpense.setOnClickListener { saveExpense() }
    }

    private fun captureReceipt() {
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "receipt_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ExpenseTrack")
        }
        receiptUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
        receiptUri?.let { takePictureLauncher.launch(it) }
    }

    private fun loadExpense() {
        lifecycleScope.launch(Dispatchers.IO) {
            val exp = database.expenseDao().getExpenseById(expenseId)
            withContext(Dispatchers.Main) {
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

        val category = binding.spinnerCategory.selectedItem.toString()
        val expense = Expense(if (isEditMode) expenseId else 0, amt, category, selectedDate, binding.etNote.text.toString().trim(), receiptUri?.toString())

        lifecycleScope.launch(Dispatchers.IO) {
            // Check limits before saving
            checkLimitsAndNotify(category, amt)

            if (isEditMode) database.expenseDao().updateExpense(expense) else database.expenseDao().insertExpense(expense)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddExpenseActivity, "Success", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private suspend fun checkLimitsAndNotify(category: String, newAmount: Double) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val monthFilter = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        val expenses = database.expenseDao().getAllExpenses().filter { it.date.contains(monthFilter) }
        
        // Total Limit check
        val totalSpent = expenses.sumOf { it.amount }
        val totalLimit = prefs.getFloat("monthly_limit", 0f)
        if (totalLimit > 0 && totalSpent + newAmount > totalLimit) {
            showLimitNotification("Monthly Limit Exceeded", "You have spent ${Utils.formatCurrency(totalSpent + newAmount)} which is above your limit of ${Utils.formatCurrency(totalLimit.toDouble())}")
        }

        // Category Limit check
        val catSpent = expenses.filter { it.category == category }.sumOf { it.amount }
        val catLimit = prefs.getFloat("limit_$category", 0f)
        if (catLimit > 0 && catSpent + newAmount > catLimit) {
            showLimitNotification("$category Limit Exceeded", "Spent ${Utils.formatCurrency(catSpent + newAmount)} on $category, limit was ${Utils.formatCurrency(catLimit.toDouble())}")
        }
    }

    private fun showLimitNotification(title: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }
}
