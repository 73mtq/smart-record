package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.data.db.TagEntity
import kotlinx.coroutines.launch

/** 可选标签颜色，0 表示跟随主题。 */
internal val tagPalette = listOf(
    0,
    0xFFF06A6A.toInt(),
    0xFFFF9F43.toInt(),
    0xFFFFD166.toInt(),
    0xFF48AB93.toInt(),
    0xFF4D96FF.toInt(),
    0xFF6C5CE7.toInt(),
    0xFFE84393.toInt(),
    0xFF00B894.toInt(),
    0xFF0984E3.toInt(),
    0xFF636E72.toInt(),
    0xFFB2BEC3.toInt(),
)

/**
 * 标签新增/编辑页：原版 AddTagActivity 与 EditTagActivity（activity_add_tag.xml、activity_edit_tag.xml）。
 * 名称、颜色、一级标签三个区域，编辑态多一个删除入口。
 */
@Composable
fun TagEditScreen(
    container: AppContainer,
    tagId: Long,
    parentTagId: Long,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val allTags by remember(container) { container.tagRepository.observeAll() }.collectAsState(initial = emptyList())
    var original by remember { mutableStateOf<TagEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(0) }
    var parent by remember { mutableLongStateOf(parentTagId) }
    var showColors by remember { mutableStateOf(false) }
    var showParents by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val isEditing = tagId > 0L

    LaunchedEffect(tagId) {
        if (isEditing) {
            val tag = container.tagRepository.getById(tagId)
            if (tag != null) {
                original = tag
                name = tag.tagName
                color = tag.color
                parent = tag.parentTagId
            }
        }
    }

    val parents = allTags.filter { it.parentTagId == 0L && it.tagId != tagId }
    val parentName = parents.firstOrNull { it.tagId == parent }?.tagName ?: "无（一级标签）"

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Filled.Close, "关闭", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = if (isEditing) "编辑标签" else "新增标签",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (isEditing) {
                IconButton(onClick = { showDelete = true }, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Filled.Delete, "删除标签", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            IconButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        error = "标签名称不能为空"
                        return@IconButton
                    }
                    scope.launch {
                        try {
                            val current = original
                            if (current == null) {
                                container.tagRepository.create(trimmed, color, parent)
                            } else {
                                container.tagRepository.rename(current, trimmed)
                                container.tagRepository.update(
                                    current.copy(tagName = trimmed, color = color, parentTagId = parent),
                                )
                            }
                            onBack()
                        } catch (e: IllegalStateException) {
                            error = e.message
                        }
                    }
                },
                modifier = Modifier.size(50.dp),
            ) {
                Icon(Icons.Filled.Check, "保存", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("标签名称", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth().height(55.dp),
        )
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        Row(
            Modifier.fillMaxWidth().height(55.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { showColors = true }
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("标签颜色", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (color == 0) "跟随系统" else "自定义颜色",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(15.dp).clip(CircleShape)
                    .background(if (color == 0) MaterialTheme.colorScheme.primary else Color(color)),
            )
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

        Row(
            Modifier.fillMaxWidth().height(55.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { showParents = true }
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("一级标签", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(parentName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
        }

        error?.let { message ->
            AlertDialog(
                onDismissRequest = { error = null },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { error = null }) { Text("确定") } },
            )
        }
    }

    if (showColors) {
        AlertDialog(
            onDismissRequest = { showColors = false },
            title = { Text("标签颜色") },
            text = {
                Column(Modifier.padding(vertical = 8.dp)) {
                    val rows = tagPalette.chunked(6)
                    rows.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { value ->
                                val selected = value == color
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (value == 0) MaterialTheme.colorScheme.primary else Color(value))
                                        .clickable { color = value; showColors = false }
                                        .then(
                                            if (selected) {
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (value == 0) {
                                        Text("系", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColors = false }) { Text("取消") } },
        )
    }

    if (showParents) {
        AlertDialog(
            onDismissRequest = { showParents = false },
            title = { Text("一级标签") },
            text = {
                Column {
                    TextButton(onClick = { parent = 0L; showParents = false }) {
                        Text("无（一级标签）", color = MaterialTheme.colorScheme.onSurface)
                    }
                    parents.forEach { tag ->
                        TextButton(onClick = { parent = tag.tagId; showParents = false }) {
                            Text(tag.tagName, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showParents = false }) { Text("取消") } },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除标签") },
            text = { Text("删除「${original?.tagName ?: name}」后，其下二级标签与账单关联会一并移除。") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        container.tagRepository.delete(tagId)
                        onBack()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } },
        )
    }
}
