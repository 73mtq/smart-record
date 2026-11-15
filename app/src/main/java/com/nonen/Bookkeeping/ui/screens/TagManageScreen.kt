package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.data.db.TagEntity

/**
 * 标签管理页：原版 TagManagerActivity（activity_tag_manager.xml）。
 * 一级标签为卡片，卡片内是二级标签胶囊；顶部工具栏提供返回与新增。
 */
@Composable
fun TagManageScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onAddTag: (Long) -> Unit,
    onEditTag: (Long) -> Unit,
) {
    val tags by remember(container) { container.tagRepository.observeAll() }.collectAsState(initial = emptyList())
    val parents = tags.filter { it.parentTagId == 0L }
    val childrenByParent = tags.filter { it.parentTagId != 0L }.groupBy { it.parentTagId }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background),
    ) {
        TagToolbar(title = "标签管理", onBack = onBack, onAdd = { onAddTag(0L) })
        if (parents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "没有任何标签",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 15.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                items(parents, key = { it.tagId }) { parent ->
                    TagGroupCard(
                        parent = parent,
                        children = childrenByParent[parent.tagId].orEmpty(),
                        onAddChild = { onAddTag(parent.tagId) },
                        onEdit = onEditTag,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagGroupCard(
    parent: TagEntity,
    children: List<TagEntity>,
    onAddChild: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).padding(start = 15.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(15.dp).clip(CircleShape).background(tagColor(parent.color)))
            Spacer(Modifier.width(10.dp))
            Text(parent.tagName, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAddChild, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Add, "新增子标签", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onEdit(parent.tagId) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Edit, "编辑标签", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (children.isNotEmpty()) {
            TagChips(children = children, onEdit = onEdit)
            Spacer(Modifier.height(15.dp))
        } else {
            Spacer(Modifier.height(15.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(children: List<TagEntity>, onEdit: (Long) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, top = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        children.forEach { child ->
            TagChip(child, onEdit)
        }
    }
}

@Composable
private fun TagChip(tag: TagEntity, onEdit: (Long) -> Unit) {
    Box(
        Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(tagColor(tag.color))
            .padding(horizontal = 10.dp)
            .clickable { onEdit(tag.tagId) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tag.tagName,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TagToolbar(title: String, onBack: () -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回", Modifier.size(22.dp))
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onAdd) { Text("新增", fontSize = 15.sp, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
internal fun tagColor(color: Int): Color =
    if (color == 0) MaterialTheme.colorScheme.primary else Color(color)
