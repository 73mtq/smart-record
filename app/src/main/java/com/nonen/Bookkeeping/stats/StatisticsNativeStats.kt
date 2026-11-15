package com.nonen.Bookkeeping.stats

import com.nonen.Bookkeeping.data.db.TagEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.TransactionTagCrossRef
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneId

/**
 * 统计页标签行：原版 entity.TagParentType 的字段集合。
 * 同一笔账单挂多个标签时，每个标签各计一笔、各计全额；同一标签下同一账单只计一次。
 */
data class TagStat(
    val tagId: Long,
    val tagName: String,
    val billCount: Int,
    val pay: Double,
    val income: Double,
)

/** 统计页分类数据行：原版 entity.BillParentType 中列表渲染需要的字段。 */
data class CategoryRow(
    val name: String,
    val amount: Double,
    val count: Int,
    val percent: Double,
    val rate: Double,
    val isIncome: Boolean,
    /** 分类图标资源名，由 UI 层按分类目录填充 */
    val iconName: String = "",
)

/** 统计页报表行：原版 entity.DateForm 中列表渲染需要的字段。 */
data class DayFormRow(
    val label: String,
    val income: Double,
    val pay: Double,
    val balance: Double,
)

/**
 * 按标签聚合账单金额与笔数，照原版 i4.D0 的行为：
 * 支出账单累加到 pay、收入账单累加到 income，金额取绝对值。
 * 结果按笔数降序、金额降序排列，与原版 TagParentType.compareTo 一致。
 */
fun aggregateTagStats(
    transactions: List<TransactionEntity>,
    tags: List<TagEntity>,
    refs: List<TransactionTagCrossRef>,
): List<TagStat> {
    if (transactions.isEmpty() || tags.isEmpty() || refs.isEmpty()) return emptyList()
    val nameById = tags.associate { it.tagId to it.tagName }
    val tagIdsByTransaction = refs.groupBy({ it.transactionId }, { it.tagId })
    val billCounts = LinkedHashMap<Long, Int>()
    val paySums = LinkedHashMap<Long, Double>()
    val incomeSums = LinkedHashMap<Long, Double>()
    for (transaction in transactions) {
        val tagIds = tagIdsByTransaction[transaction.id] ?: continue
        for (tagId in tagIds.toSet()) {
            if (!nameById.containsKey(tagId)) continue
            billCounts[tagId] = (billCounts[tagId] ?: 0) + 1
            if (transaction.amount < 0.0) {
                paySums[tagId] = (paySums[tagId] ?: 0.0) - transaction.amount
            } else {
                incomeSums[tagId] = (incomeSums[tagId] ?: 0.0) + transaction.amount
            }
        }
    }
    return billCounts.map { (tagId, count) ->
        TagStat(
            tagId = tagId,
            tagName = nameById.getValue(tagId),
            billCount = count,
            pay = paySums[tagId] ?: 0.0,
            income = incomeSums[tagId] ?: 0.0,
        )
    }.sortedWith(
        compareByDescending<TagStat> { it.billCount }.thenByDescending { it.pay + it.income },
    )
}

/**
 * 按一级分类聚合账单，生成统计页「分类数据」列表。
 *
 * @param transactions 区间内账单
 * @param isIncome true 聚合收入分类，false 聚合支出分类
 */
fun aggregateCategoryRows(
    transactions: List<TransactionEntity>,
    isIncome: Boolean,
    nameOf: (TransactionEntity) -> String = { it.category },
): List<CategoryRow> {
    val rows = LinkedHashMap<String, Pair<Double, Int>>()
    for (transaction in transactions) {
        val matches = if (isIncome) transaction.amount > 0.0 else transaction.amount < 0.0
        if (!matches) continue
        val name = nameOf(transaction)
        val current = rows[name]
        val amount = current?.first ?: 0.0
        val count = current?.second ?: 0
        rows[name] = (amount + kotlin.math.abs(transaction.amount)) to (count + 1)
    }
    val total = rows.values.sumOf { it.first }
    return rows.map { (name, value) ->
        CategoryRow(
            name = name,
            amount = value.first,
            count = value.second,
            percent = if (total > 0.0) value.first / total * 100.0 else 0.0,
            rate = 0.0,
            isIncome = isIncome,
        )
    }.sortedByDescending { it.amount }
}

/**
 * 按年月聚合收支，生成年度统计页「报表统计」列表；月份文本与原版一致（M月），按月份降序排列。
 */
fun aggregateMonthForms(
    transactions: List<TransactionEntity>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<DayFormRow> {
    if (transactions.isEmpty()) return emptyList()
    val grouped = LinkedHashMap<Pair<Int, Int>, DoubleArray>()
    for (transaction in transactions) {
        val date = Instant.ofEpochMilli(transaction.timestamp).atZone(zone).toLocalDate()
        val values = grouped.getOrPut(date.year to date.monthValue) { DoubleArray(2) }
        if (transaction.amount < 0.0) values[1] -= transaction.amount else values[0] += transaction.amount
    }
    return grouped.entries
        .sortedByDescending { it.key.first * 100 + it.key.second }
        .map { (key, values) ->
            DayFormRow(
                label = key.second.toString() + "月",
                income = values[0],
                pay = values[1],
                balance = values[0] - values[1],
            )
        }
}

/**
 * 按日聚合收支，生成统计页「报表统计」列表；日期格式与原版一致（MM-dd），
 * 只保留有收支的日期，按日期降序排列。
 */
fun aggregateDayForms(
    transactions: List<TransactionEntity>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<DayFormRow> {
    if (transactions.isEmpty()) return emptyList()
    val formatter = DateTimeFormatter.ofPattern("MM-dd")
    val grouped = LinkedHashMap<LocalDate, DoubleArray>()
    for (transaction in transactions) {
        val date = Instant.ofEpochMilli(transaction.timestamp).atZone(zone).toLocalDate()
        val values = grouped.getOrPut(date) { DoubleArray(2) }
        if (transaction.amount < 0.0) values[1] -= transaction.amount else values[0] += transaction.amount
    }
    return grouped.entries
        .sortedByDescending { it.key }
        .map { (date, values) ->
            DayFormRow(
                label = date.format(formatter),
                income = values[0],
                pay = values[1],
                balance = values[0] - values[1],
            )
        }
}
