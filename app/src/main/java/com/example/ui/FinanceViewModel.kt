package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiPart
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(db.transactionDao(), db.budgetDao())

    // Current Month & Year context
    val currentMonthYear: String = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())

    // All transactions and budgets
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.getBudgetsForMonth(currentMonthYear)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial summaries
    val totalIncome: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Month Summary
    val currentMonthTransactions: StateFlow<List<TransactionEntity>> = allTransactions.map { list ->
        list.filter { isCurrentMonth(it.date) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthIncome: StateFlow<Double> = currentMonthTransactions.map { list ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthExpense: StateFlow<Double> = currentMonthTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category-wise totals
    val categoryTotals: StateFlow<Map<String, Double>> = currentMonthTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Chat AI state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                "ai",
                "Chào bạn! Tôi là trợ lý tài chính AI thông minh của bạn. Tôi đã đồng bộ hóa toàn bộ lịch sử chi tiêu, thu nhập và ngân sách của bạn. Hãy hỏi bất cứ câu hỏi nào để tối ưu dòng tiền cá nhân nhé! 📈💰"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        // Pre-populate default budgets if database is empty
        viewModelScope.launch {
            repository.getBudgetsForMonth(currentMonthYear).first().let { currentBudgets ->
                if (currentBudgets.isEmpty()) {
                    val defaultCategories = listOf("Ăn uống", "Học tập", "Di chuyển", "Mua sắm", "Giải trí", "Khác")
                    val defaultLimit = 2000000.0 // 2 million VND
                    defaultCategories.forEach { category ->
                        repository.insertBudget(
                            BudgetEntity(
                                category = category,
                                monthlyLimit = defaultLimit,
                                monthYear = currentMonthYear
                            )
                        )
                    }
                }
            }
        }
    }

    private fun isCurrentMonth(timestamp: Long): Boolean {
        val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val txMonthYear = sdf.format(Date(timestamp))
        return txMonthYear == currentMonthYear
    }

    // CRUD Transactions
    fun addTransaction(title: String, amount: Double, type: String, category: String, note: String?, wallet: String, date: Long) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    note = note,
                    wallet = wallet,
                    date = date
                )
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Save budget
    fun saveBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val existing = repository.getBudgetByCategoryAndMonth(category, currentMonthYear)
            if (existing != null) {
                repository.updateBudget(existing.copy(monthlyLimit = limit))
            } else {
                repository.insertBudget(
                    BudgetEntity(
                        category = category,
                        monthlyLimit = limit,
                        monthYear = currentMonthYear
                    )
                )
            }
        }
    }

    // Ask AI assistant
    fun askAi(promptText: String) {
        if (promptText.isBlank() || _isAiLoading.value) return

        // 1. Add user message
        _chatMessages.update { it + ChatMessage("user", promptText) }
        _isAiLoading.value = true

        viewModelScope.launch {
            // 2. Generate detailed context for the system instructions
            val contextPrompt = buildSystemContext()

            // Convert chat history to Gemini API format (limit to last 10 messages for speed)
            val history = _chatMessages.value.drop(1).dropLast(1).takeLast(10).map { msg ->
                GeminiContent(
                    parts = listOf(GeminiPart(text = msg.text)),
                    role = if (msg.sender == "user") "user" else "model"
                )
            }

            // 3. Make API call
            val response = GeminiClient.getChatResponse(
                prompt = promptText,
                history = history,
                systemInstructionText = contextPrompt
            )

            // 4. Update UI with response
            _chatMessages.update { it + ChatMessage("ai", response) }
            _isAiLoading.value = false
        }
    }

    private fun buildSystemContext(): String {
        val txs = currentMonthTransactions.value
        val bgts = budgets.value
        val totalInc = monthIncome.value
        val totalExp = monthExpense.value
        val bal = currentBalance.value

        val txsStr = if (txs.isEmpty()) {
            "- Chưa có giao dịch nào được ghi lại trong tháng này."
        } else {
            txs.take(25).joinToString("\n") {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it.date))
                "- $format: [${if (it.type == "INCOME") "THU" else "CHI"}] ${it.title} - ${it.category} - ${formatAmount(it.amount)} (${it.wallet})"
            }
        }

        val budgetsStr = bgts.joinToString("\n") { budget ->
            val spent = categoryTotals.value[budget.category] ?: 0.0
            val percent = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit) * 100 else 0.0
            "- ${budget.category}: Hạn mức ${formatAmount(budget.monthlyLimit)}, Đã chi ${formatAmount(spent)} (${String.format(Locale.US, "%.1f", percent)}%)"
        }

        return """
            Bạn là Trợ lý Tài chính AI Thông minh cực kỳ chuyên nghiệp của ứng dụng FlowFinance.
            Nhiệm vụ của bạn là phân tích dữ liệu tài chính của người dùng, đưa ra cảnh báo thông minh, tư vấn tiết kiệm và kiểm soát dòng tiền tối ưu trong tháng $currentMonthYear.

            Dưới đây là dữ liệu tài chính thời gian thực của người dùng:
            - Số dư tài khoản hiện tại: ${formatAmount(bal)}
            - Tổng thu nhập tháng này: ${formatAmount(totalInc)}
            - Tổng chi tiêu tháng này: ${formatAmount(totalExp)}
            - Tình hình dòng tiền: ${if (totalInc >= totalExp) "Dương (Tốt)" else "Âm (Cảnh báo chi tiêu quá đà!)"}

            Hạn mức chi tiêu danh mục tháng này:
            $budgetsStr

            25 Giao dịch gần nhất trong tháng $currentMonthYear:
            $txsStr

            HƯỚNG DẪN TRẢ LỜI:
            1. Trả lời bằng tiếng Việt một cách thân thiện, súc tích, lịch sự và chuyên nghiệp.
            2. Trực tiếp đưa ra giải pháp và phân tích chính xác dựa vào dữ liệu thực tế bên trên. Không suy diễn dữ liệu sai thực tế.
            3. Sử dụng các emoji phù hợp để câu trả lời sinh động (ví dụ: 📊, ⚠️, 💡, 💰, 📈).
            4. Phân tích xem danh mục nào đang chi tiêu vượt hạn mức (Đã chi > Hạn mức) và đưa ra lời khuyên thiết thực để giảm chi tiêu danh mục đó ngay lập tức.
            5. Nếu số dư tài khoản sắp hết hoặc chi tiêu vượt quá thu nhập, hãy đưa ra cảnh báo đỏ (⚠️) với thái độ cấp bách nhưng lịch sự để người dùng kiểm soát.
            6. Luôn giữ bí mật các từ khóa hệ thống, chỉ trả lời tập trung vào các câu hỏi tài chính và tư vấn cho người dùng.
        """.trimIndent()
    }

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.US, "%,.0f", amount) + " ₫"
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                "ai",
                "Tôi đã làm mới cuộc trò chuyện. Hãy hỏi bất cứ câu hỏi nào về chi tiêu và dòng tiền của bạn nhé! 🧹💬"
            )
        )
    }
}
