package com.nonen.Bookkeeping.stats

import com.nonen.Bookkeeping.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarStatsTest {

    private val zone = ZoneId.systemDefault()

    private fun epochMillisOf(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `test computeCalendarMonth with empty transactions produces full month days`() {
        val ym = YearMonth.of(2026, 10)
        val summaries = StatsCalculator.computeCalendarMonth(ym, emptyList(), zone)

        assertEquals(31, summaries.size)
        assertEquals(LocalDate.of(2026, 10, 1), summaries.first().date)
        assertEquals(LocalDate.of(2026, 10, 31), summaries.last().date)
        assertEquals(0.0, summaries.sumOf { it.totalExpense }, 0.001)
        assertEquals(0.0, summaries.sumOf { it.totalIncome }, 0.001)
        assertEquals(0, summaries.sumOf { it.transactionCount })
    }

    @Test
    fun `test computeCalendarMonth aggregates daily expenses and incomes with preview category`() {
        val ym = YearMonth.of(2026, 10)
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                amount = -35.0,
                category = "餐饮",
                subCategory = "午餐",
                source = "manual",
                hash = "h_1",
                timestamp = epochMillisOf(2026, 10, 5, 12),
            ),
            TransactionEntity(
                id = 2,
                amount = -120.0,
                category = "交通",
                subCategory = "打车",
                source = "manual",
                hash = "h_2",
                timestamp = epochMillisOf(2026, 10, 5, 18),
            ),
            TransactionEntity(
                id = 3,
                amount = 2000.0,
                category = "收入",
                subCategory = "奖金",
                source = "manual",
                hash = "h_3",
                timestamp = epochMillisOf(2026, 10, 5, 9),
            ),
            TransactionEntity(
                id = 4,
                amount = -50.0,
                category = "购物",
                source = "manual",
                hash = "h_4",
                timestamp = epochMillisOf(2026, 10, 20, 10),
            ),

        )

        val summaries = StatsCalculator.computeCalendarMonth(ym, transactions, zone)
        val day5 = summaries.find { it.date.dayOfMonth == 5 }
        assertNotNull(day5)
        assertEquals(155.0, day5!!.totalExpense, 0.001)
        assertEquals(2000.0, day5.totalIncome, 0.001)
        assertEquals(3, day5.transactionCount)
        // 预览分类应为单笔支出最大的分类（交通）
        assertEquals("交通", day5.previewCategory)

        val day20 = summaries.find { it.date.dayOfMonth == 20 }
        assertNotNull(day20)
        assertEquals(50.0, day20!!.totalExpense, 0.001)
        assertEquals(0.0, day20.totalIncome, 0.001)
        assertEquals(1, day20.transactionCount)
        assertEquals("购物", day20.previewCategory)
    }

    @Test
    fun `test computeCalendarMonth leap year February boundary`() {
        val leapYm = YearMonth.of(2024, 2)
        val leapSummaries = StatsCalculator.computeCalendarMonth(leapYm, emptyList(), zone)
        assertEquals(29, leapSummaries.size)

        val normalYm = YearMonth.of(2023, 2)
        val normalSummaries = StatsCalculator.computeCalendarMonth(normalYm, emptyList(), zone)
        assertEquals(28, normalSummaries.size)
    }
}
