package com.example.expensetrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrack.R
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
                
                val iconRes = when (expense.category) {
                    "Food" -> R.drawable.ic_food
                    "Travel" -> R.drawable.ic_travel
                    "Shopping" -> R.drawable.ic_shopping
                    "Bills" -> R.drawable.ic_bills
                    "Entertainment" -> R.drawable.ic_entertainment
                    else -> R.drawable.ic_other
                }
                ivCategoryIcon.setImageResource(iconRes)

                ivReceipt.visibility = if (expense.receiptUri != null) View.VISIBLE else View.GONE
                
                val clickListener = View.OnClickListener { onExpenseClick(expense) }
                root.setOnClickListener(clickListener)
                layoutContent.setOnClickListener(clickListener)
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
