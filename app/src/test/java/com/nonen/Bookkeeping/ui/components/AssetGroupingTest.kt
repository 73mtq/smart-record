package com.nonen.Bookkeeping.ui.components

import com.nonen.Bookkeeping.data.db.AssetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/** 资产页分组：原版 manager/c.java 的分组与 database/action/f.java 的默认组名。 */
class AssetGroupingTest {

    @Test
    fun `相同 groupName 的账户归入同一组`() {
        val groups = groupAssets(
            listOf(
                asset(1L, "现金", groupName = "现金账户"),
                asset(2L, "微信", groupName = "现金账户"),
            ),
        )
        assertEquals(1, groups.size)
        assertEquals("现金账户", groups.single().name)
        assertEquals(listOf(1L, 2L), groups.single().assets.map { it.assetId })
    }

    @Test
    fun `groupName 为空时按 assetType 回退到默认组名`() {
        val groups = groupAssets(
            listOf(
                asset(1L, "现金", assetType = 1),
                asset(2L, "信用卡", assetType = 2),
                asset(3L, "借出", assetType = 6),
                asset(4L, "借入", assetType = 7),
                asset(5L, "报销", assetType = 9),
            ),
        )
        assertEquals(
            listOf("资金账户", "信贷账户", "应收款/借出", "应付款/借入", "报销"),
            groups.map { it.name },
        )
    }

    @Test
    fun `空白 groupName 同样按 assetType 回退`() {
        val groups = groupAssets(listOf(asset(1L, "现金", assetType = 1, groupName = "   ")))
        assertEquals("资金账户", groups.single().name)
    }

    @Test
    fun `组间按固定默认顺序排列，其余按名称`() {
        val groups = groupAssets(
            listOf(
                asset(1L, "自建", groupName = "现金账户"),
                asset(2L, "报销", assetType = 9),
                asset(3L, "信用卡", assetType = 2),
                asset(4L, "现金", assetType = 1),
            ),
        )
        assertEquals(listOf("资金账户", "信贷账户", "报销", "现金账户"), groups.map { it.name })
    }

    @Test
    fun `未识别的组名按名称升序排列`() {
        val groups = groupAssets(
            listOf(
                asset(1L, "甲", groupName = "自定义"),
                asset(2L, "乙", groupName = "备用金"),
            ),
        )
        assertEquals(listOf("备用金", "自定义"), groups.map { it.name })
    }

    @Test
    fun `组内按 positionWeight 再按 assetId 升序`() {
        val groups = groupAssets(
            listOf(
                asset(10L, "末位", groupName = "现金账户", positionWeight = 2),
                asset(5L, "次位", groupName = "现金账户", positionWeight = 1),
                asset(3L, "首位", groupName = "现金账户", positionWeight = 1),
            ),
        )
        assertEquals(listOf(3L, 5L, 10L), groups.single().assets.map { it.assetId })
    }

    @Test
    fun `组总额只统计计入总资产的账户`() {
        val groups = groupAssets(
            listOf(
                asset(1L, "现金", assetNumber = 100.0, groupName = "现金账户"),
                asset(2L, "备用金", assetNumber = 23.5, groupName = "现金账户"),
                asset(3L, "信用卡", assetNumber = -50.0, groupName = "现金账户", intoTotalAsset = false),
            ),
        )
        assertEquals(123.5, groups.single().total, 0.0001)
    }

    @Test
    fun `隐藏金额时组总额与行内金额都显示掩码`() {
        assertEquals("****", assetAmountText(1234.5, hidden = true))
        assertEquals("****", assetAmountText(-0.5, hidden = true))
        assertEquals("1,234.50", assetAmountText(1234.5, hidden = false))
        assertEquals("-1,234.50", assetAmountText(-1234.5, hidden = false))
        assertEquals("0.00", assetAmountText(0.0, hidden = false))
    }

    private fun asset(
        id: Long,
        name: String,
        assetType: Int = 1,
        assetNumber: Double = 0.0,
        groupName: String? = null,
        positionWeight: Int = 0,
        intoTotalAsset: Boolean = true,
    ): AssetEntity = AssetEntity(
        assetId = id,
        assetName = name,
        assetNumber = assetNumber,
        assetType = assetType,
        intoTotalAsset = intoTotalAsset,
        groupName = groupName,
        positionWeight = positionWeight,
    )
}
