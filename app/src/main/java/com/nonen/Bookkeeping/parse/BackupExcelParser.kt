package com.nonen.Bookkeeping.parse

import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.core.HashUtil
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.export.BackupExporter
import kotlin.math.abs

/**
 * 本应用导出的 Excel 备份或一木记账 23 列 Excel 解析出的一行标准记录。
 */
data class BackupRow(
    val timestamp: Long? = null,
    val amount: Double? = null,
    val isIncome: Boolean = false,
    /** 类型列缺失或数值非法时为 false，按失败行计 */
    val typeValid: Boolean = true,
    val category: String? = null,
    val subCategory: String? = null,
    val merchant: String? = null,
    val note: String? = null,
    val source: String? = null,
    val account: String? = null,
    val book: String? = null,
    val tag: String? = null,
    val refund: String? = null,
    val discount: String? = null,
    val reimbursementAccount: String? = null,
    val reimbursementAmount: String? = null,
    val reimbursementDetail: String? = null,
    val multiCurrency: String? = null,
    val address: String? = null,
    val createdUser: String? = null,
    val other: String? = null,
    val attachments: List<String> = emptyList(),
    /** 导出时的去重哈希，原样回灌保证「导出→导入」幂等 */
    val hash: String? = null,
)

/**
 * 23 列标准 Excel 与备份解析器：表头按列名动态定位，兼容 .xls、.xlsx 与 CSV。
 */
object BackupExcelParser {

    private val STANDARD_PRIMARY_CATEGORIES = setOf(
        "食品餐饮", "出行交通", "居家生活", "休闲娱乐", "文化教育",
        "健康医疗", "购物消费", "送礼人情", "其他", "收入",
    )

    fun mapToInternalCategory(mainCat: String?, subCat: String?, isIncome: Boolean): String {
        val s = subCat?.trim().orEmpty()
        val m = mainCat?.trim().orEmpty()
        if (s.isNotEmpty()) {
            if (s in Categories.allCategories) return s
            when (s) {
                "早餐", "午餐", "晚餐", "饮料酒水", "休闲零食", "生鲜食品", "粮油调味", "请客吃饭", "外卖", "小吃", "快餐" -> return "餐饮"
                "打车", "公共交通", "火车", "飞机", "停车费", "加油", "租车", "骑行" -> return "交通"
                "水费", "电费", "燃气费", "家政清洁", "房租", "物业" -> return "居住"
                "话费宽带", "话费", "宽带" -> return "通讯"
                "日常家居", "办公用品", "手机数码", "生活电器", "虚拟充值", "装修装饰", "配饰腕表" -> return "购物"
                "个护美妆", "服饰运动", "美妆", "衣服" -> return "服饰美容"
                "运动健身", "电影唱歌", "棋牌桌游", "足浴按摩", "聚会" -> return "娱乐"
                "旅游度假" -> return "旅行"
                "买药", "医院", "滋补保健" -> return "医疗"
                "书报杂志", "培训考试", "学费" -> return "教育"
                "礼物", "礼金人情" -> return if (isIncome) "红包" else "人情"
                "红包" -> return if (isIncome) "红包" else "人情"
                "理财支出" -> return "金融"
                "母婴玩具" -> return "母婴"
                "宠物" -> return "宠物"
                "生活费", "补贴" -> return "生活费"
                "奖学金" -> return "奖学金"
                "工资" -> return "工资"
                "兼职", "兼职劳务", "监考费" -> return "兼职"
                "退款" -> return "退款"
                "理财" -> return if (isIncome) "理财" else "金融"
                "软件服务", "AI/API", "云服务", "会员订阅" -> return "软件服务"
                "闲置出售", "卖书", "转卖" -> return "闲置出售"
                "平台收益" -> return "平台收益"
                "退税" -> return "退税"
            }
        }
        if (m.isNotEmpty()) {
            if (m in Categories.allCategories) return m
            when (m) {
                "食品餐饮" -> return "餐饮"
                "出行交通" -> return "交通"
                "居家生活" -> return "居住"
                "购物消费" -> return "购物"
                "休闲娱乐" -> return "娱乐"
                "健康医疗" -> return "医疗"
                "文化教育" -> return "教育"
                "送礼人情" -> return "人情"
                "软件服务" -> return "软件服务"
                "收入" -> return if (s.isNotBlank()) s else "其他收入"
            }
        }
        return if (s.isNotBlank()) s else if (m.isNotBlank()) m else if (isIncome) "其他收入" else "其他"
    }

    fun normalizeCategory(mainCat: String?, subCat: String?, isIncome: Boolean): Pair<String, String?> {
        val m = mainCat?.trim().orEmpty()
        val s = subCat?.trim().orEmpty().ifBlank { null }

        if (m in STANDARD_PRIMARY_CATEGORIES) {
            return m to s
        }

        val standardMain = when (m) {
            "餐饮" -> "食品餐饮"
            "交通" -> "出行交通"
            "居住", "通讯", "宠物" -> "居家生活"
            "购物", "服饰美容", "母婴" -> "购物消费"
            "娱乐", "旅行" -> "休闲娱乐"
            "医疗" -> "健康医疗"
            "教育" -> "文化教育"
            "人情" -> "送礼人情"
            "金融" -> "其他"
            "软件服务" -> "居家生活"
            "生活费", "奖学金", "工资", "兼职", "闲置出售", "平台收益", "退税", "理财" -> "收入"
            else -> if (m.isNotBlank()) m else if (isIncome) "收入" else "其他"
        }
        val finalSub = s ?: when (m) {
            "服饰美容" -> "服饰运动"
            "母婴" -> "母婴玩具"
            "通讯" -> "话费宽带"
            "宠物" -> "宠物用品"
            "旅行" -> "旅游度假"
            "生活费", "奖学金", "工资", "兼职", "闲置出售", "平台收益", "退税", "理财" -> m
            else -> null
        }
        return standardMain to finalSub
    }

    fun mapAccountToSource(account: String?): String = when (account?.trim()) {
        "微信支付", "微信" -> "wechat"
        "支付宝" -> "alipay"
        "银行卡" -> "bank"
        "现金/手动", "手动", "现金" -> "manual"
        else -> account?.trim().orEmpty().ifBlank { "excel" }
    }

    /**
     * @return null 表示无法定位到必要表头（非支持的账单格式）
     */
    fun parse(bytes: ByteArray): List<BackupRow>? {
        val table = XlsxSupport.readAnyTable(bytes)
        val headerIdx = table.indexOfFirst { row ->
            row.any { cell ->
                val text = cell?.trim().orEmpty()
                text == "日期" || text == BackupExporter.HEADER_TIME
            }
        }
        if (headerIdx < 0) return null
        val header = table[headerIdx].map { it?.trim().orEmpty() }

        fun col(vararg names: String): Int {
            for (name in names) {
                val idx = header.indexOf(name)
                if (idx >= 0) return idx
            }
            return -1
        }

        val cTime = col("日期", BackupExporter.HEADER_TIME)
        val cType = col("收支类型", BackupExporter.HEADER_TYPE)
        val cAmount = col("金额", BackupExporter.HEADER_AMOUNT)
        val cCategory = col("类别", BackupExporter.HEADER_CATEGORY)
        val cSubCategory = col("二级分类")
        val cAccount = col("账户", BackupExporter.HEADER_SOURCE, "来源")
        val cBook = col("账本")
        val cRefund = col("退款")
        val cDiscount = col("优惠")
        val cNote = col("备注", BackupExporter.HEADER_NOTE)
        val cTag = col("标签")
        val cReimburseAcc = col("报销账户")
        val cReimburseAmt = col("报销金额")
        val cReimburseDetail = col("报销明细")
        val cCurrency = col("多币种")
        val cAddress = col("地址")
        val cUser = col("创建用户")
        val cOther = col("其他", BackupExporter.HEADER_HASH, "校验码")
        val cMerchant = col(BackupExporter.HEADER_MERCHANT, "商户")

        val attachCols = listOf(
            col("附件1"), col("附件2"), col("附件3"), col("附件4"), col("附件5"),
        ).filter { it >= 0 }

        if (cTime < 0 || cType < 0 || cAmount < 0) return null

        return table.drop(headerIdx + 1)
            .filter { row -> row.any { !it.isNullOrBlank() } }
            .map { row ->
                val type = row.getOrNull(cType)?.trim().orEmpty()
                val rawAmtStr = row.getOrNull(cAmount)?.trim()
                    ?.replace("¥", "")
                    ?.replace("￥", "")
                    ?.replace(",", "")
                val amtDouble = rawAmtStr?.toDoubleOrNull()
                val amount = amtDouble?.let { abs(it) }?.takeIf { it > 0.0 }
                val isIncome = when {
                    type == "收入" -> true
                    type == "支出" -> false
                    amtDouble != null && amtDouble > 0.0 -> true
                    else -> false
                }
                val typeValid = type in setOf("收入", "支出") || (type.isEmpty() && amtDouble != null)

                val rawCategory = if (cCategory >= 0) row.getOrNull(cCategory)?.trim()?.ifBlank { null } else null
                val rawSubCategory = if (cSubCategory >= 0) row.getOrNull(cSubCategory)?.trim()?.ifBlank { null } else null
                val finalCategory = mapToInternalCategory(rawCategory, rawSubCategory, isIncome)

                val rawOther = if (cOther >= 0) row.getOrNull(cOther)?.trim()?.ifBlank { null } else null
                val (explicitHash, otherMerchant) = if (rawOther != null && rawOther.contains("|")) {
                    val parts = rawOther.split("|", limit = 2)
                    parts[0].ifBlank { null } to parts[1].ifBlank { null }
                } else {
                    rawOther to null
                }

                val explicitMerchant = if (cMerchant >= 0) row.getOrNull(cMerchant)?.trim()?.ifBlank { null } else null
                val merchant = explicitMerchant ?: otherMerchant
                val note = if (cNote >= 0) row.getOrNull(cNote)?.trim()?.ifBlank { null } else null
                val rawAccount = if (cAccount >= 0) row.getOrNull(cAccount)?.trim()?.ifBlank { null } else null
                val rawBook = if (cBook >= 0) row.getOrNull(cBook)?.trim()?.ifBlank { null } else null
                val source = mapAccountToSource(rawAccount)
                val rawRefund = if (cRefund >= 0) row.getOrNull(cRefund)?.trim()?.ifBlank { null } else null
                val rawDiscount = if (cDiscount >= 0) row.getOrNull(cDiscount)?.trim()?.ifBlank { null } else null
                val rawTag = if (cTag >= 0) row.getOrNull(cTag)?.trim()?.ifBlank { null } else null

                val timestamp = CsvSupport.parseFlexibleTimestamp(row.getOrNull(cTime))

                val hash = explicitHash ?: if (timestamp != null && amount != null) {
                    val signed = if (isIncome) amount else -amount
                    HashUtil.transactionHash(timestamp, signed, merchant ?: note, source)
                } else null

                val attachments = attachCols.mapNotNull { idx ->
                    row.getOrNull(idx)?.trim()?.ifBlank { null }
                }

                BackupRow(
                    timestamp = timestamp,
                    amount = amount,
                    isIncome = isIncome,
                    typeValid = typeValid,
                    category = finalCategory,
                    subCategory = rawSubCategory,
                    merchant = merchant,
                    note = note,
                    source = source,
                    account = rawAccount,
                    book = rawBook,
                    tag = rawTag,
                    refund = rawRefund,
                    discount = rawDiscount,
                    reimbursementAccount = if (cReimburseAcc >= 0) row.getOrNull(cReimburseAcc)?.trim()?.ifBlank { null } else null,
                    reimbursementAmount = if (cReimburseAmt >= 0) row.getOrNull(cReimburseAmt)?.trim()?.ifBlank { null } else null,
                    reimbursementDetail = if (cReimburseDetail >= 0) row.getOrNull(cReimburseDetail)?.trim()?.ifBlank { null } else null,
                    multiCurrency = if (cCurrency >= 0) row.getOrNull(cCurrency)?.trim()?.ifBlank { null } else null,
                    address = if (cAddress >= 0) row.getOrNull(cAddress)?.trim()?.ifBlank { null } else null,
                    createdUser = if (cUser >= 0) row.getOrNull(cUser)?.trim()?.ifBlank { null } else null,
                    other = rawOther,
                    attachments = attachments,
                    hash = hash,
                )
            }
    }

    /**
     * 将 BackupRow 映射为领域实体 TransactionEntity。
     * 自动匹配关联的 assetId、bookId、parentCategoryId、childCategoryId。
     */
    fun toEntity(
        row: BackupRow,
        assetNameToId: Map<String, Long> = emptyMap(),
        bookNameToId: Map<String, Long> = emptyMap(),
        categoryNameToIds: Map<String, Pair<Int, Int?>> = emptyMap(),
        defaultAssetId: Long = 1L,
        defaultBookId: Long = 1L,
    ): TransactionEntity? {
        val timestamp = row.timestamp ?: return null
        val amount = row.amount ?: return null
        if (!row.typeValid) return null

        val signed = if (row.isIncome) amount else -amount
        val source = row.source ?: "excel"
        val categoryName = row.category ?: if (row.isIncome) "收入" else "其他"
        val subCategoryName = row.subCategory

        val childCatIds = subCategoryName?.let { categoryNameToIds[it] }
        val standardParentName = when (categoryName) {
            "餐饮" -> "食品餐饮"
            "交通" -> "出行交通"
            "居住" -> "居家生活"
            "购物" -> "购物消费"
            "娱乐" -> "休闲娱乐"
            "医疗" -> "健康医疗"
            "教育" -> "文化教育"
            "人情" -> "送礼人情"
            else -> categoryName
        }
        val parentCatIds = categoryNameToIds[categoryName] ?: categoryNameToIds[standardParentName]

        val parentId = childCatIds?.first ?: parentCatIds?.first
        val childId = childCatIds?.second ?: parentCatIds?.second

        val assetId = row.account?.let { assetNameToId[it] } ?: defaultAssetId
        val bookId = row.book?.let { bookNameToId[it] } ?: defaultBookId

        val hash = row.hash ?: HashUtil.transactionHash(timestamp, signed, row.merchant ?: row.note, source)

        return TransactionEntity(
            amount = signed,
            category = categoryName,
            subCategory = subCategoryName,
            note = row.note,
            merchant = row.merchant,
            timestamp = timestamp,
            source = source,
            account = row.account,
            tag = row.tag,
            transactionType = if (!row.refund.isNullOrBlank()) "REFUND" else (if (row.isIncome) "INCOME" else "EXPENSE"),
            assetId = assetId,
            bookId = bookId,
            parentCategoryId = parentId,
            childCategoryId = childId,
            hash = hash,
        )
    }
}
