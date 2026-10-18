package com.nonen.Bookkeeping.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.ui.components.AnimatedSegmented
import com.nonen.Bookkeeping.ui.theme.ExpenseColor
import com.nonen.Bookkeeping.ui.theme.IncomeColor

@Composable
fun CategoryManageScreen(
    vm: CategoryManageViewModel,
    onBack: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var targetParentIdForAdd by remember { mutableStateOf(0) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var usageCountToDelete by remember { mutableStateOf(0) }

    val parents = remember(vm.categories) {
        vm.categories.filter { it.parentId == 0 }
    }
    val childrenMap = remember(vm.categories) {
        vm.categories.filter { it.parentId != 0 }.groupBy { it.parentId }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            CategoryManageHeader(
                selectedType = vm.selectedType,
                onBack = onBack,
                onTypeChanged = vm::setType,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    targetParentIdForAdd = if (vm.selectedType == 1) {
                        parents.firstOrNull()?.categoryId ?: 9
                    } else {
                        0
                    }
                    showAddDialog = true
                },
                containerColor = if (vm.selectedType == 1) IncomeColor else ExpenseColor,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增分类")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            items(parents, key = { it.id }) { parent ->
                val children = childrenMap[parent.categoryId] ?: emptyList()
                val isExpanded = parent.categoryId in vm.expandedParentIds

                ParentCategoryCard(
                    category = parent,
                    isExpanded = isExpanded,
                    childrenCount = children.size,
                    onToggleExpand = { vm.toggleExpand(parent.categoryId) },
                    onToggleHide = { vm.toggleHide(parent) },
                    onMoveUp = { vm.moveUp(parent, parents) },
                    onMoveDown = { vm.moveDown(parent, parents) },
                    onEdit = { categoryToEdit = parent },
                    onDelete = {
                        vm.checkUsage(parent.categoryId) { count ->
                            usageCountToDelete = count
                            categoryToDelete = parent
                        }
                    },
                    onAddChild = {
                        targetParentIdForAdd = parent.categoryId
                        showAddDialog = true
                    },
                )

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 6.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        children.forEach { child ->
                            ChildCategoryRow(
                                category = child,
                                onToggleHide = { vm.toggleHide(child) },
                                onMoveUp = { vm.moveUp(child, children) },
                                onMoveDown = { vm.moveDown(child, children) },
                                onEdit = { categoryToEdit = child },
                                onDelete = {
                                    vm.checkUsage(child.categoryId) { count ->
                                        usageCountToDelete = count
                                        categoryToDelete = child
                                    }
                                },
                            )
                        }

                        // 二级新增入口
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    targetParentIdForAdd = parent.categoryId
                                    showAddDialog = true
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "添加【${parent.categoryName}】二级子分类",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddEditCategoryDialog(
            title = if (targetParentIdForAdd == 0) "新增一级分类" else "新增二级分类",
            initialName = "",
            initialIcon = "ic_parent_other",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, icon ->
                vm.createCategory(name, targetParentIdForAdd, icon) {
                    showAddDialog = false
                }
            },
        )
    }

    categoryToEdit?.let { cat ->
        AddEditCategoryDialog(
            title = "编辑分类【${cat.categoryName}】",
            initialName = cat.categoryName,
            initialIcon = cat.iconUrl,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, icon ->
                vm.updateCategory(cat, name, icon) {
                    categoryToEdit = null
                }
            },
        )
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("确认删除分类") },
            text = {
                Column {
                    Text("您确定要删除自定义分类【${cat.categoryName}】吗？")
                    if (usageCountToDelete > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "⚠️ 检测到有 $usageCountToDelete 笔历史账单引用了此分类。建议将其设为隐藏。若直接删除，相关账单将保留文字说明并解除分类关联。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteCategory(cat, unlinkTransactions = true) {
                            categoryToDelete = null
                        }
                    },
                ) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun CategoryManageHeader(
    selectedType: Int,
    onBack: () -> Unit,
    onTypeChanged: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "分类管理",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        AnimatedSegmented(
            options = listOf("支出分类", "收入分类"),
            selectedIndex = selectedType,
            onSelected = onTypeChanged,
            thumbColor = if (selectedType == 1) IncomeColor else ExpenseColor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ParentCategoryCard(
    category: CategoryEntity,
    isExpanded: Boolean,
    childrenCount: Int,
    onToggleExpand: () -> Unit,
    onToggleHide: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddChild: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = Categories.emoji(category.categoryName), fontSize = 18.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (category.isCustom) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "自定义",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    if (category.hide) {
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
                }
                Text(
                    text = "$childrenCount 个子分类",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(18.dp))
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "操作", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(if (category.hide) "设为显示" else "设为隐藏") },
                        onClick = {
                            showMenu = false
                            onToggleHide()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("编辑名称与图标") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("添加子分类") },
                        onClick = {
                            showMenu = false
                            onAddChild()
                        },
                    )
                    if (category.isCustom) {
                        DropdownMenuItem(
                            text = { Text("删除分类", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
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
private fun ChildCategoryRow(
    category: CategoryEntity,
    onToggleHide: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = Categories.emoji(category.categoryName), fontSize = 14.sp)
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (category.isCustom) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "自定义",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 3.dp, vertical = 1.dp),
                        )
                    }
                    if (category.hide) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "隐藏",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 3.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(16.dp))
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "操作", modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(if (category.hide) "设为显示" else "设为隐藏") },
                        onClick = {
                            showMenu = false
                            onToggleHide()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    )
                    if (category.isCustom) {
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
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
private fun AddEditCategoryDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("图标预览：", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = Categories.emoji(name.ifEmpty { "其他" }), fontSize = 22.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedIcon)
                    }
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
