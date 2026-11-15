package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.AssetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetSummaryTest {
    @Test
    fun `credit accounts distinguish available credit and overpayment`() {
        val summary = summarizeAssets(
            listOf(
                asset(1, 15000.0, AssetRepository.TYPE_CREDIT, quota = 20000.0),
                asset(2, 21000.0, AssetRepository.TYPE_CREDIT, quota = 20000.0),
                asset(3, -100.0, AssetRepository.TYPE_CREDIT),
                asset(4, 200.0, AssetRepository.TYPE_CREDIT),
            ),
        )

        assertEquals(1200.0, summary.totalAsset, 0.0)
        assertEquals(5100.0, summary.totalLiability, 0.0)
        assertEquals(-3900.0, summary.netAsset, 0.0)
    }

    @Test
    fun `fund claim and debt accounts retain their balance semantics`() {
        val summary = summarizeAssets(
            listOf(
                asset(1, 1000.0),
                asset(2, -30.0),
                asset(3, 400.0, AssetRepository.TYPE_CLAIM),
                asset(4, 200.0, AssetRepository.TYPE_DEBT),
                asset(5, -50.0, AssetRepository.TYPE_DEBT),
                asset(6, 9999.0).copy(intoTotalAsset = false),
            ),
        )

        assertEquals(1400.0, summary.totalAsset, 0.0)
        assertEquals(280.0, summary.totalLiability, 0.0)
        assertEquals(1120.0, summary.netAsset, 0.0)
    }

    @Test
    fun `historical balances preserve precision before presentation rounding`() {
        val summary = summarizeAssets(listOf(asset(1, 100.0), asset(2, -20.0))) {
            if (it.assetId == 1L) 12.3456 else -2.1234
        }

        assertEquals(12.3456, summary.totalAsset, 0.0)
        assertEquals(2.1234, summary.totalLiability, 0.0)
        assertEquals(10.2222, summary.netAsset, 0.0000001)
    }

    @Test
    fun `empty accounts have zero totals`() {
        assertEquals(AssetSummary(), summarizeAssets(emptyList()))
    }

    private fun asset(id: Long, balance: Double, type: Int = AssetRepository.TYPE_FUND, quota: Double = 0.0) =
        AssetEntity(assetId = id, assetName = "账户$id", assetNumber = balance, assetType = type, totalQuota = quota)
}
