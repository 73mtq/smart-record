package com.nonen.Bookkeeping.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nonen.Bookkeeping.BookkeepingApp
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateFixtureInstrumentationTest {
    private val app: BookkeepingApp
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BookkeepingApp

    private val repository
        get() = app.container.templateRepository

    @Test
    fun insertTemplate() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName ==
            "com.nonen.Bookkeeping.uivalidation")
        runBlocking {
            repository.observeAll(1L).first()
                .filter { it.templateName == TEMPLATE_NAME }
                .forEach { repository.deleteTemplate(it.moduleBillId) }
            repository.createTemplate(
                ModuleBillEntity(
                    bookId = 1L,
                    moduleType = ModuleBillEntity.MODULE_TYPE_BILL,
                    templateName = TEMPLATE_NAME,
                    cost = 12.34,
                    parentCategoryId = 7,
                    childCategoryId = 708,
                    assetId = 0L,
                    moduleLock = false,
                ),
            )
            val templates = repository.observeAll(1L).first()
            assertEquals(1, templates.count { it.templateName == TEMPLATE_NAME })
            assertEquals(12.34, templates.first { it.templateName == TEMPLATE_NAME }.cost, 0.001)
        }
    }

    @Test
    fun deleteTemplate() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName ==
            "com.nonen.Bookkeeping.uivalidation")
        runBlocking {
            repository.observeAll(1L).first()
                .filter { it.templateName == TEMPLATE_NAME }
                .forEach { repository.deleteTemplate(it.moduleBillId) }
            assertEquals(0, repository.observeAll(1L).first().count { it.templateName == TEMPLATE_NAME })
        }
    }

    companion object {
        private const val TEMPLATE_NAME = "食品餐饮-早餐"
    }
}
