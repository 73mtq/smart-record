package com.nonen.Bookkeeping.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 预算主实体表
 */
@Entity(
    tableName = "budgets",
    indices = [
        Index("bookId"),
        Index(value = ["bookId", "year", "month", "type"], unique = true),
    ],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val budgetId: Long = 0L,
    val bookId: Long = 1L,
    val budgetName: String = "月度总预算",
    val year: Int,
    val month: Int,
    val type: Int = TYPE_MONTH,
    val startTime: Long,
    val endTime: Long,
    val num: Double,
    val addNum: Double = 0.0,
    val positionWeight: Int = 0,
    val updateTime: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TYPE_MONTH = 0
        const val TYPE_SELF = 1
    }
}

/**
 * 分类预算子实体表
 */
@Entity(
    tableName = "category_budgets",
    indices = [
        Index("budgetId"),
        Index("bookId"),
        Index("parentCategory"),
        Index(value = ["budgetId", "parentCategory", "childCategory"], unique = true),
    ],
)
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryBudgetId: Long = 0L,
    val budgetId: Long,
    val bookId: Long = 1L,
    val year: Int,
    val month: Int,
    val parentCategory: Int,
    val childCategory: Int = 0,
    val num: Double,
    val addNum: Double = 0.0,
    val positionWeight: Int = 0,
    val updateTime: Long = System.currentTimeMillis(),
)
