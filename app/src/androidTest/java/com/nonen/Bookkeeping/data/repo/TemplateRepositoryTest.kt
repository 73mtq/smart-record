package com.nonen.Bookkeeping.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nonen.Bookkeeping.data.RoomRepositoryTest
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateRepositoryTest : RoomRepositoryTest() {
    @Test
    fun templateOrderIsScopedToItsBook() = runBlocking {
        val repo = TemplateRepository(database.moduleBillDao())
        repo.createTemplate(ModuleBillEntity(bookId = 3L, cost = 12.5, templateName = "午饭", parentCategoryId = 7))
        repo.createTemplate(ModuleBillEntity(bookId = 3L, cost = 8.0, templateName = "早餐", parentCategoryId = 7))
        repo.createTemplate(ModuleBillEntity(bookId = 1L, cost = 1.0, templateName = "其他账本"))

        val templates = repo.observeAll(3L).first()
        assertEquals(listOf("午饭", "早餐"), templates.map { it.templateName })
        assertEquals(listOf(1, 2), templates.map { it.positionWeight })
        assertEquals(listOf(3L, 3L), templates.map { it.bookId })
    }

    @Test
    fun deletingOneTemplatePreservesTheOtherTemplate() = runBlocking {
        val repo = TemplateRepository(database.moduleBillDao())
        val firstId = repo.createTemplate(ModuleBillEntity(cost = 1.0, templateName = "甲"))
        repo.createTemplate(ModuleBillEntity(cost = 2.0, templateName = "乙"))

        repo.deleteTemplate(firstId)

        assertEquals(listOf("乙"), repo.observeAll().first().map { it.templateName })
        assertNull(repo.getById(firstId))
    }

    @Test
    fun transferTemplateRetainsBothAccountsAndItsFee() = runBlocking {
        val repo = TemplateRepository(database.moduleBillDao())
        val id = repo.createTemplate(
            ModuleBillEntity(
                cost = 100.0, moduleType = ModuleBillEntity.MODULE_TYPE_TRANSFER,
                fromAssetId = 1L, toAssetId = 2L, serviceCharge = 0.5, templateName = "转账模板",
            ),
        )

        val stored = requireNotNull(repo.getById(id))
        assertEquals(ModuleBillEntity.MODULE_TYPE_TRANSFER, stored.moduleType)
        assertEquals(1L, stored.fromAssetId)
        assertEquals(2L, stored.toAssetId)
        assertEquals(0.5, stored.serviceCharge, 0.0)
    }

    @Test
    fun templateChangesPersistAndRetainTheirSortOrder() = runBlocking {
        val repo = TemplateRepository(database.moduleBillDao())
        val firstId = repo.createTemplate(
            ModuleBillEntity(templateName = "早餐", cost = 15.0, parentCategoryId = 7, childCategoryId = 708),
        )
        val secondId = repo.createTemplate(
            ModuleBillEntity(templateName = "中餐", cost = 35.0, parentCategoryId = 7, childCategoryId = 707),
        )

        assertEquals(1L, firstId)
        assertEquals(2L, secondId)
        val templates = repo.observeAll().first()
        assertEquals(listOf(1, 2), templates.map { it.positionWeight })
        repo.updateTemplate(requireNotNull(repo.getById(firstId)).copy(cost = 18.0))
        assertEquals(18.0, requireNotNull(repo.getById(firstId)).cost, 0.001)

        repo.deleteTemplate(firstId)
        assertNull(repo.getById(firstId))
        assertEquals(listOf(secondId), repo.observeAll().first().map { it.moduleBillId })
    }
}
