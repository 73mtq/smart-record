package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.ui.components.YimuAiTypeNativeView

/** 规则页：原版 CategoryKeyManagerActivity（activity_self_ai_type.xml），列表行 item_ai_type.xml。 */
@Composable
fun RulesScreen(vm: RulesViewModel, onBack: () -> Unit) {
    val rules by vm.rules.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var duplicateWarning by remember { mutableStateOf(false) }

    val statusBarPx = WindowInsets.statusBars.getTop(LocalDensity.current)
    AndroidView(
        modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        factory = { viewContext -> YimuAiTypeNativeView(viewContext) },
        update = { view ->
            view.applyToolbarInset(statusBarPx)
            view.bind(
                object : YimuAiTypeNativeView.Listener {
                    override fun onBack() = onBack()
                    override fun onAdd() { showAdd = true }
                },
                rules,
            )
        },
    )

    if (showAdd) {
        AddRuleDialog(
            onDismiss = { showAdd = false },
            onConfirm = { keyword, category, isIncome ->
                vm.add(keyword, category, isIncome) { ok -> if (!ok) duplicateWarning = true }
                showAdd = false
            },
        )
    }

    if (duplicateWarning) {
        AlertDialog(
            onDismissRequest = { duplicateWarning = false },
            title = { Text(stringResource(R.string.dialog_duplicate_title)) },
            text = { Text(stringResource(R.string.dialog_duplicate_message)) },
            confirmButton = {
                TextButton(onClick = { duplicateWarning = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }
}

@Composable
private fun AddRuleDialog(onDismiss: () -> Unit, onConfirm: (String, String, Boolean) -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Categories.expenseCategories.first()) }
    var menu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_rule_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.dialog_add_rule_hint), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.field_keyword)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_category_prefix), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { menu = true }) { Text("${Categories.emoji(category)} $category") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        Categories.allCategories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${Categories.emoji(c)} $c") },
                                onClick = { category = c; menu = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isIncome = category in Categories.incomeCategories
            TextButton(onClick = { onConfirm(keyword, category, isIncome) }) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
