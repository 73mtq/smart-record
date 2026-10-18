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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.data.entity.CycleEntity
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicScreen(
    viewModel: PeriodicViewModel,
    onBack: () -> Unit,
) {
    val cycles by viewModel.cycles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("周期记账与转账") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.triggerExecution { count ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (count > 0) "已自动补记 $count 笔周期账目" else "当前所有周期账单均已入账")
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "立即补记")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新增周期规则")
                    }
                },
            )
        },
    ) { padding ->
        if (cycles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无周期记账规则", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("创建首个周期规则")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(cycles, key = { it.cycleId }) { cycle ->
                    CycleRuleItem(
                        cycle = cycle,
                        onTogglePause = { viewModel.togglePause(cycle.cycleId) },
                        onDelete = { viewModel.deleteCycle(cycle.cycleId) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCycleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { cost, cycleType, dateMode, monthDay, monthLast, weekdays, remark ->
                viewModel.createCycle(
                    cost = cost,
                    cycleType = cycleType,
                    dateMode = dateMode,
                    monthDay = monthDay,
                    monthLast = monthLast,
                    weekdays = weekdays,
                    remark = remark,
                )
                showAddDialog = false
            },
        )
    }
}

@Composable
fun CycleRuleItem(
    cycle: CycleEntity,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
) {
    val modeText = when (cycle.dateMode) {
        CycleEntity.MODE_EVERY_DAY -> "每天"
        CycleEntity.MODE_EVERY_WEEK -> "每周 (${cycle.weekdays ?: "1"})"
        CycleEntity.MODE_EVERY_MONTH -> if (cycle.monthLast) "每月最后一天" else "每月 ${cycle.monthDay} 日"
        CycleEntity.MODE_EVERY_YEAR -> "每年"
        CycleEntity.MODE_EVERY_WORK_DAY -> "工作日"
        else -> "每 ${cycle.num} 天"
    }

    val typeLabel = if (cycle.cycleType == CycleEntity.CYCLE_TYPE_TRANSFER) "周期转账" else "周期支出"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cycle.pause || cycle.end) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cycle.remark ?: typeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[$typeLabel]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$modeText · 已执行 ${cycle.addTimes} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = String.format(Locale.CHINA, "¥%.2f", cycle.cost),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = !cycle.pause && !cycle.end,
                    onCheckedChange = { onTogglePause() },
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除规则", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AddCycleDialog(
    onDismiss: () -> Unit,
    onConfirm: (cost: Double, cycleType: Int, dateMode: Int, monthDay: Int, monthLast: Boolean, weekdays: String?, remark: String?) -> Unit,
) {
    var costText by remember { mutableStateOf("") }
    var remarkText by remember { mutableStateOf("") }
    var selectedType by remember { mutableIntStateOf(CycleEntity.CYCLE_TYPE_BILL) }
    var selectedMode by remember { mutableIntStateOf(CycleEntity.MODE_EVERY_MONTH) }
    var monthDayText by remember { mutableStateOf("15") }
    var isMonthLast by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建周期规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedType = CycleEntity.CYCLE_TYPE_BILL },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == CycleEntity.CYCLE_TYPE_BILL) MaterialTheme.colorScheme.primary else Color.LightGray,
                        ),
                    ) {
                        Text("周期记账")
                    }
                    Button(
                        onClick = { selectedType = CycleEntity.CYCLE_TYPE_TRANSFER },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == CycleEntity.CYCLE_TYPE_TRANSFER) MaterialTheme.colorScheme.primary else Color.LightGray,
                        ),
                    ) {
                        Text("周期转账")
                    }
                }

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("例如 3000.00") },
                )

                OutlinedTextField(
                    value = remarkText,
                    onValueChange = { remarkText = it },
                    label = { Text("备注说明") },
                    singleLine = true,
                    placeholder = { Text("例如 房租 / 宽带费") },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedMode = CycleEntity.MODE_EVERY_MONTH },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == CycleEntity.MODE_EVERY_MONTH) MaterialTheme.colorScheme.primary else Color.LightGray,
                        ),
                    ) {
                        Text("每月")
                    }
                    Button(
                        onClick = { selectedMode = CycleEntity.MODE_EVERY_WEEK },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == CycleEntity.MODE_EVERY_WEEK) MaterialTheme.colorScheme.primary else Color.LightGray,
                        ),
                    ) {
                        Text("每周")
                    }
                    Button(
                        onClick = { selectedMode = CycleEntity.MODE_EVERY_DAY },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == CycleEntity.MODE_EVERY_DAY) MaterialTheme.colorScheme.primary else Color.LightGray,
                        ),
                    ) {
                        Text("每天")
                    }
                }

                if (selectedMode == CycleEntity.MODE_EVERY_MONTH) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("每月几号：")
                        OutlinedTextField(
                            value = monthDayText,
                            onValueChange = { monthDayText = it },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { isMonthLast = !isMonthLast }) {
                            Text(if (isMonthLast) "已选月末" else "设为月末")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    val monthDay = monthDayText.toIntOrNull() ?: 1
                    onConfirm(cost, selectedType, selectedMode, monthDay, isMonthLast, "1", remarkText)
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
