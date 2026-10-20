package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.CategoryDao
import com.nonen.Bookkeeping.data.db.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据仓库，统一管理一木 76 项默认分类与用户自定义分类。
 */
class CategoryRepository(
    private val categoryDao: CategoryDao,
) {

    fun getAllFlow(): Flow<List<CategoryEntity>> = categoryDao.getAllFlow()

    suspend fun getAll(): List<CategoryEntity> = categoryDao.getAll()

    suspend fun getParentCategories(categoryType: Int): List<CategoryEntity> =
        categoryDao.getParentCategories(categoryType)

    suspend fun getChildCategories(parentId: Int): List<CategoryEntity> =
        categoryDao.getChildCategories(parentId)

    suspend fun findCategory(categoryId: Int, parentId: Int): CategoryEntity? =
        categoryDao.findCategory(categoryId, parentId)

    suspend fun findByName(name: String): CategoryEntity? =
        categoryDao.findByName(name)

    suspend fun findByNameAndParent(name: String, parentId: Int): CategoryEntity? =
        categoryDao.findByNameAndParent(name, parentId)

    suspend fun insert(category: CategoryEntity): Long =
        categoryDao.insert(category)

    suspend fun insertAll(categories: List<CategoryEntity>) =
        categoryDao.insertAll(categories)

    suspend fun update(category: CategoryEntity) =
        categoryDao.update(category)

    suspend fun deleteById(id: Long) =
        categoryDao.deleteById(id)

    suspend fun count(): Int = categoryDao.count()

    fun observeByType(categoryType: Int): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(categoryType)

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun toggleHide(id: Long, hide: Boolean) {
        val cat = categoryDao.getById(id) ?: return
        categoryDao.update(cat.copy(hide = hide))
    }

    suspend fun toggleBookVisibility(id: Long, bookId: Long, hidden: Boolean) {
        val cat = categoryDao.getById(id) ?: return
        categoryDao.update(cat.withBookHidden(bookId, hidden))
    }

    suspend fun updatePositionWeights(items: List<CategoryEntity>) {
        items.forEachIndexed { index, cat ->
            categoryDao.update(cat.copy(positionWeight = items.size - index))
        }
    }

    suspend fun createCustomCategory(
        name: String,
        parentId: Int,
        categoryType: Int,
        iconUrl: String,
    ): CategoryEntity {
        val existing = categoryDao.findByNameAndParent(name, parentId)
        if (existing != null) {
            return existing
        }

        val nextCategoryId: Int = if (parentId == 0) {
            val maxId = categoryDao.getMaxCustomParentCategoryId() ?: 80
            if (maxId in 88..89) maxId + 2 else maxId + 1
        } else {
            val maxId = categoryDao.getMaxCustomChildCategoryId(parentId) ?: (parentId * 100 + 80)
            maxId + 1
        }

        val newCat = CategoryEntity(
            categoryId = nextCategoryId,
            parentId = parentId,
            categoryName = name.trim(),
            iconUrl = iconUrl,
            positionWeight = nextCategoryId,
            categoryType = categoryType,
            isCustom = true,
            hide = false,
            hideBook = null,
        )
        val insertedId = categoryDao.insert(newCat)
        return newCat.copy(id = insertedId)
    }

    suspend fun countTransactionsUsingCategory(categoryId: Int): Int =
        categoryDao.countTransactionsUsingCategory(categoryId)

    suspend fun deleteCustomCategory(id: Long, categoryId: Int, unlinkTransactions: Boolean = true) {
        if (unlinkTransactions) {
            categoryDao.clearTransactionParentCategory(categoryId)
            categoryDao.clearTransactionChildCategory(categoryId)
        }
        categoryDao.deleteById(id)
    }

    /**
     * 确保自定义分类存在：若已存在则直接返回，若不存在则创建并写入数据库。
     */
    suspend fun ensureCustomCategory(
        categoryId: Int,
        parentId: Int,
        name: String,
        categoryType: Int,
        iconUrl: String,
        positionWeight: Int = categoryId,
    ): CategoryEntity {
        val existing = categoryDao.findCategory(categoryId, parentId)
            ?: categoryDao.findByNameAndParent(name, parentId)
        if (existing != null) {
            return existing
        }

        val newCat = CategoryEntity(
            categoryId = categoryId,
            parentId = parentId,
            categoryName = name,
            iconUrl = iconUrl,
            positionWeight = positionWeight,
            categoryType = categoryType,
            isCustom = true,
            hide = false,
            hideBook = null,
        )
        val id = categoryDao.insert(newCat)
        return newCat.copy(id = id)
    }
}
