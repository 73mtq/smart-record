package com.nonen.Bookkeeping.stats

import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.AssetRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AssetTrendTest {

    private val zone = ZoneId.systemDefault()

    private fun epochMillisOf(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `test computeAssetTrend matches current balance on end date`() {
        val assets = listOf(
            AssetEntity(
                assetId = 1L,
                assetName = "现金",
                assetNumber = 3000.0,
                assetType = AssetRepository.TYPE_FUND,
                intoTotalAsset = true,
            ),
            AssetEntity(
                assetId = 2L,
                assetName = "招行信用卡",
                assetNumber = 8000.0,
                totalQuota = 10000.0,
                assetType = AssetRepository.TYPE_CREDIT,
                intoTotalAsset = true,
            ),
        )

        val startDate = LocalDate.of(2026, 10, 1)
        val endDate = LocalDate.of(2026, 10, 10)

        // 无后续交易时，所有日期的快照都应与当前状态保持一致
        val trend = StatsCalculator.computeAssetTrend(startDate, endDate, assets, emptyList(), zone)

        assertEquals(10, trend.size)
        val latest = trend.last()
        // 现金 3000 资产，信用卡欠款 10000 - 8000 = 2000 负债，净资产 = 1000
        assertEquals(3000.0, latest.totalAsset, 0.001)
        assertEquals(2000.0, latest.totalDebt, 0.001)
        assertEquals(1000.0, latest.netAsset, 0.001)
    }

    @Test
    fun `test computeAssetTrend accurately rolls back past transactions`() {
        // 当前时刻状态（10月18日）：
        // 账户 1 当前余额 1500.0
        val assets = listOf(
            AssetEntity(
                assetId = 1L,
                assetName = "微信零钱",
                assetNumber = 1500.0,
                assetType = AssetRepository.TYPE_FUND,
                intoTotalAsset = true,
            ),
        )

        // 历史流水：
        // 10月10日 收入 1000.0 (使得当时余额从 700 变成 1700)
        // 10月15日 支出 200.0 (使得当时余额从 1700 变成 1500)
        val txs = listOf(
            TransactionEntity(
                id = 1,
                assetId = 1L,
                amount = 1000.0,
                category = "收入",
                source = "manual",
                hash = "h_1",
                timestamp = epochMillisOf(2026, 10, 10, 10),
            ),
            TransactionEntity(
                id = 2,
                assetId = 1L,
                amount = -200.0,
                category = "餐饮",
                source = "manual",
                hash = "h_2",
                timestamp = epochMillisOf(2026, 10, 15, 18),
            ),
        )


        val startDate = LocalDate.of(2026, 10, 8)
        val endDate = LocalDate.of(2026, 10, 18)

        val trend = StatsCalculator.computeAssetTrend(startDate, endDate, assets, txs, zone)
        val trendMap = trend.associateBy { it.date.dayOfMonth }

        // 10月8日（两笔交易均在之后发生）：
        // 当时余额 = 1500 - (1000 + (-200)) = 1500 - 800 = 700.0
        val day8 = trendMap[8]!!
        assertEquals(700.0, day8.netAsset, 0.001)

        // 10月12日（收入已发生，支出在之后发生）：
        // 当时余额 = 1500 - (-200) = 1700.0
        val day12 = trendMap[12]!!
        assertEquals(1700.0, day12.netAsset, 0.001)

        // 10月18日（两笔均已发生）：
        // 当时余额 = 当前余额 1500.0
        val day18 = trendMap[18]!!
        assertEquals(1500.0, day18.netAsset, 0.001)
    }

    @Test
    fun `test computeAssetTrend ignores accounts excluded from total asset`() {
        val assets = listOf(
            AssetEntity(
                assetId = 1L,
                assetName = "现金",
                assetNumber = 2000.0,
                intoTotalAsset = true,
            ),
            AssetEntity(
                assetId = 2L,
                assetName = "私密账户",
                assetNumber = 50000.0,
                intoTotalAsset = false,
            ),
        )

        val trend = StatsCalculator.computeAssetTrend(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            assets,
            emptyList(),
            zone,
        )

        assertEquals(2000.0, trend.first().netAsset, 0.001)
        assertEquals(2000.0, trend.first().totalAsset, 0.001)
        assertEquals(0.0, trend.first().totalDebt, 0.001)
    }
}
