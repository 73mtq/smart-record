package com.nonen.Bookkeeping.data.migration

import com.nonen.Bookkeeping.core.CategoryMapping
import com.nonen.Bookkeeping.core.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryMigrationTest {

    @Test
    fun `expense category mappings match deterministic matrix`() {
        // 1. 唯一对应一级
        val food = CategoryMapping.resolve("餐饮", null, isIncome = false)
        assertEquals(7, food.parentId)
        assertNull(food.childId)
        assertFalse(food.isCustom)

        val traffic = CategoryMapping.resolve("交通", null, isIncome = false)
        assertEquals(6, traffic.parentId)
        assertNull(traffic.childId)

        val shopping = CategoryMapping.resolve("购物", null, isIncome = false)
        assertEquals(8, shopping.parentId)
        assertNull(shopping.childId) // 规则：不根据泛词推测商品用途

        val housing = CategoryMapping.resolve("居住", null, isIncome = false)
        assertEquals(4, housing.parentId)

        val entertainment = CategoryMapping.resolve("娱乐", null, isIncome = false)
        assertEquals(5, entertainment.parentId)

        val medical = CategoryMapping.resolve("医疗", null, isIncome = false)
        assertEquals(1, medical.parentId)

        val education = CategoryMapping.resolve("教育", null, isIncome = false)
        assertEquals(3, education.parentId)

        val gift = CategoryMapping.resolve("人情", null, isIncome = false)
        assertEquals(2, gift.parentId)

        val other = CategoryMapping.resolve("其他", null, isIncome = false)
        assertEquals(99, other.parentId)

        // 2. 细化归属
        val travel = CategoryMapping.resolve("旅行", null, isIncome = false)
        assertEquals(5, travel.parentId)
        assertEquals(507, travel.childId)

        val telecom = CategoryMapping.resolve("通讯", null, isIncome = false)
        assertEquals(4, telecom.parentId)
        assertEquals(408, telecom.childId)

        val pets = CategoryMapping.resolve("宠物", null, isIncome = false)
        assertEquals(8, pets.parentId)
        assertEquals(803, pets.childId)

        val baby = CategoryMapping.resolve("母婴", null, isIncome = false)
        assertEquals(8, baby.parentId)
        assertEquals(805, baby.childId)

        val finance = CategoryMapping.resolve("金融", null, isIncome = false)
        assertEquals(99, finance.parentId)
        assertEquals(9902, finance.childId)

        // 3. 自定义保留
        val clothing = CategoryMapping.resolve("服饰美容", null, isIncome = false)
        assertEquals(8, clothing.parentId)
        assertEquals(891, clothing.childId)
        assertTrue(clothing.isCustom)

        val software = CategoryMapping.resolve("软件服务", null, isIncome = false)
        assertEquals(88, software.parentId)
        assertNull(software.childId)
        assertTrue(software.isCustom)
    }

    @Test
    fun `expense subcategories map correctly to standard or custom items`() {
        val bfast = CategoryMapping.resolve("餐饮", "早餐", isIncome = false)
        assertEquals(7, bfast.parentId)
        assertEquals(708, bfast.childId)
        assertFalse(bfast.isCustom)

        val snack = CategoryMapping.resolve("餐饮", "饮品零食", isIncome = false)
        assertEquals(7, snack.parentId)
        assertEquals(791, snack.childId)
        assertTrue(snack.isCustom)

        val taxi = CategoryMapping.resolve("交通", "打车", isIncome = false)
        assertEquals(6, taxi.parentId)
        assertEquals(607, taxi.childId)

        val bike = CategoryMapping.resolve("交通", "共享单车", isIncome = false)
        assertEquals(6, bike.parentId)
        assertEquals(691, bike.childId)
        assertTrue(bike.isCustom)

        val api = CategoryMapping.resolve("软件服务", "AI/API", isIncome = false)
        assertEquals(88, api.parentId)
        assertEquals(8801, api.childId)
        assertTrue(api.isCustom)
    }

    @Test
    fun `income categories map to standard or custom income items`() {
        val salary = CategoryMapping.resolve("工资", null, isIncome = true)
        assertEquals(9, salary.parentId)
        assertEquals(904, salary.childId)
        assertFalse(salary.isCustom)

        val partTime = CategoryMapping.resolve("兼职", null, isIncome = true)
        assertEquals(9, partTime.parentId)
        assertEquals(905, partTime.childId)

        val living = CategoryMapping.resolve("生活费", null, isIncome = true)
        assertEquals(9, living.parentId)
        assertEquals(991, living.childId)
        assertTrue(living.isCustom)

        val refund = CategoryMapping.resolve("退款", null, isIncome = true)
        assertEquals(9, refund.parentId)
        assertEquals(994, refund.childId)
        assertTrue(refund.isCustom)
    }

    @Test
    fun `custom seed categories do not collide with default 76 categories`() {
        val defaultIds = DefaultCategories.ALL.map { it.categoryId to it.parentId }.toSet()
        val customSeeds = CategoryMapping.CUSTOM_SEED_CATEGORIES

        assertTrue("CUSTOM_SEED_CATEGORIES should not be empty", customSeeds.isNotEmpty())

        for (custom in customSeeds) {
            val key = custom.categoryId to custom.parentId
            assertFalse(
                "Custom category ID ${custom.categoryId} under parent ${custom.parentId} collides with default categories!",
                defaultIds.contains(key),
            )
            assertTrue("Custom categories must be marked isCustom = true", custom.isCustom)
        }
    }

}
