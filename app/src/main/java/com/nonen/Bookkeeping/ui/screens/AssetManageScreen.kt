package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.ui.theme.ExpenseColor
import com.nonen.Bookkeeping.ui.theme.IncomeColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetManageScreen(
    vm: AssetManageViewModel,
    onBack: () -> Unit,
) {
    val displayAssets by vm.displayAssets.collectAsState()
    val summary by vm.summary.collectAsState()
    val showHidden by vm.showHidden.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资产与账户管理", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = vm::openAddDialog) {
                        Icon(Icons.Default.Add, contentDescription = "添加账户")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. 顶部总资产/净资产概览卡片
            item(key = "summary_card") {
                AssetSummaryCard(
                    netAsset = summary.netAsset,
                    totalAsset = summary.totalAsset,
                    totalLiability = summary.totalLiability,
                )
            }

            // 2. 工具栏：显示隐藏账户开关与快捷操作
            item(key = "toolbar_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = showHidden,
                            onCheckedChange = vm::setShowHidden,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = "显示隐藏账户", style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = vm::openAddDialog,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新增账户", fontSize = 12.sp)
                    }
                }
            }

            // 3. 分组资产卡片列表
            val fundAssets = displayAssets.filter { it.assetType == AssetRepository.TYPE_FUND }
            val creditAssets = displayAssets.filter { it.assetType == AssetRepository.TYPE_CREDIT }
            val lendAssets = displayAssets.filter { it.assetType == AssetRepository.TYPE_CLAIM || it.assetType == AssetRepository.TYPE_DEBT }

            if (fundAssets.isNotEmpty()) {
                item(key = "group_header_fund") {
                    GroupHeaderTitle(title = "资金账户", count = fundAssets.size)
                }
                items(fundAssets, key = { "asset_${it.assetId}" }) { asset ->
                    AssetItemRow(
                        asset = asset,
                        onAdjust = { vm.openAdjustDialog(asset) },
                        onEdit = { vm.openEditDialog(asset) },
                        onToggleHide = { vm.toggleHide(asset) },
                        onDelete = { vm.openDeleteDialog(asset) },
                        onMoveUp = { vm.moveUp(asset.assetId) },
                        onMoveDown = { vm.moveDown(asset.assetId) },
                    )
                }
            }

            if (creditAssets.isNotEmpty()) {
                item(key = "group_header_credit") {
                    GroupHeaderTitle(title = "信用卡账户", count = creditAssets.size)
                }
                items(creditAssets, key = { "asset_${it.assetId}" }) { asset ->
                    AssetItemRow(
                        asset = asset,
                        onAdjust = { vm.openAdjustDialog(asset) },
                        onEdit = { vm.openEditDialog(asset) },
                        onToggleHide = { vm.toggleHide(asset) },
                        onDelete = { vm.openDeleteDialog(asset) },
                        onMoveUp = { vm.moveUp(asset.assetId) },
                        onMoveDown = { vm.moveDown(asset.assetId) },
                    )
                }
            }

            if (lendAssets.isNotEmpty()) {
                item(key = "group_header_lend") {
                    GroupHeaderTitle(title = "借贷账户", count = lendAssets.size)
                }
                items(lendAssets, key = { "asset_${it.assetId}" }) { asset ->
                    AssetItemRow(
                        asset = asset,
                        onAdjust = { vm.openAdjustDialog(asset) },
                        onEdit = { vm.openEditDialog(asset) },
                        onToggleHide = { vm.toggleHide(asset) },
                        onDelete = { vm.openDeleteDialog(asset) },
                        onMoveUp = { vm.moveUp(asset.assetId) },
                        onMoveDown = { vm.moveDown(asset.assetId) },
                    )
                }
            }

            if (displayAssets.isEmpty()) {
                item(key = "empty_assets") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无账户，点击右上角添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // 弹窗：新增/编辑账户
    if (vm.showAddEditDialog) {
        AddEditAssetDialog(
            editingAsset = vm.editingAsset,
            onDismiss = vm::closeAddEditDialog,
            onConfirm = vm::saveAsset,
        )
    }

    // 弹窗：余额调平
    vm.adjustingAsset?.let { asset ->
        if (vm.showAdjustDialog) {
            AdjustBalanceDialog(
                asset = asset,
                onDismiss = vm::closeAdjustDialog,
                onConfirm = { target, generateBill, note ->
                    vm.adjustBalance(asset.assetId, target, generateBill, note)
                },
            )
        }
    }

    // 弹窗：删除确认
    vm.deletingAsset?.let { asset ->
        if (vm.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = vm::closeDeleteDialog,
                title = { Text("删除账户确认") },
                text = {
                    Text("确定要删除「${asset.assetName}」吗？关联该账户的历史账单将自动转移到默认现金账户。")
                },
                confirmButton = {
                    TextButton(onClick = vm::deleteAsset) {
                        Text("确认删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = vm::closeDeleteDialog) {
                        Text("取消")
                    }
                },
            )
        }
    }

    // 错误提示弹窗
    vm.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = vm::clearError) { Text("好") }
            },
        )
    }
}

@Composable
private fun GroupHeaderTitle(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun AssetSummaryCard(
    netAsset: Double,
    totalAsset: Double,
    totalLiability: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "净资产 (CNY)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "¥ %,.2f", netAsset),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (netAsset >= 0.0) MaterialTheme.colorScheme.onSurface else ExpenseColor,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("总资产", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = String.format(Locale.US, "¥ %,.2f", totalAsset),
                        style = MaterialTheme.typography.titleMedium,
                        color = IncomeColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("总负债", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = String.format(Locale.US, "¥ %,.2f", totalLiability),
                        style = MaterialTheme.typography.titleMedium,
                        color = ExpenseColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetItemRow(
    asset: AssetEntity,
    onAdjust: () -> Unit,
    onEdit: () -> Unit,
    onToggleHide: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 图标圆圈
            val initialChar = asset.assetName.firstOrNull()?.toString() ?: "资"
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialChar,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = asset.assetName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (asset.hide) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "已隐藏",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    if (!asset.intoTotalAsset) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "不计总资产",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                asset.groupName?.let { g ->
                    Text(text = g, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "¥ %,.2f", asset.assetNumber),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (asset.assetNumber >= 0.0) MaterialTheme.colorScheme.onSurface else ExpenseColor,
                )
                if (asset.assetType == AssetRepository.TYPE_CREDIT && asset.totalQuota > 0.0) {
                    Text(
                        text = String.format(Locale.US, "额度: ¥ %,.0f", asset.totalQuota),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // 快捷操作菜单
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("调平余额") },
                        onClick = {
                            menuExpanded = false
                            onAdjust()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("编辑账户") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (asset.hide) "显示账户" else "隐藏账户") },
                        onClick = {
                            menuExpanded = false
                            onToggleHide()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("上移") },
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("下移") },
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
                        },
                    )
                    if (asset.assetId != AssetRepository.DEFAULT_ASSET_ID) {
                        DropdownMenuItem(
                            text = { Text("删除账户", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditAssetDialog(
    editingAsset: AssetEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, balance: Double, quota: Double, type: Int, groupName: String?, intoTotalAsset: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(editingAsset?.assetName ?: "") }
    var balanceText by remember { mutableStateOf(editingAsset?.let { String.format(Locale.US, "%.2f", it.assetNumber) } ?: "0.00") }
    var quotaText by remember { mutableStateOf(editingAsset?.let { String.format(Locale.US, "%.2f", it.totalQuota) } ?: "0.00") }
    var type by remember { mutableIntStateOf(editingAsset?.assetType ?: AssetRepository.TYPE_FUND) }
    var groupName by remember { mutableStateOf(editingAsset?.groupName ?: "") }
    var intoTotalAsset by remember { mutableStateOf(editingAsset?.intoTotalAsset ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingAsset == null) "新增账户" else "编辑账户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 账户类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        AssetRepository.TYPE_FUND to "资金",
                        AssetRepository.TYPE_CREDIT to "信用卡",
                        AssetRepository.TYPE_CLAIM to "债权",
                        AssetRepository.TYPE_DEBT to "债务",
                    ).forEach { (t, label) ->
                        val selected = type == t
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { type = t }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text(if (type == AssetRepository.TYPE_CREDIT) "当前可用额度/余额" else "当前可用余额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (type == AssetRepository.TYPE_CREDIT) {
                    OutlinedTextField(
                        value = quotaText,
                        onValueChange = { quotaText = it },
                        label = { Text("信用卡总信用额度") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("分组名称（选填，如：银行卡、电子钱包）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { intoTotalAsset = !intoTotalAsset },
                ) {
                    Checkbox(
                        checked = intoTotalAsset,
                        onCheckedChange = { intoTotalAsset = it },
                    )
                    Text("计入净资产总额", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balance = balanceText.toDoubleOrNull() ?: 0.0
                    val quota = quotaText.toDoubleOrNull() ?: 0.0
                    onConfirm(name, balance, quota, type, groupName, intoTotalAsset)
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun AdjustBalanceDialog(
    asset: AssetEntity,
    onDismiss: () -> Unit,
    onConfirm: (targetBalance: Double, generateBill: Boolean, note: String?) -> Unit,
) {
    var targetText by remember { mutableStateOf(String.format(Locale.US, "%.2f", asset.assetNumber)) }
    var generateBill by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    val current = asset.assetNumber
    val target = targetText.toDoubleOrNull() ?: current
    val delta = target - current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("余额调平（${asset.assetName}）") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "系统当前记录余额: ¥ ${String.format(Locale.US, "%.2f", current)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("实际盘点金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                val diffText = if (delta > 0) "盘盈 +¥ ${String.format(Locale.US, "%.2f", delta)}"
                else if (delta < 0) "盘亏 -¥ ${String.format(Locale.US, "%.2f", kotlin.math.abs(delta))}"
                else "差额为 0"

                Text(
                    text = "计算差额: $diffText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (delta >= 0) IncomeColor else ExpenseColor,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { generateBill = !generateBill },
                ) {
                    Checkbox(checked = generateBill, onCheckedChange = { generateBill = it })
                    Text("自动生成调平账单（收支记录）", style = MaterialTheme.typography.bodySmall)
                }

                if (generateBill && delta != 0.0) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("平账备注（选填）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(target, generateBill, note.ifBlank { null })
                },
            ) {
                Text("确认调平")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
