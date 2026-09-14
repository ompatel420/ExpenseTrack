package com.example.expensetrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrack.Utils
import com.example.expensetrack.database.Expense
import com.example.expensetrack.databinding.ItemExpenseBinding

class ExpenseAdapter(
    private var expenseList: List<Expense>,
    private val onExpenseClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense) {
            binding.apply {
                tvCategory.text = expense.category
                tvAmount.text = Utils.formatCurrency(expense.amount)
                tvDate.text = expense.date
                tvNote.text = expense.note.ifEmpty { "No note" }
                tvCategoryIcon.text = when (expense.category) {
                    "Food" -> "🍔"
                    "Travel" -> "🚕"
                    "Shopping" -> "🛍"
                    "Bills" -> "💡"
                    "Entertainment" -> "🎬"
                    else -> "📦"
                }
                ivReceipt.visibility = if (expense.receiptUri != null) View.VISIBLE else View.GONE
                root.setOnClickListener { onExpenseClick(expense) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder =
        ExpenseViewHolder(ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) = holder.bind(expenseList[position])

    override fun getItemCount(): Int = expenseList.size

    fun updateList(newList: List<Expense>) {
        expenseList = newList
        notifyDataSetChanged()
    }
}
