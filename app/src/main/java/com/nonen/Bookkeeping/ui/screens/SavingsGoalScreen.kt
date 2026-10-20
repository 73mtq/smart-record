package com.nonen.Bookkeeping.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import com.nonen.Bookkeeping.ui.components.YimuTopAppBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.statusBarsPadding
import com.nonen.Bookkeeping.ui.components.YimuDreamNativeView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.data.repo.SavingsGoalDetail
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalScreen(
    viewModel: SavingsGoalViewModel,
    onBack: () -> Unit,
) {
    val goals by viewModel.goals.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var actionGoalId by remember { mutableStateOf<Long?>(null) }
    var isDepositAction by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val back = onBack
    AndroidView(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        factory = { YimuDreamNativeView(it) },
        update = { view ->
            view.bind(object : YimuDreamNativeView.Listener {
                override fun onBack() = back()
                override fun onAdd() { showCreateDialog = true }
            })
        },
    )

    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, target, initial, days, remark ->
                viewModel.createGoal(name, target, initial, days, remark)
                showCreateDialog = false
            },
        )
    }

    if (actionGoalId != null) {
        DepositWithdrawDialog(
            isDeposit = isDepositAction,
            onDismiss = { actionGoalId = null },
            onConfirm = { amount, remark ->
                val id = actionGoalId ?: return@DepositWithdrawDialog
                if (isDepositAction) {
                    viewModel.deposit(id, amount, remark)
                } else {
                    viewModel.withdraw(id, amount, remark)
                }
                actionGoalId = null
            },
        )
    }
}

@Composable
fun GoalCardItem(
    goal: SavingsGoalDetail,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.dream.dreamName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (goal.isCompleted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已达成",
                            tint = Color(0xFF48AB93),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除目标", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { goal.progressRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF48AB93),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = String.format(Locale.CHINA, "已存 ¥%.2f / ¥%.2f", goal.currentSavings, goal.dream.totalNum),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = String.format(Locale.CHINA, "%.1f%%", goal.progressRatio * 100),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF48AB93),
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (goal.remainingDays > 0) "剩 ${goal.remainingDays} 天" else "已到期",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                if (goal.dailyRecommended > 0) {
                    Text(
                        text = String.format(Locale.CHINA, "建议每天存: ¥%.2f", goal.dailyRecommended),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onWithdraw, modifier = Modifier.height(36.dp)) {
                    Text("取出", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDeposit, modifier = Modifier.height(36.dp)) {
                    Text("存入", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: Double, initial: Double, days: Int, remark: String?) -> Unit,
) {
    var nameText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf("30") }
    var remarkText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建储蓄愿望") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("愿望名称") },
                    singleLine = true,
                    placeholder = { Text("例如 旅游基金 / 换新电脑") },
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("目标金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("例如 5000.00") },
                )
                OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it },
                    label = { Text("初始已存资金（可选）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("计划完成天数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    val initial = initialText.toDoubleOrNull() ?: 0.0
                    val days = daysText.toIntOrNull() ?: 30
                    val name = nameText.ifBlank { "储蓄目标" }
                    onConfirm(name, target, initial, days, remarkText)
                },
            ) {
                Text("创建")
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
fun DepositWithdrawDialog(
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, remark: String?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var remarkText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isDeposit) "存入愿望资金" else "取出愿望资金") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = remarkText,
                    onValueChange = { remarkText = it },
                    label = { Text("备注说明") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onConfirm(amount, remarkText.ifBlank { null })
                },
            ) {
                Text(if (isDeposit) "存入" else "取出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
