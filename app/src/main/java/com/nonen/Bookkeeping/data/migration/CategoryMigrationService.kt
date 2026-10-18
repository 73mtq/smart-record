package com.nonen.Bookkeeping.data.migration

import com.nonen.Bookkeeping.core.CategoryMapping
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.CategoryDao
import com.nonen.Bookkeeping.data.db.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 历史分类迁移报告。
 */
data class CategoryMigrationResult(
    val totalTransactions: Int,
    val migratedCount: Int,
    val alreadyMigratedCount: Int,
    val customCategoriesSeeded: Int,
    val durationMs: Long,
)

/**
 * 历史分类数据迁移服务：
 * 1. 播种迁移所需的自定义分类（保证原意不丢失）；
 * 2. 遍历尚未回填 parentCategoryId 的账单，依据映射矩阵更新关联外键；
 * 3. 严格保持交易金额、时间戳、商户、哈希等数据无损。
 */
class CategoryMigrationService(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) {

    constructor(database: AppDatabase) : this(
        categoryDao = database.categoryDao(),
        transactionDao = database.transactionDao(),
    )

    suspend fun migrate(): CategoryMigrationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. 播种迁移所需的自定义分类
        var seededCount = 0
        CategoryMapping.CUSTOM_SEED_CATEGORIES.forEach { customCat ->
            val existing = categoryDao.findCategory(customCat.categoryId, customCat.parentId)
            if (existing == null) {
                categoryDao.insert(customCat)
                seededCount++
            }
        }

        // 2. 查询所有未完成分类外键关联的交易记录
        val unmigratedList = transactionDao.getUnmigratedCategories()
        var migratedCount = 0

        for (tx in unmigratedList) {
            val isIncome = tx.amount >= 0 || tx.transactionType == "INCOME"
            val mapping = CategoryMapping.resolve(
                category = tx.category,
                subCategory = tx.subCategory,
                isIncome = isIncome,
            )

            transactionDao.updateCategoryIds(
                id = tx.id,
                parentCategoryId = mapping.parentId,
                childCategoryId = mapping.childId,
            )
            migratedCount++
        }

        val allTransactions = transactionDao.getAll()
        val alreadyMigrated = allTransactions.size - unmigratedList.size

        CategoryMigrationResult(
            totalTransactions = allTransactions.size,
            migratedCount = migratedCount,
            alreadyMigratedCount = alreadyMigrated,
            customCategoriesSeeded = seededCount,
            durationMs = System.currentTimeMillis() - startTime,
        )
    }
}
