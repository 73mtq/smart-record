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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.data.entity.CycleEntity
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 周期设置页：原版 CycleSetSimpleActivity（activity_cycle_set_simple.xml）。
 * 重复类型、金额、备注与每月日期规则。
 */
@Composable
fun CycleEditScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var costText by remember { mutableStateOf("") }
    var remarkText by remember { mutableStateOf("") }
    var cycleType by remember { mutableIntStateOf(CycleEntity.CYCLE_TYPE_BILL) }
    var dateMode by remember { mutableIntStateOf(CycleEntity.MODE_EVERY_MONTH) }
    var monthDayText by remember { mutableStateOf("1") }
    var monthLast by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Filled.Close, "关闭", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("设置重复", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    if (cost <= 0.0) return@IconButton
                    scope.launch {
                        val today = LocalDate.now()
                        val entity = CycleEntity(
                            cost = cost,
                            cycleType = cycleType,
                            dateMode = dateMode,
                            monthDay = monthDayText.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                            monthLast = monthLast,
                            remark = remarkText.trim().ifEmpty { null },
                            addTime = System.currentTimeMillis(),
                            updateTime = System.currentTimeMillis(),
                        )
                        container.periodicRepository.createCycle(entity)
                        onBack()
                    }
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
            CycleField("金额", costText, "请输入周期金额") {
                costText = it.filter { ch -> ch.isDigit() || ch == '.' }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            CycleField("备注", remarkText, "例如 房租 / 宽带费") { remarkText = it }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            CycleChoiceRow("周期类型", listOf("周期记账" to 0, "周期转账" to 1), cycleType) { cycleType = it }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            CycleChoiceRow(
                "重复周期",
                listOf("每天" to CycleEntity.MODE_EVERY_DAY, "每周" to CycleEntity.MODE_EVERY_WEEK, "每月" to CycleEntity.MODE_EVERY_MONTH),
                dateMode,
            ) { dateMode = it }
            if (dateMode == CycleEntity.MODE_EVERY_MONTH) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("每月几号", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(15.dp))
                    TextField(
                        value = monthDayText,
                        onValueChange = { monthDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.width(80.dp).height(56.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { monthLast = !monthLast }) {
                        Text(if (monthLast) "已选月末" else "设为月末")
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(80.dp))
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

@Composable
private fun CycleChoiceRow(
    label: String,
    options: List<Pair<String, Int>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(72.dp))
        options.forEach { (text, value) ->
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (selected == value) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (selected == value) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}
