package com.nonen.Bookkeeping.ui.screens

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.DefaultCategories
import com.nonen.Bookkeeping.data.RoomRepositoryTest
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.TemplateRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddEditViewModelTest : RoomRepositoryTest() {
    private lateinit var scope: CoroutineScope
    private lateinit var transactions: TransactionRepository
    private lateinit var assets: AssetRepository
    private lateinit var templates: TemplateRepository

    @Before
    fun prepareRepositories() = runBlocking {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        transactions = TransactionRepository(
            database.transactionDao(), database.categoryRuleDao(), database.merchantCategoryDao(),
            assetDao = database.assetDao(), transferDao = database.transferDao(), database = database,
        )
        assets = AssetRepository(database.assetDao(), database.transferDao(), database.transactionDao(), database)
        templates = TemplateRepository(database.moduleBillDao())
        database.categoryDao().insertAll(DefaultCategories.ALL)
        database.assetDao().insertAll(
            listOf(
                AssetEntity(assetId = 1L, assetName = "现金", assetNumber = 100.0),
                AssetEntity(assetId = 2L, assetName = "银行卡", assetNumber = 50.0),
            ),
        )
    }

    @After
    fun cancelViewModelWork() {
        scope.cancel()
    }

    @Test
    fun addingAndEditingBillsPreserveFieldsIdentityAndAccountBalances() = runBlocking {
        val adding = viewModel()
        withContext(Dispatchers.Main) {
            adding.amount = "12.34"
            adding.selectCategoryManually("食品餐饮", 7)
            adding.selectSubCategory("午餐", 707)
            adding.note = " 午饭 "
            adding.merchant = " 食堂 "
            adding.tag = " 工作日 "
            adding.timestamp = 1771234567890L
        }
        save(adding)
        val original = database.transactionDao().getAll().single()
        assertEquals(-12.34, original.amount, 0.0)
        assertEquals("午饭", original.note)
        assertEquals("食堂", original.merchant)
        assertEquals("工作日", original.tag)
        assertEquals(7, original.parentCategoryId)
        assertEquals(707, original.childCategoryId)
        assertEquals("manual", original.source)
        assertEquals(87.66, requireNotNull(assets.getById(1L)).assetNumber, 0.000001)

        val editing = viewModel(original.id)
        withContext(Dispatchers.Main) {
            editing.amount = "20.00"
            editing.assetId = 2L
            editing.account = "银行卡"
            editing.note = " 修改后的午饭 "
        }
        save(editing)

        val updated = database.transactionDao().getAll().single()
        assertEquals(original.id, updated.id)
        assertEquals(original.hash, updated.hash)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(original.source, updated.source)
        assertEquals(-20.0, updated.amount, 0.0)
        assertEquals("修改后的午饭", updated.note)
        assertEquals(2L, updated.assetId)
        assertEquals("银行卡", updated.account)
        assertEquals(100.0, requireNotNull(assets.getById(1L)).assetNumber, 0.000001)
        assertEquals(30.0, requireNotNull(assets.getById(2L)).assetNumber, 0.000001)
    }

    @Test
    fun duplicateBillShowsItsErrorAndPreservesTheBalance() = runBlocking {
        val first = viewModel()
        withContext(Dispatchers.Main) {
            first.amount = "10"
            first.timestamp = 1771234567890L
            first.merchant = "食堂"
        }
        save(first)
        val second = viewModel()
        var completed = false
        withContext(Dispatchers.Main) {
            second.amount = "10"
            second.timestamp = first.timestamp
            second.merchant = "食堂"
            second.save { completed = true }
        }
        withTimeout(5000) {
            while (second.saving) delay(10)
        }

        assertFalse(completed)
        assertEquals(R.string.error_duplicate_record, second.errorMessageRes)
        assertEquals(1, database.transactionDao().getAll().size)
        assertEquals(90.0, requireNotNull(assets.getById(1L)).assetNumber, 0.0)
    }

    @Test
    fun templateLockIsSavedOnlyWithAPositiveAmount() = runBlocking {
        val vm = viewModel()
        withContext(Dispatchers.Main) {
            vm.amount = "0"
            vm.templateLock = true
        }
        saveTemplate(vm, "零金额模板")
        assertFalse(database.moduleBillDao().getAll().single().moduleLock)

        withContext(Dispatchers.Main) { vm.amount = "15" }
        saveTemplate(vm, "固定金额模板")
        assertTrue(database.moduleBillDao().getAll().single { it.templateName == "固定金额模板" }.moduleLock)
    }

    private suspend fun viewModel(id: Long = 0L): AddEditViewModel {
        val vm = withContext(Dispatchers.Main) {
            AddEditViewModel(
                repo = transactions, categoryRepo = CategoryRepository(database.categoryDao()),
                assetRepo = assets, templateRepo = templates, txId = id, externalScope = scope,
            )
        }
        withTimeout(5000) {
            while (vm.loading || vm.liveAssets.size != 2) delay(10)
        }
        return vm
    }

    private suspend fun save(vm: AddEditViewModel) {
        val completed = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) { vm.save { completed.complete(Unit) } }
        withTimeout(5000) { completed.await() }
    }

    private suspend fun saveTemplate(vm: AddEditViewModel, name: String) {
        val completed = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) { vm.saveCurrentAsTemplate(name) { completed.complete(Unit) } }
        withTimeout(5000) { completed.await() }
    }
}
