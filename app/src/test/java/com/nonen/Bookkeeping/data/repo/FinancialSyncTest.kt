package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.AssetDao
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.CategoryRuleDao
import com.nonen.Bookkeeping.data.db.CategoryRuleEntity
import com.nonen.Bookkeeping.data.db.MerchantCategoryDao
import com.nonen.Bookkeeping.data.db.MerchantCategoryEntity
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.TransferDao
import com.nonen.Bookkeeping.data.db.TransferEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialSyncTest {

    private class FakeAssetDao : AssetDao {
        val storage = mutableMapOf<Long, AssetEntity>()

        fun put(asset: AssetEntity) {
            storage[asset.assetId] = asset
        }

        override fun getAllVisibleFlow(): Flow<List<AssetEntity>> = flowOf(storage.values.toList())
        override fun getAllFlow(): Flow<List<AssetEntity>> = flowOf(storage.values.toList())
        override suspend fun getAll(): List<AssetEntity> = storage.values.toList()
        override suspend fun findById(assetId: Long): AssetEntity? = storage[assetId]
        override suspend fun findByName(name: String): AssetEntity? = storage.values.find { it.assetName == name }

        override suspend fun insert(asset: AssetEntity): Long {
            storage[asset.assetId] = asset
            return asset.assetId
        }

        override suspend fun insertAll(assets: List<AssetEntity>) {
            assets.forEach { insert(it) }
        }

        override suspend fun update(asset: AssetEntity) {
            storage[asset.assetId] = asset
        }

        override suspend fun updateBalance(assetId: Long, delta: Double, timestamp: Long) {
            storage[assetId]?.let {
                storage[assetId] = it.copy(assetNumber = it.assetNumber + delta, updatedAt = timestamp)
            }
        }

        override suspend fun getMaxAssetId(): Long? = storage.keys.maxOrNull()
        override suspend fun getMaxPositionWeight(): Int? = storage.values.maxOfOrNull { it.positionWeight }
        override suspend fun countTransactionsUsingAsset(assetId: Long): Int = 0
        override suspend fun countTransfersUsingAsset(assetId: Long): Int = 0
        override suspend fun updateAssetHide(assetId: Long, hide: Boolean, timestamp: Long) {}
        override suspend fun updatePositionWeight(assetId: Long, weight: Int, timestamp: Long) {}
        override suspend fun deleteById(assetId: Long) { storage.remove(assetId) }
    }

    private class FakeTransferDao : TransferDao {
        val storage = mutableMapOf<Long, TransferEntity>()
        private var nextId = 1L

        override fun getAllFlow(): Flow<List<TransferEntity>> = flowOf(storage.values.toList())
        override suspend fun findById(id: Long): TransferEntity? = storage[id]
        override suspend fun getByAssetId(assetId: Long): List<TransferEntity> =
            storage.values.filter { it.fromAssetId == assetId || it.toAssetId == assetId }

        override suspend fun insert(transfer: TransferEntity): Long {
            val id = nextId++
            storage[id] = transfer.copy(transferId = id)
            return id
        }

        override suspend fun update(transfer: TransferEntity) {
            storage[transfer.transferId] = transfer
        }

        override suspend fun delete(transfer: TransferEntity) {
            storage.remove(transfer.transferId)
        }

        override suspend fun deleteById(id: Long) {
            storage.remove(id)
        }
    }

    private class FakeTransactionDao : TransactionDao {
        val storage = mutableMapOf<Long, TransactionEntity>()
        private var nextId = 1L

        override suspend fun insert(t: TransactionEntity): Long {
            val id = nextId++
            storage[id] = t.copy(id = id)
            return id
        }

        override suspend fun update(t: TransactionEntity) {
            storage[t.id] = t
        }

        override suspend fun delete(t: TransactionEntity) {
            storage.remove(t.id)
        }

        override suspend fun getById(id: Long): TransactionEntity? = storage[id]
        override fun observeRange(start: Long, end: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getRange(start: Long, end: Long): List<TransactionEntity> = emptyList()
        override suspend fun countSimilarAuto(amount: Double, from: Long, to: Long): Int = 0
        override suspend fun search(keyword: String, category: String?, type: String?, start: Long, end: Long): List<TransactionEntity> = emptyList()
        override suspend fun allCategories(): List<String> = emptyList()
        override suspend fun getAll(): List<TransactionEntity> = storage.values.toList()
        override suspend fun getUnmigratedCategories(): List<TransactionEntity> = emptyList()
        override suspend fun updateCategoryIds(id: Long, parentCategoryId: Int, childCategoryId: Int?) {}
        override suspend fun countUnmigrated(): Int = 0
        override suspend fun reassignAsset(oldAssetId: Long, newAssetId: Long, newAccount: String) {}
        override fun observeExpensesBetween(bookId: Long, start: Long, end: Long): Flow<List<TransactionEntity>> =
            flowOf(storage.values.filter { it.bookId == bookId && it.amount < 0 && it.timestamp in start..end })
        override suspend fun getExpensesBetween(bookId: Long, start: Long, end: Long): List<TransactionEntity> =
            storage.values.filter { it.bookId == bookId && it.amount < 0 && it.timestamp in start..end }
    }

    private class FakeRuleDao : CategoryRuleDao {
        override fun observeAll(): Flow<List<CategoryRuleEntity>> = flowOf(emptyList())
        override suspend fun getAll(): List<CategoryRuleEntity> = emptyList()
        override suspend fun findByKeyword(keyword: String): CategoryRuleEntity? = null
        override suspend fun countByKeyword(keyword: String): Int = 0
        override suspend fun insert(rule: CategoryRuleEntity): Long = 1L
        override suspend fun insertAll(rules: List<CategoryRuleEntity>) {}
        override suspend fun deleteBuiltin(keyword: String, category: String) {}
        override suspend fun deleteById(id: Long) {}
    }

    private class FakeMerchantDao : MerchantCategoryDao {
        override suspend fun find(merchantKey: String, isIncome: Boolean): MerchantCategoryEntity? = null
        override suspend fun upsert(entity: MerchantCategoryEntity) {}
        override suspend fun getAll(): List<MerchantCategoryEntity> = emptyList()
        override fun observeCount(): Flow<Int> = flowOf(0)
        override suspend fun clearAll() {}
    }

    @Test
    fun `test transaction balance synchronization on insert, update, and delete`() = runBlocking {
        val assetDao = FakeAssetDao()
        val transferDao = FakeTransferDao()
        val txDao = FakeTransactionDao()
        val repo = TransactionRepository(
            dao = txDao,
            ruleDao = FakeRuleDao(),
            merchantDao = FakeMerchantDao(),
            assetDao = assetDao,
            transferDao = transferDao,
        )

        // 初始账户 A: 1000 元，账户 B: 500 元
        assetDao.put(AssetEntity(assetId = 1L, assetName = "现金", assetNumber = 1000.0))
        assetDao.put(AssetEntity(assetId = 2L, assetName = "微信钱包", assetNumber = 500.0))

        // 1. 记一笔支出：现金 -100.0
        val expenseTx = TransactionEntity(
            amount = -100.0,
            category = "餐饮",
            assetId = 1L,
            account = "现金",
            source = "manual",
            timestamp = 1000L,
            hash = "h1",
        )
        val inserted = repo.insertIfNew(expenseTx)
        assertTrue(inserted)
        assertEquals(900.0, assetDao.findById(1L)!!.assetNumber, 0.001)

        // 2. 记一笔收入：微信钱包 +300.0
        val incomeTx = TransactionEntity(
            amount = 300.0,
            category = "工资",
            assetId = 2L,
            account = "微信钱包",
            source = "manual",
            timestamp = 2000L,
            hash = "h2",
        )
        repo.insertIfNew(incomeTx)
        assertEquals(800.0, assetDao.findById(2L)!!.assetNumber, 0.001)

        // 3. 修改第一笔账单：金额从 -100.0 改为 -150.0
        val savedExpense = txDao.getAll().first { it.amount == -100.0 }
        repo.update(savedExpense.copy(amount = -150.0))
        assertEquals(850.0, assetDao.findById(1L)!!.assetNumber, 0.001)

        // 4. 跨账户修改：将第一笔账单从现金转到微信钱包支付
        val currentExpense = txDao.getById(savedExpense.id)!!
        repo.update(currentExpense.copy(assetId = 2L, account = "微信钱包"))
        // 现金账户应回滚 150 -> 恢复至 1000.0
        assertEquals(1000.0, assetDao.findById(1L)!!.assetNumber, 0.001)
        // 微信钱包应扣减 150 -> 800 - 150 = 650.0
        assertEquals(650.0, assetDao.findById(2L)!!.assetNumber, 0.001)

        // 5. 删除第一笔账单：微信钱包应恢复 150 -> 800.0
        repo.delete(txDao.getById(savedExpense.id)!!)
        assertEquals(800.0, assetDao.findById(2L)!!.assetNumber, 0.001)
    }

    @Test
    fun `test transfer balance atomicity and rollback`() = runBlocking {
        val assetDao = FakeAssetDao()
        val transferDao = FakeTransferDao()
        val txDao = FakeTransactionDao()
        val repo = TransactionRepository(
            dao = txDao,
            ruleDao = FakeRuleDao(),
            merchantDao = FakeMerchantDao(),
            assetDao = assetDao,
            transferDao = transferDao,
        )

        // 账户 1: 500 元，账户 2: 100 元
        assetDao.put(AssetEntity(assetId = 1L, assetName = "储蓄卡", assetNumber = 500.0))
        assetDao.put(AssetEntity(assetId = 2L, assetName = "零钱通", assetNumber = 100.0))

        // 转账 200 元，手续费 2 元
        val transferId = repo.createTransfer(
            fromAssetId = 1L,
            toAssetId = 2L,
            cost = 200.0,
            toCost = 200.0,
            serviceCharge = 2.0,
            remark = "转入理财",
        )
        assertTrue(transferId > 0)

        // 账户 1 扣减 202 元 -> 298.0
        assertEquals(298.0, assetDao.findById(1L)!!.assetNumber, 0.001)
        // 账户 2 增加 200 元 -> 300.0
        assertEquals(300.0, assetDao.findById(2L)!!.assetNumber, 0.001)

        // 撤销/删除转账
        val deleted = repo.deleteTransfer(transferId)
        assertTrue(deleted)

        // 两个账户余额完全恢复初始值
        assertEquals(500.0, assetDao.findById(1L)!!.assetNumber, 0.001)
        assertEquals(100.0, assetDao.findById(2L)!!.assetNumber, 0.001)
    }
}
