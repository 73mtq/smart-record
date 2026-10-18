package com.nonen.Bookkeeping.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nonen.Bookkeeping.data.entity.BudgetEntity
import com.nonen.Bookkeeping.data.entity.CategoryBudgetEntity
import com.nonen.Bookkeeping.data.entity.CycleEntity
import com.nonen.Bookkeeping.data.entity.CycleLogEntity
import com.nonen.Bookkeeping.data.entity.DreamEntity
import com.nonen.Bookkeeping.data.entity.DreamLogEntity
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE bookId = :bookId AND year = :year AND month = :month AND type = :type LIMIT 1")
    suspend fun getBudget(bookId: Long, year: Int, month: Int, type: Int = 0): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE bookId = :bookId AND year = :year AND month = :month AND type = :type LIMIT 1")
    fun observeBudget(bookId: Long, year: Int, month: Int, type: Int = 0): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE budgetId = :budgetId LIMIT 1")
    suspend fun getBudgetById(budgetId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(categoryBudget: CategoryBudgetEntity): Long

    @Update
    suspend fun updateCategoryBudget(categoryBudget: CategoryBudgetEntity)

    @Delete
    suspend fun deleteCategoryBudget(categoryBudget: CategoryBudgetEntity)

    @Query("SELECT * FROM category_budgets WHERE budgetId = :budgetId ORDER BY positionWeight ASC, categoryBudgetId ASC")
    fun observeCategoryBudgets(budgetId: Long): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets WHERE budgetId = :budgetId ORDER BY positionWeight ASC, categoryBudgetId ASC")
    suspend fun getCategoryBudgets(budgetId: Long): List<CategoryBudgetEntity>

    @Query("DELETE FROM category_budgets WHERE budgetId = :budgetId")
    suspend fun deleteCategoryBudgetsByBudgetId(budgetId: Long)
}

@Dao
interface CycleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleEntity): Long

    @Update
    suspend fun updateCycle(cycle: CycleEntity)

    @Delete
    suspend fun deleteCycle(cycle: CycleEntity)

    @Query("DELETE FROM cycles WHERE cycleId = :cycleId")
    suspend fun deleteCycleById(cycleId: Long)

    @Query("SELECT * FROM cycles WHERE bookId = :bookId ORDER BY cycleId DESC")
    fun observeAllCycles(bookId: Long = 1L): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycles WHERE pause = 0 AND `end` = 0")
    suspend fun getActiveCycles(): List<CycleEntity>

    @Query("SELECT * FROM cycles WHERE cycleId = :cycleId LIMIT 1")
    suspend fun getCycleById(cycleId: Long): CycleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: CycleLogEntity): Long

    @Query("SELECT COUNT(*) > 0 FROM cycle_logs WHERE dedupKey = :dedupKey")
    suspend fun hasExecuted(dedupKey: String): Boolean

    @Query("SELECT * FROM cycle_logs WHERE cycleId = :cycleId ORDER BY executedTime DESC")
    fun observeLogsForCycle(cycleId: Long): Flow<List<CycleLogEntity>>
}

@Dao
interface ModuleBillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(moduleBill: ModuleBillEntity): Long

    @Update
    suspend fun update(moduleBill: ModuleBillEntity)

    @Delete
    suspend fun delete(moduleBill: ModuleBillEntity)

    @Query("DELETE FROM module_bills WHERE moduleBillId = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM module_bills WHERE bookId = :bookId ORDER BY positionWeight ASC, moduleBillId DESC")
    fun observeAll(bookId: Long = 1L): Flow<List<ModuleBillEntity>>

    @Query("SELECT * FROM module_bills WHERE moduleBillId = :id LIMIT 1")
    suspend fun getById(id: Long): ModuleBillEntity?

    @Query("SELECT MAX(positionWeight) FROM module_bills WHERE bookId = :bookId")
    suspend fun getMaxPositionWeight(bookId: Long = 1L): Int?
}

@Dao
interface DreamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDream(dream: DreamEntity): Long

    @Update
    suspend fun updateDream(dream: DreamEntity)

    @Delete
    suspend fun deleteDream(dream: DreamEntity)

    @Query("DELETE FROM dreams WHERE dreamId = :id")
    suspend fun deleteDreamById(id: Long)

    @Query("SELECT * FROM dreams ORDER BY positionWeight ASC, dreamId DESC")
    fun observeAllDreams(): Flow<List<DreamEntity>>

    @Query("SELECT * FROM dreams WHERE dreamId = :id LIMIT 1")
    suspend fun getDreamById(id: Long): DreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DreamLogEntity): Long

    @Delete
    suspend fun deleteLog(log: DreamLogEntity)

    @Query("SELECT * FROM dream_logs WHERE dreamId = :dreamId ORDER BY time DESC")
    fun observeLogsForDream(dreamId: Long): Flow<List<DreamLogEntity>>

    @Query("SELECT * FROM dream_logs WHERE dreamId = :dreamId ORDER BY time DESC")
    suspend fun getLogsForDream(dreamId: Long): List<DreamLogEntity>

    @Query("DELETE FROM dream_logs WHERE dreamId = :dreamId")
    suspend fun deleteLogsByDreamId(dreamId: Long)
}
