package com.example.data.repository

import com.example.data.local.TransactionDao
import com.example.data.local.BudgetDao
import com.example.data.model.TransactionEntity
import com.example.data.model.BudgetEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsInRange(startTime, endTime)

    suspend fun insertTransaction(transaction: TransactionEntity) =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Int) =
        transactionDao.deleteById(id)

    fun getTotalAmountByType(type: String): Flow<Double?> =
        transactionDao.getTotalAmountByType(type)

    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(monthYear)

    suspend fun insertBudget(budget: BudgetEntity) =
        budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) =
        budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) =
        budgetDao.deleteBudget(budget)

    suspend fun getBudgetByCategoryAndMonth(category: String, monthYear: String): BudgetEntity? =
        budgetDao.getBudgetByCategoryAndMonth(category, monthYear)
}
