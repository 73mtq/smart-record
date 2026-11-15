package com.nonen.Bookkeeping.ui.components

import com.nonen.Bookkeeping.data.db.CategoryRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 规则页行数据：原版 CategoryKeyManagerActivity.a0() 只读取自定义 AIType 并按类别成行。 */
class YimuAiTypeNativeViewTest {

    @Test
    fun `只保留自定义规则并按类别聚合关键词`() {
        val rows = aiTypeRows(
            listOf(
                CategoryRuleEntity(id = 1, keyword = "晚饭", category = "餐饮", isCustom = true),
                CategoryRuleEntity(id = 2, keyword = "内置词", category = "餐饮", isCustom = false),
                CategoryRuleEntity(id = 3, keyword = "外卖", category = "餐饮", isCustom = true),
                CategoryRuleEntity(id = 4, keyword = "生活费", category = "生活费", isCustom = true),
            ),
        )
        assertEquals(2, rows.size)
        assertEquals("餐饮", rows[0].category)
        assertEquals(listOf("晚饭", "外卖"), rows[0].keywords)
        assertFalse(rows[0].isIncome)
        assertEquals("生活费", rows[1].category)
        assertEquals(listOf("生活费"), rows[1].keywords)
        assertTrue(rows[1].isIncome)
    }
}
