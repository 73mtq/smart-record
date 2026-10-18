package com.nonen.Bookkeeping.export

import com.nonen.Bookkeeping.core.MiniXlsx
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.parse.BackupExcelParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Excel 备份导出/导入回环 + MiniXlsx 基础读写 */
class BackupRoundTripTest {

    private fun entity(
        amount: Double,
        category: String,
        merchant: String? = "苍南县科蕊小吃店",
        note: String? = null,
        source: String = "auto",
    ) = TransactionEntity(
        amount = amount,
        category = category,
        note = note,
        merchant = merchant,
        timestamp = 1788068580000L, // 2026-08-30 13:43:00（精确至分钟）
        source = source,
        hash = "abc123def456",
    )

    @Test
    fun `mini xlsx round trip with strings numbers and blanks`() {
        val bytes = MiniXlsx.write(
            listOf(
                listOf("时间", "金额", "备注"),
                listOf("2026-08-30 14:23", 12.5, "含\"引号\"与<标签>&和'单引号'"),
                listOf(null, 3, null),
            ),
        )
        val table = MiniXlsx.read(bytes)
        assertEquals(listOf("时间", "金额", "备注"), table[0])
        assertEquals("2026-08-30 14:23", table[1][0])
        assertEquals("12.5", table[1][1])
        assertEquals("含\"引号\"与<标签>&和'单引号'", table[1][2])
        assertEquals(null, table[2][0])
        assertEquals("3", table[2][1])
    }

    @Test
    fun `backup export and import round trip`() {
        val transactions = listOf(
            entity(-25.5, "餐饮", merchant = "肯德基", note = "午餐"),
            entity(88.0, "工资", merchant = "XX公司", source = "manual"),
            entity(-0.01, "餐饮"),
        )
        val exportedBytes = BackupExporter.buildXlsx(transactions)
        val rows = BackupExcelParser.parse(exportedBytes)
        assertNotNull(rows)
        assertEquals(3, rows!!.size)

        val first = rows[0]
        assertEquals(false, first.isIncome)
        assertEquals(true, first.typeValid)
        assertEquals(25.5, first.amount!!, 0.001)
        assertEquals("餐饮", first.category)
        assertEquals("肯德基", first.merchant)
        assertEquals("午餐", first.note)
        assertEquals("auto", first.source)
        assertEquals("abc123def456", first.hash)
        assertEquals(1788068580000L, first.timestamp)

        val second = rows[1]
        assertEquals(true, second.isIncome)
        assertEquals(88.0, second.amount!!, 0.001)
        assertEquals("manual", second.source)
    }

    @Test
    fun `yimu 23 column native format parsing`() {
        val bytes = MiniXlsx.write(
            listOf(
                BackupExporter.YIMU_HEADERS,
                listOf("2026-10-17 11:55", "支出", -3.4, "食品餐饮", "饮料酒水", "微信支付", "日常账本", "", "", "买奶茶", "", "", "", "", "", "", "", "", "", "", "", "", ""),
                listOf("2026-10-16 20:19", "收入", 1500.0, "收入", "生活费", "支付宝", "日常账本", "", "", "妈妈转账生活费", "", "", "", "", "", "", "", "", "", "", "", "", ""),
            ),
        )
        val rows = BackupExcelParser.parse(bytes)
        assertNotNull(rows)
        assertEquals(2, rows!!.size)

        val expRow = rows[0]
        assertEquals(false, expRow.isIncome)
        assertEquals(3.4, expRow.amount!!, 0.001)
        assertEquals("餐饮", expRow.category)
        assertEquals("饮料酒水", expRow.subCategory)
        assertEquals("微信支付", expRow.account)
        assertEquals("wechat", expRow.source)
        assertEquals("买奶茶", expRow.note)
        assertNotNull(expRow.hash)

        val incRow = rows[1]
        assertEquals(true, incRow.isIncome)
        assertEquals(1500.0, incRow.amount!!, 0.001)
        assertEquals("生活费", incRow.category)
        assertEquals("生活费", incRow.subCategory)
        assertEquals("支付宝", incRow.account)
        assertEquals("alipay", incRow.source)
        assertEquals("妈妈转账生活费", incRow.note)
        assertNotNull(incRow.hash)
    }

    @Test
    fun `parse actual yimu reference xls file`() {
        val candidates = listOf(
            java.io.File("参考导出账单excel/账单_1017165402(1).xls"),
            java.io.File("../参考导出账单excel/账单_1017165402(1).xls"),
        )
        val target = candidates.firstOrNull { it.exists() } ?: return
        val rows = BackupExcelParser.parse(target.readBytes())
        assertNotNull(rows)
        assertEquals(1964, rows!!.size)

        val row0 = rows[0]
        assertEquals(false, row0.isIncome)
        assertEquals(3.4, row0.amount!!, 0.001)
        assertEquals("餐饮", row0.category)
        assertNotNull(row0.hash)
    }

    @Test
    fun `invalid type counts as failed not crash`() {
        val bytes = MiniXlsx.write(
            listOf(
                listOf("记账时间", "类型", "金额", "分类", "商户/交易对象", "备注", "来源", "校验码"),
                listOf("2026-08-30 14:23", "不计收支", "5.00", "", "", "", "", ""),
                listOf("2026-08-30 15:00", "支出", "6.00", "交通", "地铁", "", "", ""),
            ),
        )
        val rows = BackupExcelParser.parse(bytes)!!
        assertEquals(2, rows.size)
        assertEquals(false, rows[0].typeValid)
        assertEquals(true, rows[1].typeValid)
        assertEquals("交通", rows[1].category)
    }

    @Test
    fun `non backup file returns null`() {
        val bytes = MiniXlsx.write(listOf(listOf("随便", "什么")))
        assertNull(BackupExcelParser.parse(bytes))
    }
}
