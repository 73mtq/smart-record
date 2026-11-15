package com.nonen.Bookkeeping.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nonen.Bookkeeping.data.RoomRepositoryTest
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetRepositoryTest : RoomRepositoryTest() {
    private fun repository() = AssetRepository(
        database.assetDao(), database.transferDao(), database.transactionDao(), database,
    )

    @Test
    fun creationAndSummaryIncludeCreditLiability() = runBlocking {
        val repo = repository()
        database.assetDao().insert(AssetEntity(assetId = 1L, assetName = "现金", assetNumber = 500.0))
        val bankId = repo.createAsset("招商银行", initialBalance = 10000.0, groupName = "银行卡")
        val creditId = repo.createAsset(
            "浦发信用卡", initialBalance = 15000.0, quota = 20000.0,
            type = AssetRepository.TYPE_CREDIT, groupName = "信用卡",
        )

        assertTrue(bankId >= 101L)
        assertTrue(creditId > bankId)
        val all = repo.getAll()
        assertEquals(3, all.size)
        val summary = repo.calculateSummary(all)
        assertEquals(10500.0, summary.totalAsset, 0.001)
        assertEquals(5000.0, summary.totalLiability, 0.001)
        assertEquals(5500.0, summary.netAsset, 0.001)
    }

    @Test
    fun balanceAdjustmentPersistsAccountAndBill() = runBlocking {
        val repo = repository()
        val assetId = repo.createAsset("微信零钱", initialBalance = 100.0)

        repo.adjustBalance(assetId, targetBalance = 150.0, generateBill = true, note = "盘点对账")

        assertEquals(150.0, requireNotNull(repo.getById(assetId)).assetNumber, 0.001)
        val bill = database.transactionDao().getAll().single()
        assertEquals(50.0, bill.amount, 0.001)
        assertEquals("INCOME", bill.transactionType)
        assertEquals("微信零钱", bill.account)
        assertEquals("盘点对账", bill.note)
    }

    @Test
    fun deletingCustomAccountReassignsBillsAndPreservesDefaultAccount() = runBlocking {
        val repo = repository()
        database.assetDao().insert(AssetEntity(assetId = 1L, assetName = "现金", assetNumber = 200.0))
        assertFalse(repo.deleteAsset(1L))
        assertNotNull(repo.getById(1L))
        val customId = repo.createAsset("测试账户", initialBalance = 50.0)
        database.transactionDao().insert(
            TransactionEntity(
                amount = -30.0, category = "餐饮", assetId = customId, account = "测试账户",
                source = "manual", timestamp = 1L, hash = "asset-reassignment",
            ),
        )

        assertTrue(repo.deleteAsset(customId))

        val bill = database.transactionDao().getAll().single()
        assertEquals(1L, bill.assetId)
        assertEquals("现金", bill.account)
        assertEquals(null, repo.getById(customId))
    }
}
