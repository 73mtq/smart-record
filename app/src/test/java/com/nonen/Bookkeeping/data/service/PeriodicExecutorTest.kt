package com.nonen.Bookkeeping.data.service

import androidx.room.RoomDatabase
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.AssetDao
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.CycleDao
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.TransferDao
import com.nonen.Bookkeeping.data.db.TransferEntity
import com.nonen.Bookkeeping.data.entity.CycleEntity
import com.nonen.Bookkeeping.data.entity.CycleLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Calendar

class PeriodicExecutorTest {

    private class FakeCycleDao : CycleDao {
        val cycles = mutableListOf<CycleEntity>()
        val logs = mutableListOf<CycleLogEntity>()
        private var idSeq = 1L

        override suspend fun insertCycle(cycle: CycleEntity): Long {
            val id = if (cycle.cycleId > 0L) cycle.cycleId else idSeq++
            val entity = cycle.copy(cycleId = id)
            cycles.removeAll { it.cycleId == id }
            cycles.add(entity)
            return id
        }

        override suspend fun updateCycle(cycle: CycleEntity) {
            val idx = cycles.indexOfFirst { it.cycleId == cycle.cycleId }
            if (idx >= 0) cycles[idx] = cycle
        }

        override suspend fun deleteCycle(cycle: CycleEntity) {
            cycles.removeAll { it.cycleId == cycle.cycleId }
        }

        override suspend fun deleteCycleById(cycleId: Long) {
            cycles.removeAll { it.cycleId == cycleId }
        }

        override fun observeAllCycles(bookId: Long): Flow<List<CycleEntity>> =
            flowOf(cycles.filter { it.bookId == bookId })

        override suspend fun getActiveCycles(): List<CycleEntity> =
            cycles.filter { !it.pause && !it.end }

        override suspend fun getCycleById(cycleId: Long): CycleEntity? =
            cycles.find { it.cycleId == cycleId }

        override suspend fun insertLog(log: CycleLogEntity): Long {
            logs.add(log)
            return log.logId
        }

        override suspend fun hasExecuted(dedupKey: String): Boolean =
            logs.any { it.dedupKey == dedupKey }

        override fun observeLogsForCycle(cycleId: Long): Flow<List<CycleLogEntity>> =
            flowOf(logs.filter { it.cycleId == cycleId })
    }

    private class FakeTransactionDao : TransactionDao {
        val storage = mutableListOf<TransactionEntity>()
        private var seq = 1L

        override suspend fun insert(t: TransactionEntity): Long {
            val id = if (t.id > 0L) t.id else seq++
            storage.add(t.copy(id = id))
            return id
        }

        override suspend fun update(t: TransactionEntity) {}
        override suspend fun delete(t: TransactionEntity) {}
        override suspend fun getById(id: Long): TransactionEntity? = storage.find { it.id == id }
        override fun observeRange(start: Long, end: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getRange(start: Long, end: Long): List<TransactionEntity> = emptyList()
        override suspend fun countSimilarAuto(amount: Double, from: Long, to: Long): Int = 0
        override suspend fun search(keyword: String, category: String?, type: String?, start: Long, end: Long): List<TransactionEntity> = emptyList()
        override suspend fun allCategories(): List<String> = emptyList()
        override suspend fun getAll(): List<TransactionEntity> = emptyList()
        override suspend fun getUnmigratedCategories(): List<TransactionEntity> = emptyList()
        override suspend fun updateCategoryIds(id: Long, parentCategoryId: Int, childCategoryId: Int?) {}
        override suspend fun countUnmigrated(): Int = 0
        override suspend fun reassignAsset(oldAssetId: Long, newAssetId: Long, newAccount: String) {}
        override fun observeExpensesBetween(bookId: Long, start: Long, end: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getExpensesBetween(bookId: Long, start: Long, end: Long): List<TransactionEntity> = emptyList()
    }

    private class FakeTransferDao : TransferDao {
        val storage = mutableListOf<TransferEntity>()
        private var seq = 1L

        override fun getAllFlow(): Flow<List<TransferEntity>> = flowOf(storage)
        override suspend fun findById(id: Long): TransferEntity? = storage.find { it.transferId == id }
        override suspend fun getByAssetId(assetId: Long): List<TransferEntity> =
            storage.filter { it.fromAssetId == assetId || it.toAssetId == assetId }

        override suspend fun insert(transfer: TransferEntity): Long {
            val id = if (transfer.transferId > 0L) transfer.transferId else seq++
            storage.add(transfer.copy(transferId = id))
            return id
        }

        override suspend fun update(transfer: TransferEntity) {
            val idx = storage.indexOfFirst { it.transferId == transfer.transferId }
            if (idx >= 0) storage[idx] = transfer
        }
        override suspend fun delete(transfer: TransferEntity) {
            storage.removeAll { it.transferId == transfer.transferId }
        }
        override suspend fun deleteById(id: Long) {
            storage.removeAll { it.transferId == id }
        }
    }

    private class FakeAssetDao : AssetDao {
        val storage = mutableListOf<AssetEntity>()

        override fun getAllVisibleFlow(): Flow<List<AssetEntity>> = flowOf(emptyList())
        override fun getAllFlow(): Flow<List<AssetEntity>> = flowOf(storage)
        override suspend fun getAll(): List<AssetEntity> = storage
        override suspend fun findById(assetId: Long): AssetEntity? = storage.find { it.assetId == assetId }
        override suspend fun findByName(name: String): AssetEntity? = null
        override suspend fun insert(asset: AssetEntity): Long {
            storage.add(asset)
            return asset.assetId
        }
        override suspend fun insertAll(assets: List<AssetEntity>) {}
        override suspend fun update(asset: AssetEntity) {}
        override suspend fun updateBalance(assetId: Long, delta: Double, timestamp: Long) {
            val idx = storage.indexOfFirst { it.assetId == assetId }
            if (idx >= 0) {
                storage[idx] = storage[idx].copy(assetNumber = storage[idx].assetNumber + delta, updatedAt = timestamp)
            }
        }
        override suspend fun getMaxAssetId(): Long? = 1L
        override suspend fun getMaxPositionWeight(): Int? = 1
        override suspend fun countTransactionsUsingAsset(assetId: Long): Int = 0
        override suspend fun countTransfersUsingAsset(assetId: Long): Int = 0
        override suspend fun updateAssetHide(assetId: Long, hide: Boolean, timestamp: Long) {}
        override suspend fun updatePositionWeight(assetId: Long, weight: Int, timestamp: Long) {}
        override suspend fun deleteById(assetId: Long) {
            storage.removeAll { it.assetId == assetId }
        }
    }

    @Test
    fun `test isCycleMatching for day month and week`() {
        val executor = PeriodicTransactionExecutor(
            database = null,
            cycleDao = FakeCycleDao(),
            transactionDao = FakeTransactionDao(),
            transferDao = FakeTransferDao(),
            assetDao = FakeAssetDao(),
            transactionRunner = { it() },
        )

        val cycleDaily = CycleEntity(cost = 10.0, dateMode = CycleEntity.MODE_EVERY_DAY)
        val cal = Calendar.getInstance()
        assertTrue(executor.isCycleMatching(cycleDaily, cal))

        // 每月 15 号
        cal.set(2026, Calendar.OCTOBER, 15)
        val cycleMonthly = CycleEntity(cost = 100.0, dateMode = CycleEntity.MODE_EVERY_MONTH, monthDay = 15)
        assertTrue(executor.isCycleMatching(cycleMonthly, cal))

        cal.set(2026, Calendar.OCTOBER, 16)
        assertFalse(executor.isCycleMatching(cycleMonthly, cal))

        // 月末最后一天：10月有 31 天
        cal.set(2026, Calendar.OCTOBER, 31)
        val cycleMonthLast = CycleEntity(cost = 200.0, dateMode = CycleEntity.MODE_EVERY_MONTH, monthLast = true)
        assertTrue(executor.isCycleMatching(cycleMonthLast, cal))

        cal.set(2026, Calendar.OCTOBER, 30)
        assertFalse(executor.isCycleMatching(cycleMonthLast, cal))
    }

    @Test
    fun `test executePendingCycles idempotency and balance update`() = runBlocking {
        val cycleDao = FakeCycleDao()
        val txDao = FakeTransactionDao()
        val transferDao = FakeTransferDao()
        val assetDao = FakeAssetDao()
        assetDao.insert(AssetEntity(assetId = 1L, assetName = "现金", assetNumber = 5000.0, totalQuota = 0.0, assetType = 1, intoTotalAsset = true, hide = false, positionWeight = 1, createdAt = 0L, updatedAt = 0L))

        val executor = PeriodicTransactionExecutor(
            database = null,
            cycleDao = cycleDao,
            transactionDao = txDao,
            transferDao = transferDao,
            assetDao = assetDao,
            transactionRunner = { it() },
        )

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        val addTime = cal.timeInMillis

        // 创建一条每天 10 元的周期支出
        val cycle = CycleEntity(
            cycleId = 101L,
            cost = 10.0,
            cycleType = CycleEntity.CYCLE_TYPE_BILL,
            dateMode = CycleEntity.MODE_EVERY_DAY,
            assetId = 1L,
            addTime = addTime,
        )
        cycleDao.insertCycle(cycle)

        // 模拟当前时间为 2026-10-03，应该补记 3 笔 (10月1日, 10月2日, 10月3日)
        cal.set(2026, Calendar.OCTOBER, 3, 12, 0, 0)
        val now = cal.timeInMillis

        val count1 = executor.executePendingCycles(now)
        assertEquals(3, count1)
        assertEquals(3, txDao.storage.size)
        assertEquals(3, cycleDao.logs.size)
        // 账户扣除了 30 元: 5000 - 30 = 4970
        assertEquals(4970.0, assetDao.findById(1L)?.assetNumber ?: 0.0, 0.001)

        // 再次触发执行，因为幂等性，不应该再执行任何账目
        val count2 = executor.executePendingCycles(now)
        assertEquals(0, count2)
        assertEquals(3, txDao.storage.size)
        assertEquals(4970.0, assetDao.findById(1L)?.assetNumber ?: 0.0, 0.001)
    }

    @Test
    fun `test periodic transfer updates both accounts and stops when cycleTimes reached`() = runBlocking {
        val cycleDao = FakeCycleDao()
        val txDao = FakeTransactionDao()
        val transferDao = FakeTransferDao()
        val assetDao = FakeAssetDao()

        assetDao.insert(AssetEntity(assetId = 1L, assetName = "招行", assetNumber = 2000.0, totalQuota = 0.0, assetType = 1, intoTotalAsset = true, hide = false, positionWeight = 1, createdAt = 0L, updatedAt = 0L))
        assetDao.insert(AssetEntity(assetId = 2L, assetName = "微信", assetNumber = 500.0, totalQuota = 0.0, assetType = 1, intoTotalAsset = true, hide = false, positionWeight = 2, createdAt = 0L, updatedAt = 0L))

        val executor = PeriodicTransactionExecutor(
            database = null,
            cycleDao = cycleDao,
            transactionDao = txDao,
            transferDao = transferDao,
            assetDao = assetDao,
            transactionRunner = { it() },
        )

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        val addTime = cal.timeInMillis

        // 创建周期转账：招行转出 200 到微信，限制最多执行 2 次
        val cycle = CycleEntity(
            cycleId = 202L,
            cost = 200.0,
            cycleType = CycleEntity.CYCLE_TYPE_TRANSFER,
            dateMode = CycleEntity.MODE_EVERY_DAY,
            fromAssetId = 1L,
            toAssetId = 2L,
            addTime = addTime,
            cycleTimes = 2,
        )
        cycleDao.insertCycle(cycle)

        cal.set(2026, Calendar.OCTOBER, 5, 12, 0, 0)
        val now = cal.timeInMillis

        // 虽然经过了 5 天，但因为限制了 2 次，只能执行 2 笔
        val count = executor.executePendingCycles(now)
        assertEquals(2, count)
        assertEquals(2, transferDao.storage.size)

        // 招行: 2000 - 400 = 1600; 微信: 500 + 400 = 900
        assertEquals(1600.0, assetDao.findById(1L)?.assetNumber ?: 0.0, 0.001)
        assertEquals(900.0, assetDao.findById(2L)?.assetNumber ?: 0.0, 0.001)

        val updatedCycle = cycleDao.getCycleById(202L)
        assertTrue(updatedCycle?.end == true)
        assertEquals(2, updatedCycle?.addTimes)
    }
}
