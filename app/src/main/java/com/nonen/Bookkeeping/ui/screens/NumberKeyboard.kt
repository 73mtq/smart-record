package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.R

@Composable
fun NumberKeyboard(
    state: AmountKeyboardState,
    onChange: (AmountKeyboardState) -> Unit,
    onSave: (Boolean) -> Unit,
    onCancel: () -> Unit,
    repeatEnabled: Boolean = true,
    onInvalidDivision: () -> Unit,
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("再记", "0", "."),
    )
    val tags = mapOf(
        "1" to "one", "2" to "two", "3" to "three", "4" to "four", "5" to "five", "6" to "six",
        "7" to "seven", "8" to "eight", "9" to "nine", "0" to "zero", "." to "point", "再记" to "add_bill_again",
    )
    Row(Modifier.fillMaxWidth().height(220.dp).testTag("number_key_board")
        .background(MaterialTheme.colorScheme.background).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Column(Modifier.weight(3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            keys.forEach { labels ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    labels.forEach { label ->
                        KeyboardKey(if (label == "再记" && !repeatEnabled) stringResource(R.string.yimu_save) else label,
                            requireNotNull(tags[label]), Modifier.weight(1f), action = label == "再记") {
                            if (label == "再记") {
                                if (state.canSave) onSave(repeatEnabled) else if (state.hasZeroDivisor) onInvalidDivision()
                                else if (state.canEvaluate) onChange(state.evaluate())
                            } else onChange(state.digit(label.single()))
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.weight(1f).fillMaxWidth().testTag("delete_one")
                .combinedClickable(onClick = { onChange(state.backspace()) }, onLongClick = { onChange(AmountKeyboardState()) }),
                contentAlignment = Alignment.Center) {
                Icon(painterResource(R.mipmap.ic_delete_one), "退格", Modifier.size(22.dp))
            }
            KeyboardDivider()
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OperatorCell(AmountOperator.ADD.symbol, "add") {
                    if (state.hasZeroDivisor) onInvalidDivision() else onChange(state.operate(AmountOperator.ADD))
                }
                KeyboardVerticalDivider()
                OperatorCell(AmountOperator.SUBTRACT.symbol, "reduce") {
                    if (state.hasZeroDivisor) onInvalidDivision() else onChange(state.operate(AmountOperator.SUBTRACT))
                }
            }
            KeyboardDivider()
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OperatorCell(AmountOperator.MULTIPLY.symbol, "ride") {
                    if (state.hasZeroDivisor) onInvalidDivision() else onChange(state.operate(AmountOperator.MULTIPLY))
                }
                KeyboardVerticalDivider()
                OperatorCell(AmountOperator.DIVIDE.symbol, "except") {
                    if (state.hasZeroDivisor) onInvalidDivision() else onChange(state.operate(AmountOperator.DIVIDE))
                }
            }
            KeyboardDivider()
            Box(Modifier.weight(1f).fillMaxWidth().testTag("cancel").clickable {
                when {
                    state.hasZeroDivisor -> onInvalidDivision()
                    state.canEvaluate -> onChange(state.evaluate())
                    state.canSave -> onSave(false)
                    state.operator == null -> onCancel()
                }
            }, contentAlignment = Alignment.Center) {
                Text(state.actionLabel, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun KeyboardDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surface))
}

@Composable
private fun KeyboardVerticalDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
}

@Composable
private fun RowScope.OperatorCell(text: String, tag: String, onClick: () -> Unit) {
    Box(Modifier.weight(1f).fillMaxHeight().testTag(tag).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KeyboardKey(text: String, tag: String, modifier: Modifier, action: Boolean = false, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight().clip(RoundedCornerShape(8.dp)).testTag(tag)
        .background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = if (action) 15.sp else 20.sp, fontWeight = if (action) FontWeight.Normal else FontWeight.Bold)
    }
}
