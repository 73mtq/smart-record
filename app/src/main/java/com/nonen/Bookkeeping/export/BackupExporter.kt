package com.nonen.Bookkeeping.export

import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.core.MiniXlsx
import com.nonen.Bookkeeping.data.db.TransactionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Excel 账单备份导出与分析生成器。
 * 格式专为人类视觉查阅与大语言模型（AI）直接进行财务习惯诊断而设计。
 *
 * Sheet 1: 账单明细（首工作表严格保持列顺序，供系统原样无损回灌，去重校验码保障幂等）
 * Sheet 2: 收支统计汇总（汇总全局指标、支出占比排行榜、收入性质构成以及 AI 财务分析指南）
 */
object BackupExporter {

    // 一木记账 23 列标准表头常量
    val YIMU_HEADERS = listOf(
        "日期", "收支类型", "金额", "类别", "二级分类",
        "账户", "账本", "退款", "优惠", "备注",
        "标签", "报销账户", "报销金额", "报销明细", "多币种",
        "地址", "创建用户", "其他", "附件1", "附件2",
        "附件3", "附件4", "附件5",
    )

    const val HEADER_TIME = "记账时间"
    const val HEADER_TYPE = "类型"
    const val HEADER_AMOUNT = "金额"
    const val HEADER_CATEGORY = "分类"
    const val HEADER_MERCHANT = "商户/交易对象"
    const val HEADER_NOTE = "备注"
    const val HEADER_SOURCE = "来源"
    const val HEADER_HASH = "校验码"

    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    fun resolveCategoryHierarchy(category: String, isIncome: Boolean): Pair<String, String> {
        if (isIncome) {
            val sub = when (category) {
                "其他收入" -> "其他"
                else -> category
            }
            return "收入" to sub
        }
        return when (category) {
            "餐饮" -> "食品餐饮" to "餐饮"
            "交通" -> "出行交通" to "交通"
            "购物" -> "购物消费" to "购物"
            "居住" -> "居家生活" to "居住"
            "娱乐" -> "休闲娱乐" to "娱乐"
            "医疗" -> "健康医疗" to "医疗"
            "服饰美容" -> "购物消费" to "服饰美容"
            "旅行" -> "休闲娱乐" to "旅游度假"
            "通讯" -> "居家生活" to "话费宽带"
            "教育" -> "文化教育" to "教育"
            "人情" -> "送礼人情" to "人情"
            "金融" -> "其他" to "理财支出"
            "宠物" -> "居家生活" to "宠物"
            "母婴" -> "购物消费" to "母婴玩具"
            "其他" -> "其他" to "其他"
            else -> "其他" to category
        }
    }

    fun resolveAccount(source: String): String = when (source.lowercase(Locale.ROOT)) {
        "wechat" -> "微信支付"
        "alipay" -> "支付宝"
        "bank" -> "银行卡"
        "manual" -> "现金/手动"
        else -> source.ifBlank { "日常账户" }
    }

    fun buildXlsx(transactions: List<TransactionEntity>): ByteArray {
        val detailRows: List<List<Any?>> = buildList(transactions.size + 1) {
            add(YIMU_HEADERS)
            transactions.forEach { t ->
                val isIncome = t.amount >= 0
                val (mainCat, subCat) = resolveCategoryHierarchy(t.category, isIncome)
                val amt = if (isIncome) abs(t.amount) else -abs(t.amount)
                val otherVal = if (t.merchant.isNullOrBlank()) t.hash else "${t.hash}|${t.merchant}"
                add(
                    listOf(
                        timeFormat.format(Instant.ofEpochMilli(t.timestamp).atZone(ZoneId.systemDefault())),
                        if (isIncome) "收入" else "支出",
                        amt,
                        mainCat,
                        subCat,
                        resolveAccount(t.source),
                        "日常账本",
                        "",
                        "",
                        t.note.orEmpty(),
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        otherVal,
                        "",
                        "",
                        "",
                        "",
                        "",
                    ),
                )
            }
        }

        // 计算财务汇总数据，供人类与 AI 直接进行直观分析
        var totalExpense = 0.0
        var totalIncome = 0.0
        val expenseCats = HashMap<String, Double>()
        val expenseCounts = HashMap<String, Int>()
        val incomeCats = HashMap<String, Double>()
        val incomeCounts = HashMap<String, Int>()

        transactions.forEach { t ->
            val v = abs(t.amount)
            if (t.amount < 0) {
                totalExpense += v
                expenseCats[t.category] = (expenseCats[t.category] ?: 0.0) + v
                expenseCounts[t.category] = (expenseCounts[t.category] ?: 0) + 1
            } else {
                totalIncome += v
                incomeCats[t.category] = (incomeCats[t.category] ?: 0.0) + v
                incomeCounts[t.category] = (incomeCounts[t.category] ?: 0) + 1
            }
        }
        val netBalance = totalIncome - totalExpense

        val summaryRows: List<List<Any?>> = buildList {
            add(listOf("【个人财务统计汇总分析】", "", "", ""))
            add(listOf("导出时间", timeFormat.format(Instant.now().atZone(ZoneId.systemDefault())), "", ""))
            add(listOf("账单总笔数", transactions.size, "笔", ""))
            add(listOf("累计总支出", String.format(Locale.US, "%.2f", totalExpense), "元", ""))
            add(listOf("累计总收入", String.format(Locale.US, "%.2f", totalIncome), "元", ""))
            add(listOf("累计净结余", String.format(Locale.US, "%.2f", netBalance), "元", ""))
            add(emptyList())

            add(listOf("【支出分类占比排行榜】", "", "", ""))
            add(listOf("支出分类", "金额(元)", "笔数", "支出占比"))
            val sortedExpenses = expenseCats.entries.sortedByDescending { it.value }
            if (sortedExpenses.isEmpty()) {
                add(listOf("暂无支出记录", 0.0, 0, "0.0%"))
            } else {
                sortedExpenses.forEach { (cat, amt) ->
                    val pct = if (totalExpense > 0) (amt / totalExpense * 100) else 0.0
                    add(listOf(cat, String.format(Locale.US, "%.2f", amt), expenseCounts[cat] ?: 0, String.format(Locale.US, "%.1f%%", pct)))
                }
            }
            add(emptyList())

            add(listOf("【收入来源构成排行榜（钱从哪来）】", "", "", ""))
            add(listOf("收入分类", "金额(元)", "笔数", "收入占比", "收入性质"))
            val sortedIncomes = incomeCats.entries.sortedByDescending { it.value }
            if (sortedIncomes.isEmpty()) {
                add(listOf("暂无收入记录", 0.0, 0, "0.0%", "无"))
            } else {
                sortedIncomes.forEach { (cat, amt) ->
                    val pct = if (totalIncome > 0) (amt / totalIncome * 100) else 0.0
                    val group = Categories.incomeGroupOf(cat).displayName
                    add(listOf(cat, String.format(Locale.US, "%.2f", amt), incomeCounts[cat] ?: 0, String.format(Locale.US, "%.1f%%", pct), group))
                }
            }
            add(emptyList())

            add(listOf("【AI 财务分析与诊断提示】", "", "", ""))
            add(listOf("数据说明", "首表「账单明细」为流水原表；本表为分类聚合，已按家庭支持、奖助学金与劳动收入进行结构化归类。"))
            add(listOf("分析建议", "请结合生活费与奖学金等非劳动收入比例评估结余韧性，并对餐饮、购物、娱乐等弹性开支提出改进建议。"))
        }

        return MiniXlsx.writeSheets(
            listOf(
                MiniXlsx.Sheet("账单", detailRows),
                MiniXlsx.Sheet("统计汇总", summaryRows),
            ),
        )
    }
}
