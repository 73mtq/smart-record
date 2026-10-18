package com.nonen.Bookkeeping.parse

import com.nonen.Bookkeeping.export.BackupExporter

/** 本应用导出的 Excel 备份解析出的一行。 */
data class BackupRow(
    val timestamp: Long? = null,
    val amount: Double? = null,
    val isIncome: Boolean = false,
    /** 类型列缺失/非法时为 false，按失败行计 */
    val typeValid: Boolean = true,
    val category: String? = null,
    val subCategory: String? = null,
    val merchant: String? = null,
    val note: String? = null,
    val source: String? = null,
    val account: String? = null,
    val tag: String? = null,
    val refund: String? = null,
    /** 导出时的去重哈希，原样回灌保证「导出→导入」幂等 */
    val hash: String? = null,
)

/**
 * 本应用 Excel 备份解析器：表头按列名定位，导入回灌用。
 * 时间列兼容文本（yyyy-MM-dd HH:mm:ss 等）与被 Excel 重存后的日期序列号。
 */
object BackupExcelParser {

    private fun mapToInternalCategory(mainCat: String?, subCat: String?, isIncome: Boolean): String {
        val s = subCat?.trim().orEmpty()
        val m = mainCat?.trim().orEmpty()
        if (s.isNotEmpty()) {
            if (s in com.nonen.Bookkeeping.core.Categories.allCategories) return s
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
            if (m in com.nonen.Bookkeeping.core.Categories.allCategories) return m
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

    private fun mapAccountToSource(account: String?): String = when (account?.trim()) {
        "微信支付", "微信" -> "wechat"
        "支付宝" -> "alipay"
        "银行卡" -> "bank"
        "现金/手动", "手动" -> "manual"
        else -> account?.trim().orEmpty().ifBlank { "excel" }
    }

    /** @return null 表示不是本应用或一木记账的备份格式（找不到表头） */
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
        val cMerchant = col(BackupExporter.HEADER_MERCHANT, "商户")
        val cNote = col("备注", BackupExporter.HEADER_NOTE)
        val cAccount = col("账户", BackupExporter.HEADER_SOURCE, "来源")
        val cRefund = col("退款")
        val cTag = col("标签")
        val cHash = col("其他", BackupExporter.HEADER_HASH, "校验码")

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
                val amount = amtDouble?.let { kotlin.math.abs(it) }?.takeIf { it > 0.0 }
                val isIncome = when {
                    type == "收入" -> true
                    type == "支出" -> false
                    amtDouble != null && amtDouble > 0.0 -> true
                    else -> false
                }
                val typeValid = type in setOf("收入", "支出") || (type.isEmpty() && amtDouble != null)

                val mainCategory = if (cCategory >= 0) row.getOrNull(cCategory)?.trim()?.ifBlank { null } else null
                val subCategory = if (cSubCategory >= 0) row.getOrNull(cSubCategory)?.trim()?.ifBlank { null } else null
                val finalCategory = mapToInternalCategory(mainCategory, subCategory, isIncome)

                val rawOther = if (cHash >= 0) row.getOrNull(cHash)?.trim()?.ifBlank { null } else null
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
                val source = mapAccountToSource(rawAccount)
                val rawRefund = if (cRefund >= 0) row.getOrNull(cRefund)?.trim()?.ifBlank { null } else null
                val rawTag = if (cTag >= 0) row.getOrNull(cTag)?.trim()?.ifBlank { null } else null

                val timestamp = CsvSupport.parseFlexibleTimestamp(row.getOrNull(cTime))

                val hash = explicitHash ?: if (timestamp != null && amount != null) {
                    val signed = if (isIncome) amount else -amount
                    com.nonen.Bookkeeping.core.HashUtil.transactionHash(timestamp, signed, merchant ?: note, source)
                } else null

                BackupRow(
                    timestamp = timestamp,
                    amount = amount,
                    isIncome = isIncome,
                    typeValid = typeValid,
                    category = finalCategory,
                    subCategory = subCategory,
                    merchant = merchant,
                    note = note,
                    source = source,
                    account = rawAccount,
                    tag = rawTag,
                    refund = rawRefund,
                    hash = hash,
                )
            }
    }
}
