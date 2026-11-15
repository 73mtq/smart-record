package com.nonen.Bookkeeping.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class YimuCalendarHeightTest {

    @Test
    fun `周日起算的月份行数与原版 wrap_content 一致`() {
        assertEquals(5, calendarRows(2026, 11))
        assertEquals(4, calendarRows(2026, 2))
        assertEquals(6, calendarRows(2026, 5))
        assertEquals(5, calendarRows(2025, 12))
    }

    @Test
    fun `参考机 5 行月份高度等于实测 1028px`() {
        assertEquals(1028, calendarHeightPx(2026, 11, 3.25f))
    }

    @Test
    fun `四行与六行月份按 55dp 增减`() {
        assertEquals(849, calendarHeightPx(2026, 2, 3.25f))
        assertEquals(1207, calendarHeightPx(2026, 5, 3.25f))
    }

    @Test
    fun `任意月份行数落在四到六之间`() {
        for (year in 2024..2027) {
            for (month in 1..12) {
                val rows = calendarRows(year, month)
                assert(rows in 4..6) { "${year}-${month} 行数 $rows" }
            }
        }
    }
}
