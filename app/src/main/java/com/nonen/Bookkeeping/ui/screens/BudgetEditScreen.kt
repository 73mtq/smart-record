package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 预算设置页：原版 BudgetSelfActivity（activity_budget_self.xml）。
 * 维护指定月份的预算总额；分类预算仍在预算页弹窗内设置。
 */
@Composable
fun BudgetEditScreen(
    container: AppContainer,
    year: Int,
    month: Int,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var amountText by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(year, month) {
        val budget = container.budgetRepository.getOrInitMonthBudget(bookId = 1L, year = year, month = month)
        amountText = if (budget.num > 0.0) String.format(Locale.US, "%.2f", budget.num) else ""
        loaded = true
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Filled.Close, "关闭", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("预算设置", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    scope.launch {
                        container.budgetRepository.setMonthBudget(
                            bookId = 1L,
                            year = year,
                            month = month,
                            amount = amount,
                        )
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
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(start = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("预算金额", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("请输入预算金额", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 15.dp),
                )
            }
            Text(
                text = "$year 年 $month 月",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 15.dp, bottom = 15.dp),
            )
        }
    }
}
