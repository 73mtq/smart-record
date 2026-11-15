package com.nonen.Bookkeeping.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** 预算趋势图 x 轴：原版 BudgetManagerActivity.B0()/g 的当月天数与奇偶日标签。 */
class YimuBudgetNativeViewTest {

    @Test
    fun `数据点数量等于当月天数`() {
        assertEquals(28, budgetAxisDays(2026, 2))
        assertEquals(30, budgetAxisDays(2026, 11))
        assertEquals(31, budgetAxisDays(2026, 12))
    }

    @Test
    fun `2026 年 2 月只显示奇数日标签`() {
        assertEquals((1..27 step 2).map { it.toString() }, visibleLabels(2026, 2))
    }

    @Test
    fun `2026 年 11 月只显示奇数日标签`() {
        assertEquals((1..29 step 2).map { it.toString() }, visibleLabels(2026, 11))
    }

    @Test
    fun `2026 年 12 月只显示奇数日标签`() {
        assertEquals((1..31 step 2).map { it.toString() }, visibleLabels(2026, 12))
    }

    @Test
    fun `超过当月天数的刻度回绕到月初`() {
        assertEquals("1", budgetDayAxisLabel(30f, 1, 30))
    }

    @Test
    fun `日均与日预算按已过天数和剩余天数计算`() {
        assertEquals(BudgetDays(13, 18), budgetDays(2026, 11, java.time.LocalDate.of(2026, 11, 13)))
        assertEquals(BudgetDays(30, 1), budgetDays(2026, 11, java.time.LocalDate.of(2026, 11, 30)))
        assertEquals(BudgetDays(31, 0), budgetDays(2026, 10, java.time.LocalDate.of(2026, 11, 13)))
        assertEquals(BudgetDays(0, 31), budgetDays(2026, 12, java.time.LocalDate.of(2026, 11, 13)))
    }

    @Test
    fun `预算进度按剩余比例截断`() {
        assertEquals(60, budgetRemainingPercent(1000.0, 400.0))
        assertEquals(99, budgetRemainingPercent(300.0, 1.0))
        assertEquals(0, budgetRemainingPercent(1000.0, 1000.0))
        assertEquals(0, budgetRemainingPercent(1000.0, 1200.0))
        assertEquals(0, budgetRemainingPercent(0.0, 0.0))
    }

    private fun visibleLabels(year: Int, month: Int): List<String> {
        val days = budgetAxisDays(year, month)
        return (0 until days)
            .map { budgetDayAxisLabel(it.toFloat(), 1, days) }
            .filter { it.isNotEmpty() }
    }
}
