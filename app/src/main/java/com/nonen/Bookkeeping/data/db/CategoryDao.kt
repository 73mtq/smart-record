package com.nonen.Bookkeeping.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 分类持久化数据访问接口。
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY positionWeight ASC, categoryId ASC")
    fun getAllFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY positionWeight ASC, categoryId ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE parentId = 0 AND categoryType = :categoryType ORDER BY positionWeight ASC, categoryId ASC")
    suspend fun getParentCategories(categoryType: Int): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY positionWeight ASC, categoryId ASC")
    suspend fun getChildCategories(parentId: Int): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId AND parentId = :parentId LIMIT 1")
    suspend fun findCategory(categoryId: Int, parentId: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE categoryName = :name LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE categoryName = :name AND parentId = :parentId LIMIT 1")
    suspend fun findByNameAndParent(name: String, parentId: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE categoryName = :name AND categoryType = :categoryType LIMIT 1")
    suspend fun findByNameAndType(name: String, categoryType: Int): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
