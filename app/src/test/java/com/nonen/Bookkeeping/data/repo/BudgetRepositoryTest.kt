package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.BudgetDao
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.entity.BudgetEntity
import com.nonen.Bookkeeping.data.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BudgetRepositoryTest {

    private class FakeBudgetDao : BudgetDao {
        val budgets = mutableListOf<BudgetEntity>()
        val categoryBudgets = mutableListOf<CategoryBudgetEntity>()
        private var idSeq = 1L

        override suspend fun insertBudget(budget: BudgetEntity): Long {
            val id = if (budget.budgetId > 0L) budget.budgetId else idSeq++
            val entity = budget.copy(budgetId = id)
            budgets.removeAll { it.budgetId == id }
            budgets.add(entity)
            return id
        }

        override suspend fun updateBudget(budget: BudgetEntity) {
            val idx = budgets.indexOfFirst { it.budgetId == budget.budgetId }
            if (idx >= 0) budgets[idx] = budget
        }

        override suspend fun deleteBudget(budget: BudgetEntity) {
            budgets.removeAll { it.budgetId == budget.budgetId }
        }

        override suspend fun getBudget(bookId: Long, year: Int, month: Int, type: Int): BudgetEntity? {
            return budgets.find { it.bookId == bookId && it.year == year && it.month == month && it.type == type }
        }

        override fun observeBudget(bookId: Long, year: Int, month: Int, type: Int): Flow<BudgetEntity?> {
            return flowOf(budgets.find { it.bookId == bookId && it.year == year && it.month == month && it.type == type })
        }

        override suspend fun getBudgetById(budgetId: Long): BudgetEntity? {
            return budgets.find { it.budgetId == budgetId }
        }

        override suspend fun insertCategoryBudget(categoryBudget: CategoryBudgetEntity): Long {
            val id = if (categoryBudget.categoryBudgetId > 0L) categoryBudget.categoryBudgetId else idSeq++
            val entity = categoryBudget.copy(categoryBudgetId = id)
            categoryBudgets.removeAll { it.categoryBudgetId == id }
            categoryBudgets.add(entity)
            return id
        }

        override suspend fun updateCategoryBudget(categoryBudget: CategoryBudgetEntity) {
            val idx = categoryBudgets.indexOfFirst { it.categoryBudgetId == categoryBudget.categoryBudgetId }
            if (idx >= 0) categoryBudgets[idx] = categoryBudget
        }

        override suspend fun deleteCategoryBudget(categoryBudget: CategoryBudgetEntity) {
            categoryBudgets.removeAll { it.categoryBudgetId == categoryBudget.categoryBudgetId }
        }

        override fun observeCategoryBudgets(budgetId: Long): Flow<List<CategoryBudgetEntity>> {
            return flowOf(categoryBudgets.filter { it.budgetId == budgetId })
        }

        override suspend fun getCategoryBudgets(budgetId: Long): List<CategoryBudgetEntity> {
            return categoryBudgets.filter { it.budgetId == budgetId }
        }

        override suspend fun deleteCategoryBudgetsByBudgetId(budgetId: Long) {
            categoryBudgets.removeAll { it.budgetId == budgetId }
        }
    }

    private class FakeTransactionDao : TransactionDao {
        val storage = mutableListOf<TransactionEntity>()

        override suspend fun insert(t: TransactionEntity): Long {
            storage.add(t)
            return t.id
        }

        override suspend fun update(t: TransactionEntity) {}
        override suspend fun delete(t: TransactionEntity) {}
        override suspend fun getById(id: Long): TransactionEntity? = null
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

        override fun observeExpensesBetween(bookId: Long, start: Long, end: Long): Flow<List<TransactionEntity>> {
            return flowOf(storage.filter { it.bookId == bookId && it.transactionType == "EXPENSE" && it.timestamp in start until end })
        }

        override suspend fun getExpensesBetween(bookId: Long, start: Long, end: Long): List<TransactionEntity> {
            return storage.filter { it.bookId == bookId && it.transactionType == "EXPENSE" && it.timestamp in start until end }
        }

        override fun observeFilteredRange(start: Long, end: Long, bookId: Long?, assetId: Long?): Flow<List<TransactionEntity>> =
            flowOf(storage.filter { it.timestamp in start..end && (bookId == null || it.bookId == bookId) && (assetId == null || it.assetId == assetId) })

        override suspend fun getFilteredRange(start: Long, end: Long, bookId: Long?, assetId: Long?): List<TransactionEntity> =
            storage.filter { it.timestamp in start..end && (bookId == null || it.bookId == bookId) && (assetId == null || it.assetId == assetId) }
    }


    @Test
    fun `test getOrInitMonthBudget creates new budget if not exists`() = runBlocking {
        val budgetDao = FakeBudgetDao()
        val txDao = FakeTransactionDao()
        val repo = BudgetRepository(budgetDao, txDao)

        val budget = repo.getOrInitMonthBudget(bookId = 1L, year = 2026, month = 10, defaultAmount = 4000.0)
        assertNotNull(budget)
        assertEquals(4000.0, budget.num, 0.001)
        assertEquals(2026, budget.year)
        assertEquals(10, budget.month)

        val fetched = budgetDao.getBudget(1L, 2026, 10)
        assertEquals(budget.budgetId, fetched?.budgetId)
    }

    @Test
    fun `test setMonthBudget updates existing budget amount`() = runBlocking {
        val budgetDao = FakeBudgetDao()
        val txDao = FakeTransactionDao()
        val repo = BudgetRepository(budgetDao, txDao)

        val bId = repo.setMonthBudget(bookId = 1L, year = 2026, month = 10, amount = 5000.0, addNum = 500.0)
        assertTrue(bId > 0)

        val overview = repo.observeBudgetOverview(bookId = 1L, year = 2026, month = 10).first()
        assertEquals(5500.0, overview.totalBudget, 0.001)
        assertEquals(0.0, overview.totalExpense, 0.001)
        assertEquals(5500.0, overview.remainingBudget, 0.001)
        assertFalse(overview.isOverBudget)
    }

    @Test
    fun `test budget overview calculates expense and over budget condition`() = runBlocking {
        val budgetDao = FakeBudgetDao()
        val txDao = FakeTransactionDao()
        val repo = BudgetRepository(budgetDao, txDao)

        repo.setMonthBudget(bookId = 1L, year = 2026, month = 10, amount = 1000.0)

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
        val time1015 = cal.timeInMillis

        // 记一笔 800 元支出（金额存为 -800）
        txDao.insert(
            TransactionEntity(
                id = 1L,
                amount = -800.0,
                category = "食品餐饮",
                note = "午餐",
                timestamp = time1015,
                source = "manual",
                parentCategoryId = 7,
                childCategoryId = 707,
                transactionType = "EXPENSE",
                bookId = 1L,
                hash = "hash_budget_test_1",
            ),
        )

        var overview = repo.observeBudgetOverview(1L, 2026, 10).first()
        assertEquals(1000.0, overview.totalBudget, 0.001)
        assertEquals(800.0, overview.totalExpense, 0.001)
        assertEquals(200.0, overview.remainingBudget, 0.001)
        assertEquals(0.8f, overview.usageRatio, 0.01f)
        assertFalse(overview.isOverBudget)

        // 再记一笔 300 元支出，触发超支
        txDao.insert(
            TransactionEntity(
                id = 2L,
                amount = -300.0,
                category = "食品餐饮",
                note = "聚餐",
                timestamp = time1015,
                source = "manual",
                parentCategoryId = 7,
                childCategoryId = 702,
                transactionType = "EXPENSE",
                bookId = 1L,
                hash = "hash_budget_test_2",
            ),
        )

        overview = repo.observeBudgetOverview(1L, 2026, 10).first()
        assertEquals(1100.0, overview.totalExpense, 0.001)
        assertEquals(-100.0, overview.remainingBudget, 0.001)
        assertEquals(1.1f, overview.usageRatio, 0.01f)
        assertTrue(overview.isOverBudget)
    }

    @Test
    fun `test category budget calculation`() = runBlocking {
        val budgetDao = FakeBudgetDao()
        val txDao = FakeTransactionDao()
        val repo = BudgetRepository(budgetDao, txDao)

        val budget = repo.getOrInitMonthBudget(1L, 2026, 10, 3000.0)
        repo.setCategoryBudget(
            budgetId = budget.budgetId,
            bookId = 1L,
            year = 2026,
            month = 10,
            parentCategoryId = 7,
            amount = 1000.0,
        )

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 10, 10, 0, 0)
        txDao.insert(
            TransactionEntity(
                id = 1L,
                amount = -600.0,
                category = "食品餐饮",
                note = "请客",
                timestamp = cal.timeInMillis,
                source = "manual",
                parentCategoryId = 7,
                childCategoryId = 702,
                transactionType = "EXPENSE",
                bookId = 1L,
                hash = "hash_budget_test_3",
            ),
        )

        val overview = repo.observeBudgetOverview(1L, 2026, 10).first()
        assertEquals(1, overview.categoryDetails.size)
        val catDetail = overview.categoryDetails.first()
        assertEquals(7, catDetail.parentCategoryId)
        assertEquals(1000.0, catDetail.budgetAmount, 0.001)
        assertEquals(600.0, catDetail.spentAmount, 0.001)
        assertEquals(400.0, catDetail.remainingAmount, 0.001)
        assertEquals(0.6f, catDetail.usageRatio, 0.01f)
        assertFalse(catDetail.isOverBudget)
    }
}
