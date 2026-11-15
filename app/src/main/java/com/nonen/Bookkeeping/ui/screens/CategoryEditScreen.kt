package com.nonen.Bookkeeping.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.ui.vmFactory
import kotlinx.coroutines.launch

/**
 * 分类新增/编辑页：原版 AddCategoryActivity（activity_add_category.xml）。
 * 名称、图标选择、仅在当前账本显示三个区域；编辑自定义分类时可删除。
 */
@Composable
fun CategoryEditScreen(
    container: AppContainer,
    parentId: Int,
    categoryId: Int,
    categoryType: Int,
    onBack: () -> Unit,
) {
    val vm: CategoryEditViewModel = viewModel(
        key = "category_edit_${parentId}_${categoryId}_$categoryType",
        factory = vmFactory {
            CategoryEditViewModel(
                categoryRepository = container.categoryRepository,
                bookDao = container.bookDao,
                settings = container.settings,
                categoryId = categoryId,
                parentId = parentId,
                categoryType = categoryType,
            )
        },
    )
    val scope = rememberCoroutineScope()
    var showIcons by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val isEditing = categoryId != 0
    val title = when {
        isEditing -> "编辑分类"
        parentId == 0 -> "新增一级分类"
        else -> "新增二级分类"
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Filled.Close, "关闭", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            if (isEditing && vm.category?.isCustom == true) {
                IconButton(onClick = { showDelete = true }, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Filled.Delete, "删除分类", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            IconButton(
                onClick = {
                    if (vm.name.isBlank()) return@IconButton
                    vm.save { onBack() }
                },
                modifier = Modifier.size(50.dp),
            ) {
                Icon(Icons.Filled.Check, "保存", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Column(Modifier.fillMaxWidth().padding(15.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("分类名称", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 15.dp))
                TextField(
                    value = vm.name,
                    onValueChange = { vm.name = it },
                    placeholder = { Text("输入类别名称", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(start = 30.dp),
                )
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(
                Modifier.fillMaxWidth().height(60.dp).clickable { showIcons = true }.padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("分类图标", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("点击可搜索、自定义图标", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                IconPreview(vm.iconUrl, Modifier.size(30.dp))
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(
                Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("仅在当前账本显示", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "在其他账本隐藏此分类",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = vm.hideInOtherBooks, onCheckedChange = { vm.hideInOtherBooks = it })
            }
        }
    }

    if (showIcons) {
        IconPickerDialog(current = vm.iconUrl, onDismiss = { showIcons = false }) { picked ->
            vm.iconUrl = picked
            showIcons = false
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除分类") },
            text = { Text("删除「${vm.category?.categoryName ?: vm.name}」后，关联账单会保留但失去分类归属。") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.delete { onBack() }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun IconPickerDialog(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    val icons = remember {
        context.assets.list("yimu/lineIcon")?.sorted()?.map { "lineIcon/$it" }.orEmpty()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择分类图标") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxWidth().height(360.dp),
            ) {
                items(icons) { icon ->
                    Box(
                        Modifier
                            .size(40.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (icon == current) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { onPick(icon) },
                        contentAlignment = Alignment.Center,
                    ) {
                        IconPreview(icon, Modifier.size(22.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun IconPreview(iconUrl: String, modifier: Modifier) {
    val context = LocalContext.current
    if (iconUrl.startsWith("lineIcon/")) {
        val bitmap = remember(iconUrl) {
            runCatching {
                context.assets.open("yimu/$iconUrl").use { BitmapFactory.decodeStream(it).asImageBitmap() }
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            )
        }
    } else {
        val resId = remember(iconUrl) { context.resources.getIdentifier(iconUrl, "mipmap", context.packageName) }
        if (resId != 0) {
            Image(painterResource(resId), null, modifier)
        }
    }
}
