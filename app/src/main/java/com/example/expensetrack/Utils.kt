package com.example.expensetrack

import java.text.NumberFormat
import java.util.Locale

object Utils {
    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }
}
