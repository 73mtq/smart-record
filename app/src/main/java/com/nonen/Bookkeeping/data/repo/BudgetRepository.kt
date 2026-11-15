package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.BudgetDao
import com.nonen.Bookkeeping.data.db.CategoryDao
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.entity.BudgetEntity
import com.nonen.Bookkeeping.data.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

data class CategoryBudgetDetail(
    val categoryBudgetId: Long,
    val parentCategoryId: Int,
    val parentCategoryName: String,
    val childCategoryId: Int,
    val budgetAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val usageRatio: Float,
    val isOverBudget: Boolean,
)

data class BudgetOverview(
    val budgetId: Long,
    val year: Int,
    val month: Int,
    val totalBudget: Double,
    val totalExpense: Double,
    val remainingBudget: Double,
    val usageRatio: Float,
    val isOverBudget: Boolean,
    val categoryDetails: List<CategoryBudgetDetail>,
)

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao? = null,
) {
    /**
     * 获取或初始化当月预算记录
     */
    suspend fun getOrInitMonthBudget(
        bookId: Long = 1L,
        year: Int,
        month: Int,
        defaultAmount: Double = 0.0,
    ): BudgetEntity {
        val existing = budgetDao.getBudget(bookId, year, month, BudgetEntity.TYPE_MONTH)
        if (existing != null) {
            return existing
        }
        val newBudget = newMonthBudget(bookId, year, month, defaultAmount, addNum = 0.0)
        val id = budgetDao.insertBudget(newBudget)
        return newBudget.copy(budgetId = id)
    }

    /**
     * 设置/更新月度总预算
     */
    suspend fun setMonthBudget(
        bookId: Long = 1L,
        year: Int,
        month: Int,
        amount: Double,
        addNum: Double = 0.0,
    ): Long {
        val existing = budgetDao.getBudget(bookId, year, month, BudgetEntity.TYPE_MONTH)
        val roundAmount = roundToTwoDecimals(amount)
        val roundAdd = roundToTwoDecimals(addNum)
        return if (existing != null) {
            val updated = existing.copy(
                num = roundAmount,
                addNum = roundAdd,
                updateTime = System.currentTimeMillis(),
            )
            budgetDao.updateBudget(updated)
            existing.budgetId
        } else {
            budgetDao.insertBudget(newMonthBudget(bookId, year, month, roundAmount, roundAdd))
        }
    }

    /**
     * 设置/更新分类预算
     */
    suspend fun setCategoryBudget(
        budgetId: Long,
        bookId: Long = 1L,
        year: Int,
        month: Int,
        parentCategoryId: Int,
        childCategoryId: Int = 0,
        amount: Double,
    ): Long {
        val roundAmount = roundToTwoDecimals(amount)
        val existingList = budgetDao.getCategoryBudgets(budgetId)
        val existing = existingList.firstOrNull {
            it.parentCategory == parentCategoryId && it.childCategory == childCategoryId
        }
        return if (existing != null) {
            val updated = existing.copy(
                num = roundAmount,
                updateTime = System.currentTimeMillis(),
            )
            budgetDao.updateCategoryBudget(updated)
            existing.categoryBudgetId
        } else {
            budgetDao.insertCategoryBudget(
                CategoryBudgetEntity(
                    budgetId = budgetId,
                    bookId = bookId,
                    year = year,
                    month = month,
                    parentCategory = parentCategoryId,
                    childCategory = childCategoryId,
                    num = roundAmount,
                    updateTime = System.currentTimeMillis(),
                ),
            )
        }
    }

    /**
     * 实时观察某月预算概览
     */
    fun observeBudgetOverview(
        bookId: Long = 1L,
        year: Int,
        month: Int,
    ): Flow<BudgetOverview> {
        val (startTime, endTime) = monthRange(year, month)
        val budgetFlow = budgetDao.observeBudget(bookId, year, month, BudgetEntity.TYPE_MONTH)
        val expensesFlow = transactionDao.observeExpensesBetween(bookId, startTime, endTime)

        return combine(budgetFlow, expensesFlow) { budget, expenses ->
            val totalExpense = roundToTwoDecimals(expenses.sumOf { abs(it.amount) })
            val budgetTotal = if (budget != null) roundToTwoDecimals(budget.num + budget.addNum) else 0.0
            val remaining = roundToTwoDecimals(budgetTotal - totalExpense)
            val ratio = if (budgetTotal > 0) (totalExpense / budgetTotal).toFloat() else 0f
            val isOver = budgetTotal > 0 && totalExpense > budgetTotal

            val catBudgets = if (budget != null) budgetDao.getCategoryBudgets(budget.budgetId) else emptyList()
            val catDetails = catBudgets.map { cb ->
                val spent = roundToTwoDecimals(
                    expenses.filter { tx ->
                        tx.parentCategoryId == cb.parentCategory &&
                            (cb.childCategory == 0 || tx.childCategoryId == cb.childCategory)
                    }.sumOf { abs(it.amount) },
                )
                val catBudgetTotal = roundToTwoDecimals(cb.num + cb.addNum)
                val catRemaining = roundToTwoDecimals(catBudgetTotal - spent)
                val catRatio = if (catBudgetTotal > 0) (spent / catBudgetTotal).toFloat() else 0f
                val catOver = catBudgetTotal > 0 && spent > catBudgetTotal
                val catName = categoryDao?.findParentCategory(cb.parentCategory)?.categoryName ?: "分类${cb.parentCategory}"

                CategoryBudgetDetail(
                    categoryBudgetId = cb.categoryBudgetId,
                    parentCategoryId = cb.parentCategory,
                    parentCategoryName = catName,
                    childCategoryId = cb.childCategory,
                    budgetAmount = catBudgetTotal,
                    spentAmount = spent,
                    remainingAmount = catRemaining,
                    usageRatio = catRatio,
                    isOverBudget = catOver,
                )
            }

            BudgetOverview(
                budgetId = budget?.budgetId ?: 0L,
                year = year,
                month = month,
                totalBudget = budgetTotal,
                totalExpense = totalExpense,
                remainingBudget = remaining,
                usageRatio = ratio,
                isOverBudget = isOver,
                categoryDetails = catDetails,
            )
        }
    }

    suspend fun deleteCategoryBudget(categoryBudgetId: Long) {
        val existing = budgetDao.getCategoryBudgets(0L).firstOrNull { it.categoryBudgetId == categoryBudgetId }
        if (existing != null) {
            budgetDao.deleteCategoryBudget(existing)
        }
    }

    private fun newMonthBudget(bookId: Long, year: Int, month: Int, amount: Double, addNum: Double): BudgetEntity {
        val (startTime, endTime) = monthRange(year, month)
        return BudgetEntity(
            bookId = bookId,
            budgetName = "${year}年${month}月总预算",
            year = year,
            month = month,
            type = BudgetEntity.TYPE_MONTH,
            startTime = startTime,
            endTime = endTime,
            num = amount,
            addNum = addNum,
            updateTime = System.currentTimeMillis(),
        )
    }

    private fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        return startTime to calendar.timeInMillis
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
