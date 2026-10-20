package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.ModuleBillDao
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import kotlinx.coroutines.flow.Flow

class TemplateRepository(
    private val moduleBillDao: ModuleBillDao,
    private val transactionRepository: TransactionRepository? = null,
    private val categoryRepository: CategoryRepository? = null,
    private val assetRepository: AssetRepository? = null,
) {
    fun observeAll(bookId: Long = 1L): Flow<List<ModuleBillEntity>> {
        return moduleBillDao.observeAll(bookId)
    }

    suspend fun getById(id: Long): ModuleBillEntity? {
        return moduleBillDao.getById(id)
    }

    suspend fun createTemplate(template: ModuleBillEntity): Long {
        val maxWeight = moduleBillDao.getMaxPositionWeight(template.bookId) ?: 0
        return moduleBillDao.insert(template.copy(positionWeight = maxWeight + 1))
    }

    suspend fun updateTemplate(template: ModuleBillEntity) {
        moduleBillDao.update(template)
    }

    suspend fun deleteTemplate(id: Long) {
        moduleBillDao.deleteById(id)
    }

    /**
     * 根据模板一键记账入库
     */
    suspend fun applyTemplate(
        templateId: Long,
        overrideCost: Double? = null,
        now: Long = System.currentTimeMillis(),
    ): Long? {
        val t = moduleBillDao.getById(templateId) ?: return null
        val actualCost = overrideCost ?: t.cost
        require(actualCost.isFinite() && actualCost > 0.0)
        val repo = requireNotNull(transactionRepository)

        return if (t.moduleType == ModuleBillEntity.MODULE_TYPE_BILL) {
            val categories = requireNotNull(categoryRepository).getAll()
            val parent = requireNotNull(categories.firstOrNull { it.parentId == 0 && it.categoryId == t.parentCategoryId })
            val child = categories.firstOrNull { it.parentId == parent.categoryId && it.categoryId == t.childCategoryId }
            val asset = requireNotNull(requireNotNull(assetRepository).getAll().firstOrNull { it.assetId == t.assetId })
            val income = parent.categoryType == 1
            val tx = com.nonen.Bookkeeping.data.db.TransactionEntity(
                amount = if (income) actualCost else -actualCost,
                category = parent.categoryName,
                subCategory = child?.categoryName,
                account = asset.assetName,
                tag = t.tags,
                note = t.remark ?: t.templateName,
                timestamp = now,
                source = "template",
                parentCategoryId = t.parentCategoryId,
                childCategoryId = t.childCategoryId,
                assetId = t.assetId,
                bookId = t.bookId,
                transactionType = if (income) "INCOME" else "EXPENSE",
                hash = "tpl_${t.moduleBillId}_$now",
            )
            val inserted = repo.insertIfNew(tx)
            if (inserted) 1L else null
        } else {
            check(requireNotNull(assetRepository).executeTransfer(
                fromAssetId = t.fromAssetId,
                toAssetId = t.toAssetId,
                amount = actualCost,
                serviceCharge = t.serviceCharge,
                time = now,
                remark = t.remark ?: t.templateName,
            ))
            1L
        }
    }
}
