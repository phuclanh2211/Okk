package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE" (Chi) or "INCOME" (Thu)
    val category: String, // e.g. "Ăn uống", "Mua sắm", "Lương"...
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val wallet: String = "Tiền mặt" // e.g. "Tiền mặt", "Ví điện tử", "Thẻ ngân hàng"
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val monthlyLimit: Double,
    val monthYear: String // Format "MM/yyyy" (e.g. "07/2026")
)
