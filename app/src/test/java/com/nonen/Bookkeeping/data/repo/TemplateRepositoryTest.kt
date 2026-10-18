package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.ModuleBillDao
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TemplateRepositoryTest {

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
            val idx = storage.indexOfFirst { it.moduleBillId == moduleBill.moduleBillId }
            if (idx >= 0) storage[idx] = moduleBill
        }

        override suspend fun delete(moduleBill: ModuleBillEntity) {
            storage.removeAll { it.moduleBillId == moduleBill.moduleBillId }
        }

        override suspend fun deleteById(id: Long) {
            storage.removeAll { it.moduleBillId == id }
        }

        override fun observeAll(bookId: Long): Flow<List<ModuleBillEntity>> {
            return flowOf(storage.filter { it.bookId == bookId }.sortedBy { it.positionWeight })
        }

        override suspend fun getById(id: Long): ModuleBillEntity? {
            return storage.find { it.moduleBillId == id }
        }

        override suspend fun getMaxPositionWeight(bookId: Long): Int? {
            return storage.filter { it.bookId == bookId }.maxOfOrNull { it.positionWeight }
        }
    }

    @Test
    fun `test template CRUD and position weight assignment`() = runBlocking {
        val dao = FakeModuleBillDao()
        val repo = TemplateRepository(dao)

        val id1 = repo.createTemplate(
            ModuleBillEntity(
                templateName = "早餐",
                cost = 15.0,
                parentCategoryId = 7,
                childCategoryId = 708,
            ),
        )
        val id2 = repo.createTemplate(
            ModuleBillEntity(
                templateName = "中餐",
                cost = 35.0,
                parentCategoryId = 7,
                childCategoryId = 707,
            ),
        )

        assertEquals(1L, id1)
        assertEquals(2L, id2)

        val list = repo.observeAll().first()
        assertEquals(2, list.size)
        assertEquals(1, list[0].positionWeight)
        assertEquals(2, list[1].positionWeight)

        // 更新模板
        val t1 = repo.getById(id1)
        assertNotNull(t1)
        repo.updateTemplate(t1!!.copy(cost = 18.0))
        assertEquals(18.0, repo.getById(id1)?.cost ?: 0.0, 0.001)

        // 删除模板
        repo.deleteTemplate(id1)
        assertNull(repo.getById(id1))
        assertEquals(1, repo.observeAll().first().size)
    }
}
