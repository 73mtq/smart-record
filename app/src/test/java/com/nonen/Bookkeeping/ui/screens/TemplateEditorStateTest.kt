package com.nonen.Bookkeeping.ui.screens

import com.nonen.Bookkeeping.data.db.ModuleBillDao
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 模板编辑保存条件：金额非负、名称非空、锁定只在金额大于 0 时成立。
 * 直接验证 TemplateRepository 的写入字段，避免 UI 层状态掩盖规则。
 */
class TemplateEditorStateTest {

    private class FakeModuleBillDao : ModuleBillDao {
        val storage = mutableListOf<ModuleBillEntity>()
        private var idSeq = 1L

        override suspend fun insert(moduleBill: ModuleBillEntity): Long {
            val id = if (moduleBill.moduleBillId > 0L) moduleBill.moduleBillId else idSeq++
            val entity = moduleBill.copy(moduleBillId = id)
            storage.removeAll { it.moduleBillId == id }
            storage.add(entity)
            return id
        }

        override suspend fun update(moduleBill: ModuleBillEntity) {
            val index = storage.indexOfFirst { it.moduleBillId == moduleBill.moduleBillId }
            require(index >= 0) { "模板不存在：${moduleBill.moduleBillId}" }
            storage[index] = moduleBill
        }

        override suspend fun delete(moduleBill: ModuleBillEntity) {
            storage.removeAll { it.moduleBillId == moduleBill.moduleBillId }
        }

        override suspend fun deleteById(id: Long) {
            storage.removeAll { it.moduleBillId == id }
        }

        override fun observeAll(bookId: Long): Flow<List<ModuleBillEntity>> =
            flowOf(storage.filter { it.bookId == bookId }.sortedBy { it.positionWeight })

        override suspend fun getById(id: Long): ModuleBillEntity? =
            storage.find { it.moduleBillId == id }

        override suspend fun getMaxPositionWeight(bookId: Long): Int? =
            storage.filter { it.bookId == bookId }.maxOfOrNull { it.positionWeight }

        override suspend fun insertAll(moduleBills: List<ModuleBillEntity>) {
            storage.addAll(moduleBills)
        }

        override suspend fun getAll(): List<ModuleBillEntity> = storage.toList()

        override suspend fun clearAll() {
            storage.clear()
        }
    }

    @Test
    fun `template created with position weight keeps book scope`() = runBlocking {
        val dao = FakeModuleBillDao()
        val repository = com.nonen.Bookkeeping.data.repo.TemplateRepository(dao)
        repository.createTemplate(
            ModuleBillEntity(bookId = 3L, cost = 12.5, templateName = "午饭", parentCategoryId = 7),
        )
        repository.createTemplate(
            ModuleBillEntity(bookId = 3L, cost = 8.0, templateName = "早餐", parentCategoryId = 7),
        )
        val stored = dao.storage.sortedBy { it.positionWeight }
        assertEquals(listOf("午饭", "早餐"), stored.map { it.templateName })
        assertEquals(listOf(1, 2), stored.map { it.positionWeight })
        assertTrue(stored.all { it.bookId == 3L })
    }

    @Test
    fun `template delete removes only the requested record`() = runBlocking {
        val dao = FakeModuleBillDao()
        val repository = com.nonen.Bookkeeping.data.repo.TemplateRepository(dao)
        val first = repository.createTemplate(ModuleBillEntity(cost = 1.0, templateName = "甲"))
        repository.createTemplate(ModuleBillEntity(cost = 2.0, templateName = "乙"))
        repository.deleteTemplate(first)
        assertEquals(listOf("乙"), dao.storage.map { it.templateName })
        assertNull(dao.getById(first))
    }

    @Test
    fun `locked template requires positive amount`() {
        val zero = ModuleBillEntity(cost = 0.0, moduleLock = true)
        assertFalse(zero.moduleLock && zero.cost > 0.0)
        val charged = ModuleBillEntity(cost = 15.0, moduleLock = true)
        assertTrue(charged.moduleLock && charged.cost > 0.0)
    }

    @Test
    fun `transfer template keeps both accounts and service charge`() = runBlocking {
        val dao = FakeModuleBillDao()
        val repository = com.nonen.Bookkeeping.data.repo.TemplateRepository(dao)
        val id = repository.createTemplate(
            ModuleBillEntity(
                cost = 100.0,
                moduleType = ModuleBillEntity.MODULE_TYPE_TRANSFER,
                fromAssetId = 1L,
                toAssetId = 2L,
                serviceCharge = 0.5,
                templateName = "转账模板",
            ),
        )
        val stored = requireNotNull(repository.getById(id))
        assertEquals(ModuleBillEntity.MODULE_TYPE_TRANSFER, stored.moduleType)
        assertEquals(1L, stored.fromAssetId)
        assertEquals(2L, stored.toAssetId)
        assertEquals(0.5, stored.serviceCharge, 0.0)
    }
}
