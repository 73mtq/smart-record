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
import com.nonen.Bookkeeping.core.CategoryMapping
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    private val categoryRepo: CategoryRepository? = null,
    private val assetRepo: com.nonen.Bookkeeping.data.repo.AssetRepository? = null,
    private val templateRepo: com.nonen.Bookkeeping.data.repo.TemplateRepository? = null,
    private val txId: Long,
    private val externalScope: CoroutineScope? = null,
    initialBookId: Long = 1L,
    private val initialTemplateId: Long = 0L,
    initialDate: LocalDate? = null,
    val editingTemplate: Boolean = false,
    initialTransactionType: String = "EXPENSE",
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    var isIncome by mutableStateOf(false)
        private set
    var amount by mutableStateOf("")
    var keyboard by mutableStateOf(AmountKeyboardState())
    var saving by mutableStateOf(false)
        private set
    var transferToAssetId by mutableStateOf<Long?>(null)
    var transferServiceCharge by mutableStateOf(0.0)
    var templateName by mutableStateOf("")
    var templateLock by mutableStateOf(false)
    private var categoryJob: Job? = null
    private var templateJob: Job? = null
    private var originalTemplate: ModuleBillEntity? = null

    /** 模板编辑模式下是否存在可删除的原始模板。 */
    val canDeleteTemplate: Boolean get() = editingTemplate && originalTemplate != null

    fun updateKeyboard(state: AmountKeyboardState) {
        keyboard = state
        amount = if (state.operator == null) state.operand else ""
    }

    fun prepareNextRecord() {
        original = null
        originalCategory = null
        isEdit = false
        amount = ""
        keyboard = AmountKeyboardState()
        note = ""
        tag = ""
        timestamp = System.currentTimeMillis()
    }

    fun saveTransfer(onDone: () -> Unit) {
        if (saving) return
        val value = amount.toDoubleOrNull()
        val target = transferToAssetId
        if (value == null || !value.isFinite() || value <= 0 || !transferServiceCharge.isFinite()
            || target == null || target == assetId) {
            errorMessageRes = R.string.yimu_invalid_transfer
            return
        }
        saving = true
        scope.launch {
            try {
                check(requireNotNull(assetRepo).executeTransfer(
                    assetId, target, value, serviceCharge = transferServiceCharge, remark = note, time = timestamp,
                ))
                onDone()
            } finally {
                saving = false
            }
        }
    }
    var category by mutableStateOf("其他")
    var subCategory by mutableStateOf("")
    var parentCategoryId by mutableStateOf<Int?>(null)
    var childCategoryId by mutableStateOf<Int?>(null)
    var bookId by mutableStateOf(initialBookId)
    var assetId by mutableStateOf(1L)
    var liveCategories by mutableStateOf<List<CategoryEntity>>(emptyList())
    var liveAssets by mutableStateOf<List<com.nonen.Bookkeeping.data.db.AssetEntity>>(emptyList())
        private set
    var liveTemplates by mutableStateOf<List<com.nonen.Bookkeeping.data.entity.ModuleBillEntity>>(emptyList())
        private set
    var note by mutableStateOf("")
    var merchant by mutableStateOf("")
    var account by mutableStateOf("")
    var tag by mutableStateOf("")
    var transactionType by mutableStateOf(initialTransactionType)
    var refundOriginalId by mutableStateOf<Long?>(null)
    var targetAccount by mutableStateOf(Categories.defaultAccounts.getOrElse(1) { "支付宝" })
    var timestamp by mutableLongStateOf(
        initialDate?.atTime(LocalTime.now())?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis(),
    )
    var saveAsRule by mutableStateOf(false)
    var loading by mutableStateOf(txId != 0L || initialTemplateId != 0L)
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
        observeCategories()
        observeAssets()
        observeTemplates()
        if (txId != 0L) {
            scope.launch {
                repo.getById(txId)?.let { t ->
                    original = t
                    originalCategory = t.category
                    isEdit = true
                    isIncome = t.amount > 0
                    amount = String.format(Locale.US, "%.2f", kotlin.math.abs(t.amount))
                    category = t.category
                    subCategory = t.subCategory.orEmpty()
                    parentCategoryId = t.parentCategoryId
                    childCategoryId = t.childCategoryId
                    bookId = t.bookId
                    assetId = t.assetId
                    note = t.note.orEmpty()
                    merchant = t.merchant.orEmpty()
                    account = t.account ?: Categories.defaultAccounts.first()
                    tag = t.tag.orEmpty()
                    transactionType = t.transactionType
                    refundOriginalId = t.refundOriginalId
                    targetAccount = t.targetAccount ?: Categories.defaultAccounts.getOrElse(1) { "支付宝" }
                    timestamp = t.timestamp
                    isCategoryManuallyChosen = true
                    observeCategories()
                }
                loading = false
            }
        } else if (initialTemplateId != 0L) {
            scope.launch {
                val template = requireNotNull(requireNotNull(templateRepo).getById(initialTemplateId))
                if (editingTemplate) originalTemplate = template
                applyTemplateValues(template)
                loading = false
            }
        } else {
            scope.launch {
                cachedRules = ruleEngine?.loadRules()
                syncCategoryIds()
            }
        }
    }

    private fun observeCategories() {
        categoryJob?.cancel()
        categoryJob = scope.launch {
            val cRepo = categoryRepo ?: return@launch
            cRepo.observeByType(if (isIncome) 1 else 0).collectLatest { list ->
                liveCategories = list.filter { !it.isHiddenInBook(bookId) }
                if (parentCategoryId == null) {
                    syncCategoryIds()
                }
            }
        }
    }

    private fun observeAssets() {
        scope.launch {
            val aRepo = assetRepo ?: return@launch
            aRepo.observeVisible().collectLatest { list ->
                liveAssets = list
                val current = list.firstOrNull { it.assetId == assetId }
                if (current != null) {
                    account = current.assetName
                } else if (list.isNotEmpty() && txId == 0L && assetId == 1L) {
                    val defaultAsset = list.firstOrNull { it.assetId == 1L } ?: list.first()
                    assetId = defaultAsset.assetId
                    account = defaultAsset.assetName
                }
            }
        }
    }

    fun selectAsset(asset: com.nonen.Bookkeeping.data.db.AssetEntity) {
        assetId = asset.assetId
        account = asset.assetName
    }

    fun selectBook(id: Long) {
        bookId = id
        observeCategories()
        observeTemplates()
    }

    fun setTransferType() {
        transactionType = "TRANSFER"
    }

    private fun observeTemplates() {
        templateJob?.cancel()
        templateJob = scope.launch {
            val tRepo = templateRepo ?: return@launch
            tRepo.observeAll(bookId).collectLatest { list ->
                liveTemplates = list
            }
        }
    }

    fun applyTemplate(t: com.nonen.Bookkeeping.data.entity.ModuleBillEntity) {
        scope.launch { applyTemplateValues(t) }
    }

    private suspend fun applyTemplateValues(t: com.nonen.Bookkeeping.data.entity.ModuleBillEntity) {
        val categories = requireNotNull(categoryRepo).getAll()
        val parent = categories.firstOrNull { it.parentId == 0 && it.categoryId == t.parentCategoryId }
        val child = categories.firstOrNull { it.parentId == t.parentCategoryId && it.categoryId == t.childCategoryId }
        if (t.moduleType == com.nonen.Bookkeeping.data.entity.ModuleBillEntity.MODULE_TYPE_BILL) requireNotNull(parent)
        isIncome = parent?.categoryType == 1
        transactionType = if (t.moduleType == com.nonen.Bookkeeping.data.entity.ModuleBillEntity.MODULE_TYPE_TRANSFER)
            "TRANSFER" else if (isIncome) "INCOME" else "EXPENSE"
        bookId = t.bookId
        updateKeyboard(AmountKeyboardState(if (t.cost == 0.0) "" else String.format(Locale.US, "%.2f", kotlin.math.abs(t.cost))))
        parentCategoryId = parent?.categoryId
        childCategoryId = child?.categoryId
        if (parent != null) category = parent.categoryName
        subCategory = child?.categoryName.orEmpty()
        note = t.remark.orEmpty()
        tag = t.tags.orEmpty()
        assetId = if (transactionType == "TRANSFER") t.fromAssetId else t.assetId
        account = liveAssets.firstOrNull { it.assetId == assetId }?.assetName.orEmpty()
        transferToAssetId = if (transactionType == "TRANSFER") t.toAssetId else null
        transferServiceCharge = t.serviceCharge
        templateName = t.templateName
        templateLock = t.moduleLock
        liveCategories = categories.filter { it.categoryType == if (isIncome) 1 else 0 }.filter { !it.isHiddenInBook(bookId) }
        observeCategories()
        observeTemplates()
        isCategoryManuallyChosen = true
    }

    fun saveCurrentAsTemplate(name: String, onDone: () -> Unit) {
        if (saving) return
        val costVal = amount.toDoubleOrNull() ?: 0.0.takeIf { amount.isBlank() }
        if (costVal == null || !costVal.isFinite() || costVal < 0.0) {
            errorMessageRes = R.string.error_invalid_amount
            return
        }
        if (name.isBlank()) {
            errorMessageRes = R.string.yimu_template_name_required
            return
        }
        val transfer = transactionType == "TRANSFER"
        if (transfer && (transferToAssetId == null || transferToAssetId == assetId || !transferServiceCharge.isFinite())) {
            errorMessageRes = R.string.yimu_invalid_transfer
            return
        }
        if (liveAssets.none { it.assetId == assetId } || (transfer && liveAssets.none { it.assetId == transferToAssetId })) {
            errorMessageRes = R.string.yimu_select_account
            return
        }
        saving = true
        scope.launch {
            try {
                val tRepo = requireNotNull(templateRepo)
                val previous = originalTemplate
                val template = (previous ?: ModuleBillEntity(cost = costVal)).copy(
                    templateName = name.trim(),
                    moduleType = if (transfer) ModuleBillEntity.MODULE_TYPE_TRANSFER else ModuleBillEntity.MODULE_TYPE_BILL,
                    cost = costVal,
                    parentCategoryId = if (transfer) null else parentCategoryId,
                    childCategoryId = if (transfer) null else childCategoryId,
                    assetId = assetId,
                    fromAssetId = assetId,
                    toAssetId = if (transfer) requireNotNull(transferToAssetId) else (previous?.toAssetId ?: assetId),
                    serviceCharge = if (transfer) transferServiceCharge else 0.0,
                    remark = note.ifBlank { null },
                    tags = tag.ifBlank { null },
                    bookId = bookId,
                    moduleLock = templateLock && costVal > 0.0,
                    updateTime = System.currentTimeMillis(),
                )
                if (previous == null) tRepo.createTemplate(template) else tRepo.updateTemplate(template)
                onDone()
            } finally {
                saving = false
            }
        }
    }

    /** 删除当前正在编辑的模板，删除后交由调用方返回上一页。 */
    fun deleteCurrentTemplate(onDone: () -> Unit) {
        if (saving) return
        val previous = requireNotNull(originalTemplate) { "当前页面不是模板编辑" }
        saving = true
        scope.launch {
            try {
                requireNotNull(templateRepo).deleteTemplate(previous.moduleBillId)
                originalTemplate = null
                onDone()
            } finally {
                saving = false
            }
        }
    }

    private fun syncCategoryIds() {
        val resolved = CategoryMapping.resolve(category, subCategory.ifEmpty { null }, isIncome)
        parentCategoryId = resolved.parentId
        childCategoryId = resolved.childId
    }

    fun setType(income: Boolean) {
        transactionType = if (income) "INCOME" else "EXPENSE"
        if (isIncome == income) return
        isIncome = income
        subCategory = ""
        childCategoryId = null
        isCategoryManuallyChosen = true
        observeCategories()
        category = if (income) "收入" else "其他"
        parentCategoryId = if (income) 9 else 99
    }

    fun setAmountInput(raw: String) {
        amount = raw.filter { it.isDigit() || it == '.' }.take(12)
    }

    fun selectCategoryManually(catName: String, catId: Int? = null) {
        category = catName
        val parentId = catId ?: CategoryMapping.resolve(catName, null, isIncome).parentId
        parentCategoryId = parentId
        // 原版选中一级分类后默认落到第一个子分类，账单显示「食品餐饮-午餐」这类完整名称
        val firstChild = liveCategories.firstOrNull {
            it.parentId == parentId && it.categoryType == if (isIncome) 1 else 0
        }
        subCategory = firstChild?.categoryName.orEmpty()
        childCategoryId = firstChild?.categoryId
        isCategoryManuallyChosen = true
    }

    fun selectSubCategory(subName: String, subId: Int? = null) {
        if (subCategory == subName) {
            subCategory = ""
            childCategoryId = null
        } else {
            subCategory = subName
            childCategoryId = subId ?: CategoryMapping.resolve(category, subName, isIncome).childId
        }
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

    /** 快速自然文本智能识别填入（使用本地离线模型推断） */
    fun applyQuickText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return

        val modelService = com.nonen.Bookkeeping.core.nlp.ClassificationModelService.getInstance()
        if (modelService != null) {
            val res = modelService.parseQuickRecord(text)
            if (res.amount != null && res.amount > 0.0) {
                amount = String.format(Locale.US, "%.2f", res.amount)
            }
            if (res.isIncome != isIncome) {
                setType(res.isIncome)
            }
            category = res.category
            if (!res.subCategory.isNullOrBlank()) {
                subCategory = res.subCategory
            }
            note = res.remark
            try {
                timestamp = res.dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {}
            syncCategoryIds()
            isCategoryManuallyChosen = true
            autoSuggestedCategory = res.category
            return
        }

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
        if (saving) return
        val value = amount.toDoubleOrNull()
        if (value == null || value <= 0.0) {
            errorMessageRes = R.string.error_invalid_amount
            return
        }
        saving = true
        scope.launch {
            try {
            if (saveAsRule) {
                val keyword = merchant.trim().ifEmpty { note.trim() }
                if (keyword.isNotEmpty()) {
                    ruleRepo?.add(keyword, category, isIncome)
                }
            }
            val existing = original
            val currentParentId = parentCategoryId ?: CategoryMapping.resolve(category, subCategory.ifEmpty { null }, isIncome).parentId
            val currentChildId = childCategoryId ?: CategoryMapping.resolve(category, subCategory.ifEmpty { null }, isIncome).childId
            if (existing != null) {
                val updated = existing.copy(
                    amount = if (isIncome) value else -value,
                    category = category,
                    subCategory = subCategory.trim().ifEmpty { null },
                    parentCategoryId = currentParentId,
                    childCategoryId = currentChildId,
                    bookId = bookId,
                    assetId = assetId,
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
                    parentCategoryId = currentParentId,
                    childCategoryId = currentChildId,
                    bookId = bookId,
                    assetId = assetId,
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
                    repo.learnFromEdit(entity, null)
                    onDone()
                } else {
                    errorMessageRes = R.string.error_duplicate_record
                }
            }
            } finally {
                saving = false
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val existing = original ?: return
        scope.launch {
            repo.delete(existing)
            onDone()
        }
    }
}
