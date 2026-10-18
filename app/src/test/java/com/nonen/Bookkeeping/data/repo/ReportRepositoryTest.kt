package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.AssetDao
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.stats.StatsPeriod
import com.nonen.Bookkeeping.stats.StatsQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ReportRepositoryTest {

    private val zone = ZoneId.systemDefault()

    private fun epochMillisOf(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    private class FakeTransactionDao : TransactionDao {
        val storage = mutableListOf<TransactionEntity>()

        override suspend fun insert(t: TransactionEntity): Long {
            storage.add(t)
            return t.id
        }

        override suspend fun update(t: TransactionEntity) {}
        override suspend fun delete(t: TransactionEntity) {}
        override suspend fun getById(id: Long): TransactionEntity? = storage.find { it.id == id }
        override fun observeRange(start: Long, end: Long): Flow<List<TransactionEntity>> =
            flowOf(storage.filter { it.timestamp in start..end })
        override suspend fun getRange(start: Long, end: Long): List<TransactionEntity> =
            storage.filter { it.timestamp in start..end }
        override suspend fun countSimilarAuto(amount: Double, from: Long, to: Long): Int = 0
        override suspend fun search(keyword: String, category: String?, type: String?, start: Long, end: Long): List<TransactionEntity> = emptyList()
        override suspend fun allCategories(): List<String> = emptyList()
        override suspend fun getAll(): List<TransactionEntity> = storage.toList()
        override suspend fun getUnmigratedCategories(): List<TransactionEntity> = emptyList()
        override suspend fun updateCategoryIds(id: Long, parentCategoryId: Int, childCategoryId: Int?) {}
        override suspend fun countUnmigrated(): Int = 0
        override suspend fun reassignAsset(oldAssetId: Long, newAssetId: Long, newAccount: String) {}
        override fun observeExpensesBetween(bookId: Long, start: Long, end: Long): Flow<List<TransactionEntity>> =
            flowOf(storage.filter { it.bookId == bookId && it.amount < 0 && it.timestamp in start..end })
        override suspend fun getExpensesBetween(bookId: Long, start: Long, end: Long): List<TransactionEntity> =
            storage.filter { it.bookId == bookId && it.amount < 0 && it.timestamp in start..end }
        override fun observeFilteredRange(start: Long, end: Long, bookId: Long?, assetId: Long?): Flow<List<TransactionEntity>> =
            flowOf(storage.filter { it.timestamp in start..end && (bookId == null || it.bookId == bookId) && (assetId == null || it.assetId == assetId) })
        override suspend fun getFilteredRange(start: Long, end: Long, bookId: Long?, assetId: Long?): List<TransactionEntity> =
            storage.filter { it.timestamp in start..end && (bookId == null || it.bookId == bookId) && (assetId == null || it.assetId == assetId) }
    }

    private class FakeAssetDao : AssetDao {
        val storage = mutableListOf<AssetEntity>()

        override fun getAllFlow(): Flow<List<AssetEntity>> = flowOf(storage)
        override fun getAllVisibleFlow(): Flow<List<AssetEntity>> = flowOf(storage.filter { !it.hide })
        override suspend fun getAll(): List<AssetEntity> = storage.toList()
        override suspend fun findById(id: Long): AssetEntity? = storage.find { it.assetId == id }
        override suspend fun findByName(name: String): AssetEntity? = storage.find { it.assetName == name }
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
        override suspend fun deleteById(id: Long) {
            storage.removeAll { it.assetId == id }
        }
        override suspend fun updateBalance(id: Long, delta: Double, timestamp: Long) {
            val idx = storage.indexOfFirst { it.assetId == id }
            if (idx >= 0) {
                val current = storage[idx]
                storage[idx] = current.copy(assetNumber = current.assetNumber + delta, updatedAt = timestamp)
            }
        }
        override suspend fun getMaxAssetId(): Long? = storage.maxOfOrNull { it.assetId }
        override suspend fun getMaxPositionWeight(): Int? = storage.maxOfOrNull { it.positionWeight }
        override suspend fun countTransactionsUsingAsset(assetId: Long): Int = 0
        override suspend fun countTransfersUsingAsset(assetId: Long): Int = 0
        override suspend fun updateAssetHide(assetId: Long, hide: Boolean, timestamp: Long) {
            val idx = storage.indexOfFirst { it.assetId == assetId }
            if (idx >= 0) storage[idx] = storage[idx].copy(hide = hide, updatedAt = timestamp)
        }
        override suspend fun updatePositionWeight(assetId: Long, weight: Int, timestamp: Long) {
            val idx = storage.indexOfFirst { it.assetId == assetId }
            if (idx >= 0) storage[idx] = storage[idx].copy(positionWeight = weight, updatedAt = timestamp)
        }
    }

    @Test
    fun `test observeCalendarMonth aggregates daily records from DAO`() = runBlocking {
        val txDao = FakeTransactionDao()
        val assetDao = FakeAssetDao()
        val repo = ReportRepository(txDao, assetDao)

        txDao.insert(
            TransactionEntity(
                id = 1,
                amount = -45.0,
                category = "餐饮",
                source = "manual",
                hash = "h_1",
                timestamp = epochMillisOf(2026, 10, 8, 12),
            )
        )
        txDao.insert(
            TransactionEntity(
                id = 2,
                amount = -55.0,
                category = "餐饮",
                source = "manual",
                hash = "h_2",
                timestamp = epochMillisOf(2026, 10, 8, 19),
            )
        )

        val monthSummaries = repo.observeCalendarMonth(YearMonth.of(2026, 10)).first()
        val day8 = monthSummaries.find { it.date.dayOfMonth == 8 }
        assertNotNull(day8)
        assertEquals(100.0, day8!!.totalExpense, 0.001)
        assertEquals(2, day8.transactionCount)
    }

    @Test
    fun `test getSubCategoryBreakdown decomposes parent category into child percentages`() = runBlocking {
        val txDao = FakeTransactionDao()
        val assetDao = FakeAssetDao()
        val repo = ReportRepository(txDao, assetDao)

        val now = epochMillisOf(2026, 10, 15)
        txDao.insert(
            TransactionEntity(
                id = 1,
                amount = -60.0,
                category = "食品餐饮",
                subCategory = "晚餐",
                source = "manual",
                hash = "h_1",
                timestamp = now,
            )
        )
        txDao.insert(
            TransactionEntity(
                id = 2,
                amount = -40.0,
                category = "食品餐饮",
                subCategory = "午餐",
                source = "manual",
                hash = "h_2",
                timestamp = now,
            )
        )

        val start = epochMillisOf(2026, 10, 1)
        val end = epochMillisOf(2026, 10, 31)

        val breakdown = repo.getSubCategoryBreakdown("食品餐饮", start, end)
        assertEquals(2, breakdown.size)
        assertEquals("晚餐", breakdown[0].name)
        assertEquals(60.0, breakdown[0].amount, 0.001)
        assertEquals(0.6f, breakdown[0].percentInParent, 0.001f)

        assertEquals("午餐", breakdown[1].name)
        assertEquals(40.0, breakdown[1].amount, 0.001)
        assertEquals(0.4f, breakdown[1].percentInParent, 0.001f)
    }

    @Test
    fun `test getStats with assetId filter`() = runBlocking {
        val txDao = FakeTransactionDao()
        val assetDao = FakeAssetDao()
        val repo = ReportRepository(txDao, assetDao)

        val now = epochMillisOf(2026, 10, 15)
        txDao.insert(
            TransactionEntity(
                id = 1,
                amount = -100.0,
                category = "购物",
                assetId = 1L,
                source = "manual",
                hash = "h_1",
                timestamp = now,
            )
        )
        txDao.insert(
            TransactionEntity(
                id = 2,
                amount = -200.0,
                category = "购物",
                assetId = 2L,
                source = "manual",
                hash = "h_2",
                timestamp = now,
            )
        )

        val query = StatsQuery(
            period = StatsPeriod.MONTH,
            isIncome = false,
            assetId = 1L,
        )

        val stats = repo.getStats(query, today = LocalDate.of(2026, 10, 15))
        assertNotNull(stats)
        assertEquals(100.0, stats!!.total, 0.001)
        assertEquals(1, stats.count)
    }
}

