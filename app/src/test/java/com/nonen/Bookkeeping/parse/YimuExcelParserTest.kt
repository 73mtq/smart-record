package com.nonen.Bookkeeping.parse

import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.export.BackupExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class YimuExcelParserTest {

    @Test
    fun `test category mapping for standard and legacy categories`() {
        // 1. 标准一木一级分类直接保持
        val (foodMain, foodSub) = BackupExcelParser.normalizeCategory("食品餐饮", "午餐", false)
        assertEquals("食品餐饮", foodMain)
        assertEquals("午餐", foodSub)

        val (incomeMain, incomeSub) = BackupExcelParser.normalizeCategory("收入", "工资", true)
        assertEquals("收入", incomeMain)
        assertEquals("工资", incomeSub)

        // 2. 旧版单一分类简写转换为标准一级分类
        val (legacyFood, legacySub) = BackupExcelParser.normalizeCategory("餐饮", null, false)
        assertEquals("食品餐饮", legacyFood)
        assertNull(legacySub)

        val (legacyTrans, _) = BackupExcelParser.normalizeCategory("交通", null, false)
        assertEquals("出行交通", legacyTrans)

        val (legacyApparel, subApparel) = BackupExcelParser.normalizeCategory("服饰美容", null, false)
        assertEquals("购物消费", legacyApparel)
        assertEquals("服饰运动", subApparel)
    }

    @Test
    fun `test account mapping to source`() {
        assertEquals("wechat", BackupExcelParser.mapAccountToSource("微信支付"))
        assertEquals("wechat", BackupExcelParser.mapAccountToSource("微信"))
        assertEquals("alipay", BackupExcelParser.mapAccountToSource("支付宝"))
        assertEquals("bank", BackupExcelParser.mapAccountToSource("银行卡"))
        assertEquals("manual", BackupExcelParser.mapAccountToSource("现金/手动"))
        assertEquals("招商银行", BackupExcelParser.mapAccountToSource("招商银行"))
    }

    @Test
    fun `test toEntity automatic foreign key resolution`() {
        val row = BackupRow(
            timestamp = 1700000000000L,
            amount = 88.5,
            isIncome = false,
            typeValid = true,
            category = "食品餐饮",
            subCategory = "午餐",
            account = "微信钱包",
            book = "家庭账本",
            note = "工作餐",
            merchant = "老乡鸡",
            refund = null,
            hash = "custom_hash_123",
        )

        val assetMap = mapOf("微信钱包" to 10L, "招商银行" to 11L)
        val bookMap = mapOf("家庭账本" to 5L, "默认账本" to 1L)
        val catMap = mapOf(
            "食品餐饮" to (7 to null),
            "午餐" to (7 to 707),
        )

        val entity = BackupExcelParser.toEntity(
            row = row,
            assetNameToId = assetMap,
            bookNameToId = bookMap,
            categoryNameToIds = catMap,
        )

        assertNotNull(entity)
        assertEquals(-88.5, entity!!.amount, 0.001)
        assertEquals(10L, entity.assetId)
        assertEquals(5L, entity.bookId)
        assertEquals(7, entity.parentCategoryId)
        assertEquals(707, entity.childCategoryId)
        assertEquals("custom_hash_123", entity.hash)
        assertEquals("EXPENSE", entity.transactionType)
    }

    @Test
    fun `test toEntity fallback to default asset and book`() {
        val row = BackupRow(
            timestamp = 1700000000000L,
            amount = 50.0,
            isIncome = true,
            typeValid = true,
            category = "收入",
            account = "未知账户",
            book = "未知账本",
        )

        val entity = BackupExcelParser.toEntity(
            row = row,
            assetNameToId = emptyMap(),
            bookNameToId = emptyMap(),
            categoryNameToIds = emptyMap(),
            defaultAssetId = 1L,
            defaultBookId = 1L,
        )

        assertNotNull(entity)
        assertEquals(50.0, entity!!.amount, 0.001)
        assertEquals(1L, entity.assetId)
        assertEquals(1L, entity.bookId)
        assertEquals("INCOME", entity.transactionType)
    }

    @Test
    fun `test csv build and parse roundtrip`() {
        val timeNow = 1700000000000L
        val tx = TransactionEntity(
            id = 1L,
            amount = -35.0,
            category = "食品餐饮",
            subCategory = "早餐",
            note = "豆浆包子",
            merchant = "早点铺",
            timestamp = timeNow,
            source = "manual",
            assetId = 1L,
            bookId = 1L,
            parentCategoryId = 7,
            childCategoryId = 708,
            hash = "h_breakfast",
        )

        val assetMap = mapOf(1L to "现金")
        val bookMap = mapOf(1L to "默认账本")
        val catMap = mapOf(7 to "食品餐饮", 708 to "早餐")

        val csvBytes = BackupExporter.buildCsv(listOf(tx), assetMap, bookMap, catMap)
        assertTrue(csvBytes.isNotEmpty())

        val parsedRows = BackupExcelParser.parse(csvBytes)
        assertNotNull(parsedRows)
        assertEquals(1, parsedRows!!.size)

        val first = parsedRows[0]
        assertEquals(35.0, first.amount ?: 0.0, 0.001)
        assertFalse(first.isIncome)
        assertEquals("餐饮", first.category)
        assertEquals("早餐", first.subCategory)
        assertEquals("现金", first.account)
        assertEquals("默认账本", first.book)
        assertEquals("豆浆包子", first.note)
        assertEquals("早点铺", first.merchant)
        assertEquals("h_breakfast", first.hash)
    }
}
