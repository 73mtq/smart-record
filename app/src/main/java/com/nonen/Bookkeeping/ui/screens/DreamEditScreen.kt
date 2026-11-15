package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.ui.vmFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 愿望新增/编辑页：原版 AddDreamActivity（activity_add_dream.xml）。
 * 名称、目标金额、已攒金额、备注、结束日期；编辑态可删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamEditScreen(
    container: AppContainer,
    dreamId: Long,
    onBack: () -> Unit,
) {
    val vm: DreamEditViewModel = viewModel(
        key = "dream_edit_$dreamId",
        factory = vmFactory { DreamEditViewModel(container.savingsGoalRepository, dreamId) },
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val isEditing = dreamId != 0L
    val endDate = Instant.ofEpochMilli(vm.endTime).atZone(ZoneId.systemDefault()).toLocalDate()
    val dateText = endDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Filled.Close, "关闭", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = if (isEditing) "编辑愿望" else "新增愿望",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (isEditing) {
                IconButton(onClick = { showDelete = true }, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Filled.Delete, "删除愿望", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
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

        Column(
            Modifier.fillMaxWidth().padding(15.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()),
        ) {
            DreamField("愿望名称", vm.name, "请输入愿望名称") { vm.name = it }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            DreamField("目标金额", vm.targetText, "请输入目标金额") { vm.targetText = it }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            DreamField("已攒金额", vm.initialText, "请输入已攒金额，默认为0") { vm.initialText = it }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            DreamField("备注", vm.remark, "请输入备注") { vm.remark = it }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(
                Modifier.fillMaxWidth().height(56.dp).clickable { showDatePicker = true }.padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("结束日期", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(dateText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(10.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = vm.endTime)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.endTime = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DatePicker(state)
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除愿望") },
            text = { Text("删除「${vm.dream?.dreamName ?: vm.name}」后，攒钱流水会一并移除。") },
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
private fun DreamField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(96.dp))
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        )
    }
}
