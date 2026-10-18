package com.nonen.Bookkeeping.ui.screens

import com.nonen.Bookkeeping.core.CategorizationRules
import com.nonen.Bookkeeping.core.RuleEngine
import com.nonen.Bookkeeping.data.db.CategoryDao
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryManageTest {

    private class FakeCategoryDao : CategoryDao {
        val storage = mutableListOf<CategoryEntity>()
        var unlinkedParentCategories = mutableListOf<Int>()
        var unlinkedChildCategories = mutableListOf<Int>()

        override fun getAllFlow(): Flow<List<CategoryEntity>> = flowOf(storage)
        override suspend fun getAll(): List<CategoryEntity> = storage.toList()
        override fun observeByType(categoryType: Int): Flow<List<CategoryEntity>> =
            flowOf(storage.filter { it.categoryType == categoryType })

        override suspend fun getById(id: Long): CategoryEntity? = storage.find { it.id == id }

        override suspend fun getParentCategories(categoryType: Int): List<CategoryEntity> =
            storage.filter { it.parentId == 0 && it.categoryType == categoryType }

        override suspend fun getChildCategories(parentId: Int): List<CategoryEntity> =
            storage.filter { it.parentId == parentId }

        override suspend fun findCategory(categoryId: Int, parentId: Int): CategoryEntity? =
            storage.find { it.categoryId == categoryId && it.parentId == parentId }

        override suspend fun findByName(name: String): CategoryEntity? =
            storage.find { it.categoryName == name }

        override suspend fun findByNameAndParent(name: String, parentId: Int): CategoryEntity? =
            storage.find { it.categoryName == name && it.parentId == parentId }

        override suspend fun findByNameAndType(name: String, categoryType: Int): CategoryEntity? =
            storage.find { it.categoryName == name && it.categoryType == categoryType }

        override suspend fun getMaxCustomChildCategoryId(parentId: Int): Int? =
            storage.filter { it.isCustom && it.parentId == parentId }.maxOfOrNull { it.categoryId }

        override suspend fun getMaxCustomParentCategoryId(): Int? =
            storage.filter { it.isCustom && it.parentId == 0 }.maxOfOrNull { it.categoryId }

        override suspend fun countTransactionsUsingCategory(categoryId: Int): Int = 5

        override suspend fun clearTransactionParentCategory(categoryId: Int) {
            unlinkedParentCategories.add(categoryId)
        }

        override suspend fun clearTransactionChildCategory(categoryId: Int) {
            unlinkedChildCategories.add(categoryId)
        }

        override suspend fun count(): Int = storage.size

        override suspend fun insert(category: CategoryEntity): Long {
            val assignedId = if (category.id == 0L) (storage.size + 1).toLong() else category.id
            val copy = category.copy(id = assignedId)
            storage.removeAll { it.id == assignedId || (it.categoryId == category.categoryId && it.parentId == category.parentId) }
            storage.add(copy)
            return assignedId
        }

        override suspend fun insertAll(categories: List<CategoryEntity>) {
            categories.forEach { insert(it) }
        }

        override suspend fun update(category: CategoryEntity) {
            val index = storage.indexOfFirst { it.id == category.id }
            if (index != -1) {
                storage[index] = category
            } else {
                storage.add(category)
            }
        }

        override suspend fun deleteById(id: Long) {
            storage.removeAll { it.id == id }
        }
    }

    private class FakeTransactionDao : TransactionDao {
        val storage = mutableListOf<TransactionEntity>()

        override fun observeRange(start: Long, end: Long): Flow<List<TransactionEntity>> = flowOf(storage)
        override suspend fun getById(id: Long): TransactionEntity? = storage.find { it.id == id }
        override suspend fun insert(t: TransactionEntity): Long {
            val id = (storage.size + 1).toLong()
            storage.add(t.copy(id = id))
            return id
        }
        override suspend fun update(t: TransactionEntity) {
            val index = storage.indexOfFirst { it.id == t.id }
            if (index != -1) storage[index] = t
        }
        override suspend fun delete(t: TransactionEntity) {
            storage.removeAll { it.id == t.id }
        }
        override suspend fun getAll(): List<TransactionEntity> = storage.toList()
        override suspend fun getRange(start: Long, end: Long): List<TransactionEntity> = storage.toList()
        override suspend fun search(keyword: String, category: String?, type: String?, start: Long, end: Long): List<TransactionEntity> = storage.toList()
        override suspend fun allCategories(): List<String> = storage.map { it.category }.distinct()
        override suspend fun countSimilarAuto(amount: Double, from: Long, to: Long): Int = 0
        override suspend fun getUnmigratedCategories(): List<TransactionEntity> = emptyList()
        override suspend fun countUnmigrated(): Int = 0
        override suspend fun updateCategoryIds(id: Long, parentCategoryId: Int, childCategoryId: Int?) {
            val index = storage.indexOfFirst { it.id == id }
            if (index != -1) {
                storage[index] = storage[index].copy(parentCategoryId = parentCategoryId, childCategoryId = childCategoryId)
            }
        }
    }

    @Test
    fun `CategoryRepository create custom child category assigns proper ID and persists`() = runBlocking {
        val fakeDao = FakeCategoryDao()
        val repo = CategoryRepository(fakeDao)

        val created = repo.createCustomCategory(
            name = "下午茶",
            parentId = 7,
            categoryType = 0,
            iconUrl = "ic_restaurant_snacks",
        )

        assertEquals("下午茶", created.categoryName)
        assertEquals(7, created.parentId)
        assertTrue("Custom category ID should be > 780", created.categoryId >= 781)
        assertTrue(created.isCustom)
        assertNotNull(fakeDao.getById(created.id))
    }

    @Test
    fun `CategoryRepository deleteCustomCategory unlinks transactions and removes entity`() = runBlocking {
        val fakeDao = FakeCategoryDao()
        val repo = CategoryRepository(fakeDao)

        val cat = repo.createCustomCategory("临时分类", 0, 0, "ic_parent_other")
        val catId = cat.categoryId

        repo.deleteCustomCategory(cat.id, catId, unlinkTransactions = true)

        assertTrue(fakeDao.unlinkedParentCategories.contains(catId))
        assertTrue(fakeDao.unlinkedChildCategories.contains(catId))
        assertEquals(0, fakeDao.storage.size)
    }

    @Test
    fun `AddEditViewModel populates parent and child category IDs on save`() = runBlocking {
        val fakeTxDao = FakeTransactionDao()
        val fakeCatDao = FakeCategoryDao()
        val catRepo = CategoryRepository(fakeCatDao)

        // 播种食品餐饮与早餐
        fakeCatDao.insert(CategoryEntity(id = 1L, categoryId = 7, parentId = 0, categoryName = "食品餐饮", iconUrl = "ic_parent_restaurant", positionWeight = 7, categoryType = 0))
        fakeCatDao.insert(CategoryEntity(id = 2L, categoryId = 708, parentId = 7, categoryName = "早餐", iconUrl = "ic_restaurant_breakfast", positionWeight = 708, categoryType = 0))

        val txRepo = TransactionRepository(
            dao = fakeTxDao,
            ruleDao = object : com.nonen.Bookkeeping.data.db.CategoryRuleDao {
                override fun observeAll(): Flow<List<com.nonen.Bookkeeping.data.db.CategoryRuleEntity>> = flowOf(emptyList())
                override suspend fun getAll(): List<com.nonen.Bookkeeping.data.db.CategoryRuleEntity> = emptyList()
                override suspend fun findByKeyword(keyword: String): com.nonen.Bookkeeping.data.db.CategoryRuleEntity? = null
                override suspend fun countByKeyword(keyword: String): Int = 0
                override suspend fun insert(rule: com.nonen.Bookkeeping.data.db.CategoryRuleEntity): Long = 1L
                override suspend fun insertAll(rules: List<com.nonen.Bookkeeping.data.db.CategoryRuleEntity>) {}
                override suspend fun deleteBuiltin(keyword: String, category: String) {}
                override suspend fun deleteById(id: Long) {}
            },
            merchantDao = object : com.nonen.Bookkeeping.data.db.MerchantCategoryDao {
                override suspend fun getAll(): List<com.nonen.Bookkeeping.data.db.MerchantCategoryEntity> = emptyList()
                override suspend fun find(key: String, isIncome: Boolean): com.nonen.Bookkeeping.data.db.MerchantCategoryEntity? = null
                override fun observeCount(): Flow<Int> = flowOf(0)
                override suspend fun upsert(entity: com.nonen.Bookkeeping.data.db.MerchantCategoryEntity) {}
                override suspend fun clearAll() {}
            },
            settings = null,
        )

        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val vm = AddEditViewModel(
            repo = txRepo,
            categoryRepo = catRepo,
            txId = 0L,
            externalScope = testScope,
        )

        vm.setAmountInput("15.5")
        vm.selectCategoryManually("食品餐饮", 7)
        vm.selectSubCategory("早餐", 708)

        var saved = false
        vm.save { saved = true }

        assertTrue(saved)
        assertEquals(1, fakeTxDao.storage.size)
        val record = fakeTxDao.storage.first()
        assertEquals("食品餐饮", record.category)
        assertEquals("早餐", record.subCategory)
        assertEquals(7, record.parentCategoryId)
        assertEquals(708, record.childCategoryId)
    }
}
