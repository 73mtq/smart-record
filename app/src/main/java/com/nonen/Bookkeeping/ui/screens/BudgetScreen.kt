package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.data.repo.CategoryBudgetDetail
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onBack: () -> Unit,
) {
    val overview by viewModel.budgetOverview.collectAsState()
    val year by viewModel.currentYear.collectAsState()
    val month by viewModel.currentMonth.collectAsState()

    var showTotalDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.prevMonth() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上月")
                        }
                        Text(
                            text = "${year}年${month}月",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下月")
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 总预算卡片
            item {
                TotalBudgetCard(
                    totalBudget = overview.totalBudget,
                    totalExpense = overview.totalExpense,
                    remaining = overview.remainingBudget,
                    usageRatio = overview.usageRatio,
                    isOverBudget = overview.isOverBudget,
                    onEditClick = { showTotalDialog = true },
                )
            }

            // 分类预算标题栏
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "分类预算",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加分类")
                    }
                }
            }

            // 分类预算列表
            if (overview.categoryDetails.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("暂无分类预算，点击右上角添加", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(overview.categoryDetails) { item ->
                    CategoryBudgetItem(
                        item = item,
                        onDelete = { viewModel.deleteCategoryBudget(item.categoryBudgetId) },
                    )
                }
            }
        }
    }

    if (showTotalDialog) {
        SetTotalBudgetDialog(
            currentBudget = overview.totalBudget,
            onDismiss = { showTotalDialog = false },
            onConfirm = { amount ->
                viewModel.setTotalBudget(amount)
                showTotalDialog = false
            },
        )
    }

    if (showCategoryDialog) {
        SetCategoryBudgetDialog(
            onDismiss = { showCategoryDialog = false },
            onConfirm = { parentCatId, amount ->
                viewModel.setCategoryBudget(parentCategoryId = parentCatId, amount = amount)
                showCategoryDialog = false
            },
        )
    }
}

@Composable
fun TotalBudgetCard(
    totalBudget: Double,
    totalExpense: Double,
    remaining: Double,
    usageRatio: Float,
    isOverBudget: Boolean,
    onEditClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isOverBudget) "已超支预算 ⚠️" else "月度剩余预算",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                )
                IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑总预算",
                        tint = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = String.format(Locale.CHINA, "¥%.2f", remaining),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { usageRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = String.format(Locale.CHINA, "总预算: ¥%.2f", totalBudget),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = String.format(Locale.CHINA, "已支出: ¥%.2f (%.1f%%)", totalExpense, usageRatio * 100),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun CategoryBudgetItem(
    item: CategoryBudgetDetail,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.parentCategoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isOverBudget) "超支 ¥${String.format(Locale.CHINA, "%.2f", -item.remainingAmount)}" else "剩余 ¥${String.format(Locale.CHINA, "%.2f", item.remainingAmount)}",
                        color = if (item.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { item.usageRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (item.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = String.format(Locale.CHINA, "已花: ¥%.2f", item.spentAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                Text(
                    text = String.format(Locale.CHINA, "限额: ¥%.2f", item.budgetAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }
    }
}

@Composable
fun SetTotalBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设定月度总预算") },
        text = {
            Column {
                Text("请输入当月支出总预算金额：")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("例如 5000.00") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = text.toDoubleOrNull() ?: 0.0
                    onConfirm(amount)
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun SetCategoryBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Double) -> Unit,
) {
    var categoryIdText by remember { mutableStateOf("7") } // 默认食品餐饮
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加分类预算") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("一级分类 ID（1=健康医疗, 7=食品餐饮, 8=购物消费等）：")
                OutlinedTextField(
                    value = categoryIdText,
                    onValueChange = { categoryIdText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Text("预算限额：")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("例如 1500.00") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val catId = categoryIdText.toIntOrNull() ?: 7
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onConfirm(catId, amount)
                },
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
