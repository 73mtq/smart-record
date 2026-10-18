package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.AssetDao
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetRepositoryTest {

    private class FakeAssetDao : AssetDao {
        val storage = mutableListOf<AssetEntity>()

        override fun getAllVisibleFlow(): Flow<List<AssetEntity>> =
            flowOf(storage.filter { !it.hide })

        override fun getAllFlow(): Flow<List<AssetEntity>> =
            flowOf(storage.toList())

        override suspend fun getAll(): List<AssetEntity> =
            storage.toList()

        override suspend fun findById(assetId: Long): AssetEntity? =
            storage.find { it.assetId == assetId }

        override suspend fun findByName(name: String): AssetEntity? =
            storage.find { it.assetName == name }

        override suspend fun insert(asset: AssetEntity): Long {
            storage.removeAll { it.assetId == asset.assetId }
            storage.add(asset)
            return asset.assetId
        }

        override suspend fun insertAll(assets: List<AssetEntity>) {
            assets.forEach { insert(it) }
        }

        override suspend fun update(asset: AssetEntity) {
            val idx = storage.indexOfFirst { it.assetId == asset.assetId }
            if (idx >= 0) storage[idx] = asset
        }

        override suspend fun updateBalance(assetId: Long, delta: Double, timestamp: Long) {
            val idx = storage.indexOfFirst { it.assetId == assetId }
            if (idx >= 0) {
                val current = storage[idx]
                storage[idx] = current.copy(assetNumber = current.assetNumber + delta, updatedAt = timestamp)
            }
        }

        override suspend fun getMaxAssetId(): Long? =
            storage.maxOfOrNull { it.assetId }

        override suspend fun getMaxPositionWeight(): Int? =
            storage.maxOfOrNull { it.positionWeight }

        override suspend fun countTransactionsUsingAsset(assetId: Long): Int = 0

        override suspend fun countTransfersUsingAsset(assetId: Long): Int = 0

        override suspend fun updateAssetHide(assetId: Long, hide: Boolean, timestamp: Long) {
            val idx = storage.indexOfFirst { it.assetId == assetId }
            if (idx >= 0) {
                storage[idx] = storage[idx].copy(hide = hide, updatedAt = timestamp)
            }
        }

        override suspend fun updatePositionWeight(assetId: Long, weight: Int, timestamp: Long) {
            val idx = storage.indexOfFirst { it.assetId == assetId }
            if (idx >= 0) {
                storage[idx] = storage[idx].copy(positionWeight = weight, updatedAt = timestamp)
            }
        }

        override suspend fun deleteById(assetId: Long) {
            storage.removeAll { it.assetId == assetId }
        }
    }

    private class FakeTransactionDao : TransactionDao {
        val storage = mutableListOf<TransactionEntity>()
        var reassignedAssets = mutableListOf<Triple<Long, Long, String>>()

        override suspend fun insert(t: TransactionEntity): Long {
            val newId = (storage.maxOfOrNull { it.id } ?: 0L) + 1L
            storage.add(t.copy(id = newId))
            return newId
        }

        override suspend fun update(t: TransactionEntity) {}
        override suspend fun delete(t: TransactionEntity) {}
        override suspend fun getById(id: Long): TransactionEntity? = storage.find { it.id == id }
        override fun observeRange(start: Long, end: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getRange(start: Long, end: Long): List<TransactionEntity> = emptyList()
        override suspend fun countSimilarAuto(amount: Double, from: Long, to: Long): Int = 0
        override suspend fun search(keyword: String, category: String?, type: String?, start: Long, end: Long): List<TransactionEntity> = emptyList()
        override suspend fun allCategories(): List<String> = emptyList()
        override suspend fun getAll(): List<TransactionEntity> = storage.toList()
        override suspend fun getUnmigratedCategories(): List<TransactionEntity> = emptyList()
        override suspend fun updateCategoryIds(id: Long, parentCategoryId: Int, childCategoryId: Int?) {}
        override suspend fun countUnmigrated(): Int = 0

        override suspend fun reassignAsset(oldAssetId: Long, newAssetId: Long, newAccount: String) {
            reassignedAssets.add(Triple(oldAssetId, newAssetId, newAccount))
            val updated = storage.map {
                if (it.assetId == oldAssetId) it.copy(assetId = newAssetId, account = newAccount) else it
            }
            storage.clear()
            storage.addAll(updated)
        }
        override fun observeExpensesBetween(bookId: Long, start: Long, end: Long): Flow<List<TransactionEntity>> =
            flowOf(storage.filter { it.bookId == bookId && it.amount < 0 && it.timestamp in start..end })
        override suspend fun getExpensesBetween(bookId: Long, start: Long, end: Long): List<TransactionEntity> =
            storage.filter { it.bookId == bookId && it.amount < 0 && it.timestamp in start..end }
    }

    @Test
    fun `test asset creation and calculation`() = runBlocking {
        val assetDao = FakeAssetDao()
        val txDao = FakeTransactionDao()
        val repo = AssetRepository(assetDao, transactionDao = txDao)

        // 默认现金账户
        assetDao.insert(
            AssetEntity(
                assetId = 1L,
                assetName = "现金",
                assetNumber = 500.0,
                assetType = AssetRepository.TYPE_FUND,
                intoTotalAsset = true,
            )
        )

        // 新建招商储蓄卡
        val bankId = repo.createAsset(
            name = "招商银行",
            initialBalance = 10000.0,
            type = AssetRepository.TYPE_FUND,
            groupName = "银行卡",
        )
        assertTrue(bankId >= 101L)

        // 新建信用卡：总额度 20000，当前可用 15000（已用 5000 负债）
        val creditId = repo.createAsset(
            name = "浦发信用卡",
            initialBalance = 15000.0,
            quota = 20000.0,
            type = AssetRepository.TYPE_CREDIT,
            groupName = "信用卡",
        )
        assertTrue(creditId > bankId)

        val all = repo.getAll()
        assertEquals(3, all.size)

        // 计算总资产与负债
        // 总资产 = 现金 500 + 招行 10000 = 10500
        // 总负债 = 信用卡欠款 (20000 - 15000) = 5000
        // 净资产 = 10500 - 5000 = 5500
        val summary = repo.calculateSummary(all)
        assertEquals(10500.0, summary.totalAsset, 0.001)
        assertEquals(5000.0, summary.totalLiability, 0.001)
        assertEquals(5500.0, summary.netAsset, 0.001)
    }

    @Test
    fun `test balance adjustment with bill generation`() = runBlocking {
        val assetDao = FakeAssetDao()
        val txDao = FakeTransactionDao()
        val repo = AssetRepository(assetDao, transactionDao = txDao)

        val assetId = repo.createAsset("微信零钱", initialBalance = 100.0)

        // 调平余额到 150 元（盘盈 50 元），生成账单
        repo.adjustBalance(assetId, targetBalance = 150.0, generateBill = true, note = "盘点对账")

        val updated = repo.getById(assetId)
        assertNotNull(updated)
        assertEquals(150.0, updated!!.assetNumber, 0.001)

        // 校验生成的调平单据
        assertEquals(1, txDao.storage.size)
        val bill = txDao.storage.first()
        assertEquals(50.0, bill.amount, 0.001)
        assertEquals("INCOME", bill.transactionType)
        assertEquals("微信零钱", bill.account)
        assertEquals("盘点对账", bill.note)
    }

    @Test
    fun `test delete protection for default asset and reassignment for custom asset`() = runBlocking {
        val assetDao = FakeAssetDao()
        val txDao = FakeTransactionDao()
        val repo = AssetRepository(assetDao, transactionDao = txDao)

        assetDao.insert(
            AssetEntity(
                assetId = 1L,
                assetName = "现金",
                assetNumber = 200.0,
                assetType = AssetRepository.TYPE_FUND,
            )
        )

        // 默认资产不可删除
        val defaultDeleteResult = repo.deleteAsset(1L)
        assertFalse(defaultDeleteResult)
        assertNotNull(repo.getById(1L))

        // 创建自定义账户并挂载一笔账单
        val customId = repo.createAsset("测试账户", initialBalance = 50.0)
        txDao.insert(
            TransactionEntity(
                amount = -30.0,
                category = "餐饮",
                assetId = customId,
                account = "测试账户",
                source = "manual",
                timestamp = System.currentTimeMillis(),
                hash = "hash1",
            )
        )

        // 删除自定义账户
        val customDeleteResult = repo.deleteAsset(customId)
        assertTrue(customDeleteResult)

        // 验证账单已安全重定向至默认现金账户
        val reassigned = txDao.storage.first()
        assertEquals(1L, reassigned.assetId)
        assertEquals("现金", reassigned.account)
    }
}
