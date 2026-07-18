package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FinanceViewModel
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    Dashboard,
    Transactions,
    Budget,
    AiAssistant
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FinanceViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
                var triggerAddDialogInTransactions by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == Screen.Dashboard,
                                onClick = { currentScreen = Screen.Dashboard },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Trang chủ") },
                                label = { Text("Tổng quan") },
                                modifier = Modifier.testTag("nav_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.Transactions,
                                onClick = { currentScreen = Screen.Transactions },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Giao dịch") },
                                label = { Text("Thu chi") },
                                modifier = Modifier.testTag("nav_transactions")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.Budget,
                                onClick = { currentScreen = Screen.Budget },
                                icon = { Icon(Icons.Default.PieChart, contentDescription = "Hạn mức") },
                                label = { Text("Hạn mức") },
                                modifier = Modifier.testTag("nav_budget")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.AiAssistant,
                                onClick = { currentScreen = Screen.AiAssistant },
                                icon = { 
                                    Icon(
                                        Icons.Default.AutoAwesome, 
                                        contentDescription = "Trợ lý AI",
                                        tint = if (currentScreen == Screen.AiAssistant) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    ) 
                                },
                                label = { Text("Trợ lý AI") },
                                modifier = Modifier.testTag("nav_ai")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        Screen.Dashboard -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onAddTransactionClick = {
                                    triggerAddDialogInTransactions = true
                                    currentScreen = Screen.Transactions
                                },
                                onViewAllTransactionsClick = {
                                    currentScreen = Screen.Transactions
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        Screen.Transactions -> {
                            TransactionsScreen(
                                viewModel = viewModel,
                                showAddDialogInitially = triggerAddDialogInTransactions,
                                onDialogClosed = {
                                    triggerAddDialogInTransactions = false
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        Screen.Budget -> {
                            BudgetScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        Screen.AiAssistant -> {
                            AiAssistantScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

