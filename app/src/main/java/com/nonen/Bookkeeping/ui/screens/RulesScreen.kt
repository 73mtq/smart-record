package com.nonen.Bookkeeping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.core.RuleEngine
import com.nonen.Bookkeeping.data.repo.RuleRepository
import com.nonen.Bookkeeping.ui.components.AnimatedSegmented
import com.nonen.Bookkeeping.ui.components.YimuCategoryKeyNativeView
import com.nonen.Bookkeeping.ui.theme.AppleBlue
import com.nonen.Bookkeeping.ui.theme.ExpenseColor
import com.nonen.Bookkeeping.ui.theme.IncomeColor
import com.nonen.Bookkeeping.ui.theme.InkPrimary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(
    private val ruleRepo: RuleRepository,
    private val ruleEngine: RuleEngine? = null,
) : ViewModel() {

    val rules = ruleRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 已学习的商户记忆条数（改分类时自动积累） */
    val merchantCount = ruleRepo.observeMerchantCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    var testInput by mutableStateOf("")
    var testIsIncome by mutableStateOf(false)
    var testResult by mutableStateOf<RuleEngine.MatchDetail?>(null)
        private set

    fun setTestText(text: String) {
        testInput = text
        triggerTest()
    }

    fun setTestIncome(income: Boolean) {
        testIsIncome = income
        triggerTest()
    }

    fun triggerTest() {
        val text = testInput.trim()
        if (text.isEmpty()) {
            testResult = null
            return
        }
        viewModelScope.launch {
            val rulesSnapshot = ruleEngine?.loadRules()
            if (rulesSnapshot != null) {
                testResult = RuleEngine.classifyDetail(
                    text = text,
                    isIncome = testIsIncome,
                    rules = rulesSnapshot,
                    merchant = text,
                )
            }
        }
    }

    fun add(keyword: String, category: String, isIncome: Boolean, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = ruleRepo.add(keyword, category, isIncome)
            onResult(ok)
            if (ok && testInput.isNotBlank()) {
                triggerTest()
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            ruleRepo.delete(id)
            if (testInput.isNotBlank()) {
                triggerTest()
            }
        }
    }

    fun clearMerchantMemory() {
        viewModelScope.launch {
            ruleRepo.clearMerchantMemory()
            if (testInput.isNotBlank()) {
                triggerTest()
            }
        }
    }
}

data class PresetRuleItem(
    val keyword: String,
    val category: String,
    val isIncome: Boolean,
)

private val PRESET_RULES = listOf(
    PresetRuleItem("生活费", "生活费", true),
    PresetRuleItem("妈妈转账", "生活费", true),
    PresetRuleItem("奖学金", "奖学金", true),
    PresetRuleItem("兼职", "兼职", true),
    PresetRuleItem("监考费", "兼职", true),
    PresetRuleItem("晚饭", "餐饮", false),
    PresetRuleItem("午饭", "餐饮", false),
    PresetRuleItem("早饭", "餐饮", false),
    PresetRuleItem("洗衣服", "居住", false),
    PresetRuleItem("空调费", "居住", false),
    PresetRuleItem("水卡", "居住", false),
    PresetRuleItem("电卡", "居住", false),
    PresetRuleItem("打印资料", "学习", false),
    PresetRuleItem("车票", "交通", false),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(vm: RulesViewModel, onBack: () -> Unit) {
    val rules by vm.rules.collectAsState()
    val merchantCount by vm.merchantCount.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmClearMemory by remember { mutableStateOf(false) }
    var duplicateWarning by remember { mutableStateOf(false) }

    var filterType by remember { mutableIntStateOf(0) } // 0: 全部, 1: 自定义, 2: 内置
    var searchQuery by remember { mutableStateOf("") }

    val customRules = remember(rules) { rules.filter { it.isCustom } }
    val builtinRules = remember(rules) { rules.filter { !it.isCustom } }
    val customKeywords = remember(customRules) { customRules.map { it.keyword }.toSet() }

    val baseRules = when (filterType) {
        1 -> customRules
        2 -> builtinRules
        else -> rules
    }

    val displayRules = remember(baseRules, searchQuery) {
        if (searchQuery.isBlank()) {
            baseRules
        } else {
            val q = searchQuery.trim()
            baseRules.filter {
                it.keyword.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
            }
        }
    }

    val statusBarPx = WindowInsets.statusBars.getTop(LocalDensity.current)
    AndroidView(
        modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        factory = { viewContext -> YimuCategoryKeyNativeView(viewContext) },
        update = { view ->
            view.applyToolbarInset(statusBarPx)
            view.bind(
                object : YimuCategoryKeyNativeView.Listener {
                    override fun onBack() = onBack()
                    override fun onAdd() { showAdd = true }
                },
                customRules.isNotEmpty(),
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

    if (confirmClearMemory) {
        AlertDialog(
            onDismissRequest = { confirmClearMemory = false },
            title = { Text(stringResource(R.string.dialog_clear_memory_title)) },
            text = { Text(stringResource(R.string.dialog_clear_memory_message)) },
            dismissButton = {
                TextButton(onClick = { confirmClearMemory = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearMemory = false
                    vm.clearMerchantMemory()
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
        )
    }
}

@Composable
private fun RuleTestingCard(
    vm: RulesViewModel,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "规则测试演练",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AnimatedSegmented(
                    options = listOf(stringResource(R.string.type_expense), stringResource(R.string.type_income)),
                    selectedIndex = if (vm.testIsIncome) 1 else 0,
                    onSelected = { vm.setTestIncome(it == 1) },
                    thumbColor = if (vm.testIsIncome) IncomeColor else ExpenseColor,
                    modifier = Modifier.width(130.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = vm.testInput,
                onValueChange = { vm.setTestText(it) },
                label = { Text("输入词语演练匹配（如：晚饭、妈妈转账、空调费）") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppleBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                ),
                trailingIcon = {
                    if (vm.testInput.isNotEmpty()) {
                        IconButton(onClick = { vm.setTestText("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            val detail = vm.testResult
            if (detail != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${Categories.emoji(detail.category)} ${detail.category}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (detail.levelName) {
                                            "商户记忆" -> AppleBlue.copy(alpha = 0.15f)
                                            "自定义规则" -> IncomeColor.copy(alpha = 0.15f)
                                            "内置规则" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = detail.levelName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when (detail.levelName) {
                                        "商户记忆" -> AppleBlue
                                        "自定义规则" -> IncomeColor
                                        "内置规则" -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                        if (detail.matchedRule != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "命中词: ${detail.matchedRule}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetRecommendationsCard(
    existingKeywords: Set<String>,
    onAddPreset: (PresetRuleItem) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "常用规则推荐",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "点按即可添加为自定义规则",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESET_RULES.forEach { preset ->
                    val added = preset.keyword in existingKeywords
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (added) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            .clickable(enabled = !added) { onAddPreset(preset) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (added) "✓ ${preset.keyword}" else "+ ${preset.keyword} → ${preset.category}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (added) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** 商户记忆卡片：说明来源、显示条数、提供清空入口（关键词规则不受影响） */
@Composable
private fun MerchantMemoryCard(count: Int, onClear: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.merchant_memory_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.merchant_memory_summary, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (count > 0) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.action_clear))
                }
            }
        }
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
                        focusedBorderColor = AppleBlue,
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
