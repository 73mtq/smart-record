package com.nonen.Bookkeeping.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nonen.Bookkeeping.data.RoomRepositoryTest
import com.nonen.Bookkeeping.data.db.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class BudgetRepositoryTest : RoomRepositoryTest() {
    private fun repository() = BudgetRepository(database.budgetDao(), database.transactionDao(), database.categoryDao())

    @Test
    fun monthlyBudgetIsCreatedOnceWithItsCalendarRange() = runBlocking {
        val repo = repository()
        val budget = repo.getOrInitMonthBudget(1L, 2026, 10, 4000.0)

        assertEquals(4000.0, budget.num, 0.001)
        assertEquals(2026, budget.year)
        assertEquals(10, budget.month)
        assertEquals(startOfDay(2026, 10, 1), budget.startTime)
        assertEquals(startOfDay(2026, 11, 1), budget.endTime)
        assertEquals(budget.budgetId, database.budgetDao().getBudget(1L, 2026, 10)?.budgetId)
        assertEquals(budget, repo.getOrInitMonthBudget(1L, 2026, 10, 9000.0))
    }

    @Test
    fun updatingBudgetPreservesItsIdentityAndIncludesAdditionalAmount() = runBlocking {
        val repo = repository()
        val id = repo.setMonthBudget(1L, 2026, 10, 1000.0)
        assertEquals(id, repo.setMonthBudget(1L, 2026, 10, 5000.0, addNum = 500.0))

        val overview = repo.observeBudgetOverview(1L, 2026, 10).first()
        assertEquals(5500.0, overview.totalBudget, 0.001)
        assertEquals(0.0, overview.totalExpense, 0.001)
        assertEquals(5500.0, overview.remainingBudget, 0.001)
        assertFalse(overview.isOverBudget)
    }

    @Test
    fun expenseQueriesUpdateOverBudgetState() = runBlocking {
        val repo = repository()
        repo.setMonthBudget(1L, 2026, 10, 1000.0)
        database.transactionDao().insert(bill(-800.0, "budget-first"))
        var overview = repo.observeBudgetOverview(1L, 2026, 10).first()
        assertEquals(800.0, overview.totalExpense, 0.001)
        assertEquals(200.0, overview.remainingBudget, 0.001)
        assertEquals(0.8f, overview.usageRatio, 0.001f)
        assertFalse(overview.isOverBudget)

        database.transactionDao().insert(bill(-300.0, "budget-second"))
        overview = repo.observeBudgetOverview(1L, 2026, 10).first()
        assertEquals(1100.0, overview.totalExpense, 0.001)
        assertEquals(-100.0, overview.remainingBudget, 0.001)
        assertEquals(1.1f, overview.usageRatio, 0.001f)
        assertTrue(overview.isOverBudget)
    }

    @Test
    fun categoryBudgetCountsOnlyMatchingExpensesWithinTheMonth() = runBlocking {
        val repo = repository()
        val budget = repo.getOrInitMonthBudget(1L, 2026, 10, 3000.0)
        repo.setCategoryBudget(budget.budgetId, 1L, 2026, 10, parentCategoryId = 7, amount = 1000.0)
        database.transactionDao().insertAll(
            listOf(
                bill(-600.0, "category-current"),
                bill(-100.0, "category-next").copy(timestamp = startOfDay(2026, 11, 1)),
                bill(-200.0, "category-other-book").copy(bookId = 2L),
            ),
        )

        val detail = repo.observeBudgetOverview(1L, 2026, 10).first().categoryDetails.single()
        assertEquals(7, detail.parentCategoryId)
        assertEquals(1000.0, detail.budgetAmount, 0.001)
        assertEquals(600.0, detail.spentAmount, 0.001)
        assertEquals(400.0, detail.remainingAmount, 0.001)
        assertEquals(0.6f, detail.usageRatio, 0.001f)
        assertFalse(detail.isOverBudget)
    }

    private fun bill(amount: Double, hash: String) = TransactionEntity(
        amount = amount, category = "食品餐饮", timestamp = startOfDay(2026, 10, 15),
        source = "manual", parentCategoryId = 7, childCategoryId = 702, bookId = 1L, hash = hash,
    )

    private fun startOfDay(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
