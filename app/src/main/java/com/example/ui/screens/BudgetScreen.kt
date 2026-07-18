package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEntity
import com.example.ui.FinanceViewModel
import java.util.Locale

@Composable
fun BudgetScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.budgets.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var currentLimitValue by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Screen Header
        Text(
            text = "Hạn mức Chi tiêu",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text = "Kiểm soát chi tiêu từng danh mục trong tháng ${viewModel.currentMonthYear}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // General Status Card
        val totalSpent = categoryTotals.values.sum()
        val totalLimit = budgets.sumOf { it.monthlyLimit }
        val overallProgress = if (totalLimit > 0) (totalSpent / totalLimit).toFloat() else 0f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "TỔNG QUAN HẠN MỨC THÁNG",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Đã chi: ${formatVnd(totalSpent)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Tổng hạn mức: ${formatVnd(totalLimit)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.1f", overallProgress * 100)}%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (overallProgress >= 1f) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Linear Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(overallProgress.coerceAtMost(1f))
                            .clip(CircleShape)
                            .background(if (overallProgress >= 1f) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Categories budgets list
        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(budgets) { budget ->
                    val spent = categoryTotals[budget.category] ?: 0.0
                    BudgetRow(
                        budget = budget,
                        spent = spent,
                        onEditClick = {
                            selectedCategory = budget.category
                            currentLimitValue = budget.monthlyLimit.toInt().toString()
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    // Edit Budget Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = currentLimitValue.toDoubleOrNull() ?: 0.0
                        if (limit >= 0) {
                            viewModel.saveBudget(selectedCategory, limit)
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Lưu lại")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Huỷ")
                }
            },
            title = {
                Text("Cập nhật hạn mức", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Thiết lập hạn mức chi tiêu hàng tháng cho danh mục:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = selectedCategory,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    OutlinedTextField(
                        value = currentLimitValue,
                        onValueChange = { currentLimitValue = it },
                        label = { Text("Hạn mức (đ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        )
    }
}

@Composable
fun BudgetRow(
    budget: BudgetEntity,
    spent: Double,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
    val progressAnimated by animateFloatAsState(targetValue = progress.coerceAtMost(1f), label = "progress")

    // Dynamic color depending on spending range
    val color = when {
        progress >= 1f -> Color(0xFFEF4444) // Coral Red (Exceeded)
        progress >= 0.7f -> Color(0xFFF59E0B) // Warning Orange
        else -> Color(0xFF10B981) // Safe Green
    }

    val animatedColor by animateColorAsState(targetValue = color, label = "color")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(animatedColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(budget.category),
                            contentDescription = budget.category,
                            tint = animatedColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = budget.category,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit budget",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đã dùng: ${formatVnd(spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Hạn mức: ${formatVnd(budget.monthlyLimit)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnimated)
                        .clip(CircleShape)
                        .background(animatedColor)
                )
            }

            if (progress >= 1f) {
                Text(
                    text = "⚠️ Vượt hạn mức chi tiêu ${formatVnd(spent - budget.monthlyLimit)}!",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
