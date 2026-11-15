package com.nonen.Bookkeeping.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 原版 ActivityMonthView.y()/v() 与 CustomWeekView.y()/v() 的绘制几何。
 * 固定 y=0、mItemHeight=252、mTextBaseLine=183，逐项锁住原版公式的结果。
 */
class YimuCalendarGeometryTest {

    private val textBaseline = 183f
    private val itemHeight = 252

    @Test
    fun `日号基线等于原版公式`() {
        // ActivityMonthView.java:95/111：mTextBaseLine + (y - mItemHeight / 4)
        assertEquals(120f, dayTextBaseline(textBaseline, 0, itemHeight), 0f)
    }

    @Test
    fun `无账单时农历基线等于原版公式`() {
        // ActivityMonthView.java:118：mTextBaseLine + y + mItemHeight / 10
        assertEquals(208f, lunarTextBaseline(textBaseline, 0, itemHeight), 0f)
    }

    @Test
    fun `有账单时支出与收入行基线等于原版公式`() {
        // ActivityMonthView.java:124/126：支出 mTextBaseLine + y，收入再加 mItemHeight / 5
        assertEquals(183f, payTextBaseline(textBaseline, 0), 0f)
        assertEquals(233f, incomeTextBaseline(textBaseline, 0, itemHeight), 0f)
    }

    @Test
    fun `选中圆角矩形边界等于原版公式`() {
        // ActivityMonthView.java:218：x+3dp, y+3dp, x+mItemWidth-3dp, y+mItemHeight-3dp
        val rect = selectedRoundRect(x = 0, y = 0, itemWidth = 108, itemHeight = itemHeight, padding = 3f)
        assertEquals(3f, rect.left, 0f)
        assertEquals(3f, rect.top, 0f)
        assertEquals(105f, rect.right, 0f)
        assertEquals(249f, rect.bottom, 0f)
    }

    @Test
    fun `非零行的日号与选中矩形按原版公式计算`() {
        // 第二行 y=252：日号 183 + (252 - 63) = 372；矩形上边 252 + 3 = 255，下边 504 - 3 = 501
        assertEquals(372f, dayTextBaseline(textBaseline, itemHeight, itemHeight), 0f)
        val rect = selectedRoundRect(x = 0, y = itemHeight, itemWidth = 108, itemHeight = itemHeight, padding = 3f)
        assertEquals(255f, rect.top, 0f)
        assertEquals(501f, rect.bottom, 0f)
    }

    @Test
    fun `周视图基线与选中矩形和月视图在 y 为 0 时一致`() {
        // CustomWeekView.java:101/107/111/112/123/128/130：y 恒为 0，农历无额外偏移
        assertEquals(120f, dayTextBaseline(textBaseline, 0, itemHeight), 0f)
        assertEquals(208f, lunarTextBaseline(textBaseline, 0, itemHeight), 0f)
        assertEquals(183f, payTextBaseline(textBaseline, 0), 0f)
        assertEquals(233f, incomeTextBaseline(textBaseline, 0, itemHeight), 0f)
        val rect = selectedRoundRect(x = 0, y = 0, itemWidth = 108, itemHeight = itemHeight, padding = 3f)
        assertEquals(3f, rect.left, 0f)
        assertEquals(3f, rect.top, 0f)
        assertEquals(105f, rect.right, 0f)
        assertEquals(249f, rect.bottom, 0f)
    }

    @Test
    fun `行高按原版整除计算`() {
        // Java 整数除法：253 / 4 = 63，253 / 10 = 25，253 / 5 = 50
        assertEquals(120f, dayTextBaseline(textBaseline, 0, 253), 0f)
        assertEquals(208f, lunarTextBaseline(textBaseline, 0, 253), 0f)
        assertEquals(233f, incomeTextBaseline(textBaseline, 0, 253), 0f)
    }
}
