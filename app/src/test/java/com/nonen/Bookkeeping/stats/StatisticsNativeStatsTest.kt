package com.nonen.Bookkeeping.stats

import com.nonen.Bookkeeping.data.db.TagEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.TransactionTagCrossRef
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsNativeStatsTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun bill(
        id: Long,
        amount: Double,
        category: String = "食品餐饮",
        day: Int = 2,
    ): TransactionEntity {
        val millis = LocalDate.of(2026, 11, day)
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        return TransactionEntity(
            id = id,
            amount = amount,
            category = category,
            timestamp = millis,
            source = "manual",
            hash = "hash-$id",
        )
    }

    @Test
    fun `标签聚合按笔数降序并区分收支`() {
        val transactions = listOf(
            bill(1, -10.0),
            bill(2, -20.0),
            bill(3, 100.0),
        )
        val tags = listOf(TagEntity(tagId = 7, tagName = "出差"), TagEntity(tagId = 8, tagName = "报销"))
        val refs = listOf(
            TransactionTagCrossRef(1, 7),
            TransactionTagCrossRef(2, 7),
            TransactionTagCrossRef(3, 8),
            TransactionTagCrossRef(3, 7),
        )

        val rows = aggregateTagStats(transactions, tags, refs)

        assertEquals(2, rows.size)
        assertEquals("出差", rows[0].tagName)
        assertEquals(3, rows[0].billCount)
        assertEquals(30.0, rows[0].pay, 0.001)
        assertEquals(100.0, rows[0].income, 0.001)
        assertEquals("报销", rows[1].tagName)
        assertEquals(1, rows[1].billCount)
        assertEquals(100.0, rows[1].income, 0.001)
    }

    @Test
    fun `同一账单重复标签关联只计一次`() {
        val transactions = listOf(bill(1, -10.0))
        val tags = listOf(TagEntity(tagId = 7, tagName = "出差"))
        val refs = listOf(TransactionTagCrossRef(1, 7), TransactionTagCrossRef(1, 7))

        val rows = aggregateTagStats(transactions, tags, refs)

        assertEquals(1, rows.size)
        assertEquals(1, rows[0].billCount)
        assertEquals(10.0, rows[0].pay, 0.001)
    }

    @Test
    fun `不存在的标签关联被忽略`() {
        val transactions = listOf(bill(1, -10.0))
        val tags = listOf(TagEntity(tagId = 7, tagName = "出差"))
        val refs = listOf(TransactionTagCrossRef(1, 99))

        assertEquals(emptyList<TagStat>(), aggregateTagStats(transactions, tags, refs))
    }

    @Test
    fun `分类聚合计百分比与笔数`() {
        val transactions = listOf(
            bill(1, -10.0, "食品餐饮"),
            bill(2, -30.0, "食品餐饮"),
            bill(3, -60.0, "出行交通"),
            bill(4, 200.0, "收入"),
        )

        val rows = aggregateCategoryRows(transactions, isIncome = false)

        assertEquals(2, rows.size)
        assertEquals("出行交通", rows[0].name)
        assertEquals(60.0, rows[0].amount, 0.001)
        assertEquals(60.0, rows[0].percent, 0.001)
        assertEquals("食品餐饮", rows[1].name)
        assertEquals(40.0, rows[1].amount, 0.001)
        assertEquals(2, rows[1].count)
        assertEquals(40.0, rows[1].percent, 0.001)
    }

    @Test
    fun `日报表按日期降序并计算结余`() {
        val transactions = listOf(
            bill(1, -10.0, day = 1),
            bill(2, 30.0, day = 1),
            bill(3, -5.0, day = 3),
        )

        val rows = aggregateDayForms(transactions, zone)

        assertEquals(2, rows.size)
        assertEquals("11-03", rows[0].label)
        assertEquals(-5.0, rows[0].balance, 0.001)
        assertEquals("11-01", rows[1].label)
        assertEquals(30.0, rows[1].income, 0.001)
        assertEquals(10.0, rows[1].pay, 0.001)
        assertEquals(20.0, rows[1].balance, 0.001)
    }
}
