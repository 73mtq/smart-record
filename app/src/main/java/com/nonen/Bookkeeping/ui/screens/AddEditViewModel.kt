package com.nonen.Bookkeeping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.core.CategorizationRules
import com.nonen.Bookkeeping.core.HashUtil
import com.nonen.Bookkeeping.core.RuleEngine
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.parse.PaymentTextParser
import com.nonen.Bookkeeping.data.repo.RuleRepository
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 「记一笔 / 编辑记录」表单状态与业务逻辑。
 * 支持输入信息实时智能自动匹配分类（商户记忆 -> 自定义规则 -> 内置规则 -> 兜底）。
 */
class AddEditViewModel(
    private val repo: TransactionRepository,
    private val ruleEngine: RuleEngine? = null,
    private val ruleRepo: RuleRepository? = null,
    private val txId: Long,
) : ViewModel() {

    var isIncome by mutableStateOf(false)
        private set
    var amount by mutableStateOf("")
    var category by mutableStateOf(Categories.expenseCategories.first())
    var subCategory by mutableStateOf("")
    var note by mutableStateOf("")
    var merchant by mutableStateOf("")
    var account by mutableStateOf(Categories.defaultAccounts.first())
    var tag by mutableStateOf("")
    var transactionType by mutableStateOf("EXPENSE")
    var refundOriginalId by mutableStateOf<Long?>(null)
    var targetAccount by mutableStateOf(Categories.defaultAccounts.getOrElse(1) { "支付宝" })
    var timestamp by mutableLongStateOf(System.currentTimeMillis())
    var saveAsRule by mutableStateOf(false)
    var loading by mutableStateOf(txId != 0L)
        private set
    var isEdit by mutableStateOf(false)
        private set
    /** 表单校验/入库冲突的提示文案（资源 id，由 UI 层解析） */
    var errorMessageRes by mutableStateOf<Int?>(null)

    /** 用户是否手动主动指定了分类；若主动选过，则不再被输入文字自动冲掉 */
    var isCategoryManuallyChosen by mutableStateOf(false)
        private set

    /** 当前智能自动推荐并应用的分类（UI 可展示高亮气泡或角标） */
    var autoSuggestedCategory by mutableStateOf<String?>(null)
        private set

    private var original: TransactionEntity? = null
    private var originalCategory: String? = null
    private var cachedRules: CategorizationRules? = null

    val categoryList: List<String>
        get() = if (isIncome) Categories.incomeCategories else Categories.expenseCategories

    val notePlaceholder: String
        get() = when (category) {
            "餐饮" -> "输入餐厅名称、商圈或外卖平台"
            "软件服务" -> "输入服务商、充值额度或订阅期限"
            "教育" -> "输入培训项目、考试科目或资料名称"
            "交通" -> "输入起止地点、共享单车平台或车次"
            "医疗" -> "输入就诊项目、药品名称或视力矫正"
            "娱乐" -> "输入影院、游戏名称或活动项目"
            else -> "备注（可选）"
        }

    val repeatedNoteWarning: Boolean
        get() = category == "餐饮" && note.trim() in setOf("早饭", "午饭", "晚饭", "早餐", "午餐", "晚餐")

    val largeExpenseWarning: Boolean
        get() = !isIncome && (amount.toDoubleOrNull() ?: 0.0) >= 100.0 && note.isBlank()

    init {
        if (txId != 0L) {
            viewModelScope.launch {
                repo.getById(txId)?.let { t ->
                    original = t
                    originalCategory = t.category
                    isEdit = true
                    isIncome = t.amount > 0
                    amount = String.format(Locale.US, "%.2f", kotlin.math.abs(t.amount))
                    category = t.category
                    subCategory = t.subCategory.orEmpty()
                    note = t.note.orEmpty()
                    merchant = t.merchant.orEmpty()
                    account = t.account ?: Categories.defaultAccounts.first()
                    tag = t.tag.orEmpty()
                    transactionType = t.transactionType
                    refundOriginalId = t.refundOriginalId
                    targetAccount = t.targetAccount ?: Categories.defaultAccounts.getOrElse(1) { "支付宝" }
                    timestamp = t.timestamp
                    isCategoryManuallyChosen = true
                }
                loading = false
            }
        } else {
            viewModelScope.launch {
                cachedRules = ruleEngine?.loadRules()
            }
        }
    }

    fun setType(income: Boolean) {
        if (isIncome == income) return
        isIncome = income
        if (category !in categoryList) category = categoryList.first()
        isCategoryManuallyChosen = true
    }

    fun setAmountInput(raw: String) {
        amount = raw.filter { it.isDigit() || it == '.' }.take(12)
    }

    fun selectCategoryManually(cat: String) {
        category = cat
        isCategoryManuallyChosen = true
    }

    fun resetManualCategory() {
        isCategoryManuallyChosen = false
        triggerAutoClassify()
    }

    fun setMerchantInput(v: String) {
        merchant = v
        triggerAutoClassify()
    }

    fun setNoteInput(v: String) {
        note = v
        triggerAutoClassify()
    }

    /** 快速自然文本智能识别填入（如：“妈妈微信转账生活费2000”或“麦当劳汉堡38.5”） */
    fun applyQuickText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return

        // 1. 尝试提取金额
        val amt = PaymentTextParser.extractAmount(text)
            ?: Regex("""(?:^|\s|[^\d.])([0-9]+(?:\.[0-9]{1,2})?)(?:$|\s|[^\d.])""").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        if (amt != null && amt > 0.0) {
            amount = String.format(Locale.US, "%.2f", amt)
        }

        note = text
        isCategoryManuallyChosen = false
        triggerAutoClassify()
    }

    /**
     * 自动分类逻辑：
     * 根据商户与备注文字，在规则引擎快照中查找最优分类并联动切换收支方向与分类高亮。
     */
    private fun triggerAutoClassify() {
        if (isEdit || isCategoryManuallyChosen) return
        val rules = cachedRules ?: return
        val combined = listOf(merchant.trim(), note.trim()).filter { it.isNotEmpty() }.joinToString(" ")
        if (combined.isEmpty()) {
            autoSuggestedCategory = null
            return
        }

        // 1. 先在当前收支方向中寻找
        val currentMatch = RuleEngine.classify(
            text = combined,
            isIncome = isIncome,
            rules = rules,
            merchant = merchant.trim().ifEmpty { null },
        )
        val defaultFallback = if (isIncome) Categories.OTHER_INCOME else Categories.OTHER_EXPENSE
        if (currentMatch != defaultFallback) {
            category = currentMatch
            autoSuggestedCategory = currentMatch
            return
        }

        // 2. 若当前方向未命中特异规则，探查另一方向是否强命中（例如当前为默认支出，但用户输入了“生活费”或“妈妈转账”）
        val oppositeMatch = RuleEngine.classify(
            text = combined,
            isIncome = !isIncome,
            rules = rules,
            merchant = merchant.trim().ifEmpty { null },
        )
        val oppositeFallback = if (!isIncome) Categories.OTHER_INCOME else Categories.OTHER_EXPENSE
        if (oppositeMatch != oppositeFallback) {
            isIncome = !isIncome
            category = oppositeMatch
            autoSuggestedCategory = oppositeMatch
        } else {
            autoSuggestedCategory = null
        }
    }

    fun save(onDone: () -> Unit) {
        val value = amount.toDoubleOrNull()
        if (value == null || value <= 0.0) {
            errorMessageRes = R.string.error_invalid_amount
            return
        }
        viewModelScope.launch {
            if (saveAsRule) {
                val keyword = merchant.trim().ifEmpty { note.trim() }
                if (keyword.isNotEmpty()) {
                    ruleRepo?.add(keyword, category, isIncome)
                }
            }
            val existing = original
            if (existing != null) {
                val updated = existing.copy(
                    amount = if (isIncome) value else -value,
                    category = category,
                    subCategory = subCategory.trim().ifEmpty { null },
                    note = note.trim().ifEmpty { null },
                    merchant = merchant.trim().ifEmpty { null },
                    account = account.trim().ifEmpty { null },
                    tag = tag.trim().ifEmpty { null },
                    transactionType = if (isIncome) "INCOME" else "EXPENSE",
                    refundOriginalId = refundOriginalId,
                    targetAccount = targetAccount.trim().ifEmpty { null },
                    timestamp = timestamp,
                )
                repo.update(updated)
                repo.learnFromEdit(updated, originalCategory)
                onDone()
            } else {
                val signed = if (isIncome) value else -value
                val m = merchant.trim().ifEmpty { null }
                val entity = TransactionEntity(
                    amount = signed,
                    category = category,
                    subCategory = subCategory.trim().ifEmpty { null },
                    note = note.trim().ifEmpty { null },
                    merchant = m,
                    account = account.trim().ifEmpty { null },
                    tag = tag.trim().ifEmpty { null },
                    transactionType = if (isIncome) "INCOME" else "EXPENSE",
                    refundOriginalId = refundOriginalId,
                    targetAccount = targetAccount.trim().ifEmpty { null },
                    timestamp = timestamp,
                    source = "manual",
                    hash = HashUtil.transactionHash(timestamp, signed, m, "manual"),
                )
                if (repo.insertIfNew(entity)) {
                    onDone()
                } else {
                    errorMessageRes = R.string.error_duplicate_record
                }
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val existing = original ?: return
        viewModelScope.launch {
            repo.delete(existing)
            onDone()
        }
    }
}
