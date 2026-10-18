package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.ModuleBillDao
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import kotlinx.coroutines.flow.Flow

class TemplateRepository(
    private val moduleBillDao: ModuleBillDao,
    private val transactionRepository: TransactionRepository? = null,
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
        val repo = transactionRepository ?: return null

        return if (t.moduleType == ModuleBillEntity.MODULE_TYPE_BILL) {
            val tx = com.nonen.Bookkeeping.data.db.TransactionEntity(
                amount = -kotlin.math.abs(actualCost),
                category = "模板记账",
                note = t.remark ?: t.templateName,
                timestamp = now,
                source = "template",
                parentCategoryId = t.parentCategoryId,
                childCategoryId = t.childCategoryId,
                assetId = t.assetId,
                bookId = t.bookId,
                transactionType = "EXPENSE",
                hash = "tpl_${t.moduleBillId}_$now",
            )
            val inserted = repo.insertIfNew(tx)
            if (inserted) 1L else null
        } else {
            repo.createTransfer(
                fromAssetId = t.fromAssetId,
                toAssetId = t.toAssetId,
                cost = actualCost,
                toCost = actualCost,
                serviceCharge = t.serviceCharge,
                time = now,
                remark = t.remark ?: t.templateName,
            )
        }
    }
}
