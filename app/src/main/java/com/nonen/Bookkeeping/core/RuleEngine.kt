package com.nonen.Bookkeeping.core

import com.nonen.Bookkeeping.data.db.CategoryRuleDao
import com.nonen.Bookkeeping.data.db.CategoryRuleEntity
import com.nonen.Bookkeeping.data.db.MerchantCategoryDao
import com.nonen.Bookkeeping.data.db.MerchantCategoryEntity
import com.nonen.Bookkeeping.core.nlp.ClassificationModelService
import java.util.Locale

/**
 * 分类依据快照：一次查库取全部依据，供批量 import 逐行复用。
 *
 * 逐行调用 categorize 会在每行都查一次规则表（导入 3000 行 = 3000 次全表读），
 * 故批量场景先用 [RuleEngine.loadRules] 取快照，再交给纯函数匹配。
 */
class CategorizationRules(
    val rules: List<CategoryRuleEntity>,
    val merchants: List<MerchantCategoryEntity>,
)

/**
 * 自动分类规则引擎。
 *
 * 命中即返回的优先链（越靠前越权威）：
 * 1. **商户记忆**——用户曾亲手把这个商户归到某分类，最具体也最可信；
 * 2. **自定义关键词规则**——用户在规则页显式写下的映射，同样属于用户意志；
 * 3. **平台自带分类**——支付宝「交易分类」/微信「交易类型」，覆盖面广但较泛，
 *    因此排在用户显式配置之后；
 * 4. **本地模型推断**——离线 SVM 模型达到既定置信度后的分类；
 * 5. **内置关键词规则**——预置的 500+ 条商户/场景词；
 * 6. 兜底「其他」。
 *
 * 每一层都用方向过滤（收入只落收入分类，反之亦然），避免「工资」被归进支出类。
 */
class RuleEngine(
    private val ruleDao: CategoryRuleDao,
    private val merchantDao: MerchantCategoryDao,
) {

    /** 取一次分类依据快照（批量导入等逐行场景务必复用） */
    suspend fun loadRules(): CategorizationRules =
        CategorizationRules(rules = ruleDao.getAll(), merchants = merchantDao.getAll())

    /** 单次分类（内部查库；批量场景请改用 [loadRules] + [classify]） */
    suspend fun categorize(
        text: String?,
        isIncome: Boolean,
        merchant: String? = null,
        platformCategory: String? = null,
    ): String = classify(
        text = text,
        isIncome = isIncome,
        rules = loadRules(),
        merchant = merchant,
        platformCategory = platformCategory,
    )

    data class MatchDetail(
        val category: String,
        val levelName: String,
        val matchedRule: String?,
    )

    companion object {

        /**
         * 分类优先链匹配并附带详细层级结果（供规则测试演练场与调试使用）。
         */
        fun classifyDetail(
            text: String?,
            isIncome: Boolean,
            rules: CategorizationRules,
            merchant: String? = null,
            platformCategory: String? = null,
        ): MatchDetail {
            val fallback = if (isIncome) Categories.OTHER_INCOME else Categories.OTHER_EXPENSE
            val allowed = PlatformCategories.allowedCategories(isIncome)

            // 1. 商户记忆
            val merchantKey = MerchantKey.normalize(merchant)
            if (merchantKey != null) {
                val hit = rules.merchants.firstOrNull {
                    it.isIncome == isIncome &&
                        it.category in allowed &&
                        MerchantKey.matches(it.merchantKey, merchantKey)
                }
                if (hit != null) return MatchDetail(hit.category, "商户记忆", hit.merchantKey)
            }

            val body = text.orEmpty()
            if (body.isNotBlank()) {
                matchKeyword(body, rules.rules, allowed, isCustom = true)?.let { return it }
            }

            PlatformCategories.resolve(platformCategory, isIncome)
                ?.takeIf { it in allowed }
                ?.let { return MatchDetail(it, "平台分类", platformCategory) }

            if (body.isNotBlank()) {
                matchLocalModel(body, isIncome, allowed)?.let { return it }
                matchKeyword(body, rules.rules, allowed, isCustom = false)?.let { return it }
            }

            return MatchDetail(fallback, "兜底分类", null)
        }

        private fun matchKeyword(
            text: String,
            rules: List<CategoryRuleEntity>,
            allowed: Set<String>,
            isCustom: Boolean,
        ): MatchDetail? {
            val rule = rules.firstOrNull {
                it.isCustom == isCustom && it.category in allowed && text.contains(it.keyword, ignoreCase = true)
            } ?: return null
            return MatchDetail(rule.category, if (isCustom) "自定义规则" else "内置规则", rule.keyword)
        }

        private fun matchLocalModel(text: String, isIncome: Boolean, allowed: Set<String>): MatchDetail? {
            val model = ClassificationModelService.getInstance() ?: return null
            val top = model.predict(text).maxByOrNull { it.value }
                ?.takeIf { it.value >= 0.15 } ?: return null
            val parts = top.key.split("-")
            val mapped = CategoryMapping.resolve(parts.first(), parts.getOrNull(1), isIncome)
            if (mapped.parentName !in allowed) return null
            return MatchDetail(
                mapped.parentName, "本地模型推断", "${top.key}:${String.format(Locale.US, "%.2f", top.value)}",
            )
        }

        /**
         * 分类优先链的纯函数实现（无 IO，便于单测覆盖各层与层间覆盖关系）。
         */
        fun classify(
            text: String?,
            isIncome: Boolean,
            rules: CategorizationRules,
            merchant: String? = null,
            platformCategory: String? = null,
        ): String = classifyDetail(text, isIncome, rules, merchant, platformCategory).category

        /**
         * 仅关键词匹配（自定义优先于内置），不含商户记忆与平台分类。
         * 保留给「重新分类」等只需要词表语义的场景与既有单测。
         */
        fun matchRules(
            text: String?,
            isIncome: Boolean,
            rules: List<CategoryRuleEntity>,
        ): String = classify(
            text = text,
            isIncome = isIncome,
            rules = CategorizationRules(rules = rules, merchants = emptyList()),
        )
    }
}
