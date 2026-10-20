package com.nonen.Bookkeeping.ui

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.BookEntity
import com.nonen.Bookkeeping.data.repo.TemplateRepository
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.ui.screens.AddEditViewModel
import com.nonen.Bookkeeping.ui.screens.AmountKeyboardState
import com.nonen.Bookkeeping.ui.screens.TemplateViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class YimuAddEditDateInstrumentationTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var databaseName: String
    private lateinit var transactions: TransactionRepository
    private lateinit var categories: CategoryRepository
    private lateinit var assets: AssetRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.nonen.Bookkeeping.uivalidation")
        val source = File(context.getExternalFilesDir(null), "yimu-validation-source.db")
        val expectedHash = requireNotNull(InstrumentationRegistry.getArguments().getString("source_sha256"))
        val actualHash = MessageDigest.getInstance("SHA-256").digest(source.readBytes())
            .joinToString("") { "%02x".format(it) }
        check(actualHash == expectedHash)
        databaseName = "yimu-add-edit-${UUID.randomUUID()}.db"
        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).createFromFile(source).build()
        transactions = TransactionRepository(
            database.transactionDao(), database.categoryRuleDao(), database.merchantCategoryDao(),
            assetDao = database.assetDao(), transferDao = database.transferDao(), database = database,
        )
        categories = CategoryRepository(database.categoryDao())
        assets = AssetRepository(database.assetDao(), database.transferDao(), database.transactionDao(), database)
    }

    @After
    fun tearDown() {
        scope.cancel()
        if (::database.isInitialized) database.close()
        if (::databaseName.isInitialized) check(context.deleteDatabase(databaseName))
    }

    @Test
    fun calendarDatePrefillPersistsThroughActualSave(): Unit = runBlocking {
        val date = LocalDate.of(2024, 2, 29)
        assertEquals("add/0?date=2024-02-29", Routes.add(0L, date))
        assertEquals("add/0", Routes.add(0L))
        val vm = withContext(Dispatchers.Main) {
            AddEditViewModel(
                repo = transactions, categoryRepo = categories, assetRepo = assets, txId = 0L,
                externalScope = scope, initialDate = date,
            )
        }
        withTimeout(10_000) {
            while (vm.liveCategories.isEmpty() || vm.liveAssets.isEmpty()) delay(20)
        }
        assertEquals(date, Instant.ofEpochMilli(vm.timestamp).atZone(ZoneId.systemDefault()).toLocalDate())
        val category = vm.liveCategories.first { it.parentId == 0 }
        val note = "S10日期验证-${UUID.randomUUID()}"
        val done = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) {
            vm.selectCategoryManually(category.categoryName, category.categoryId)
            vm.updateKeyboard(AmountKeyboardState("1.25"))
            vm.note = note
            vm.save { done.complete(Unit) }
        }
        withTimeout(10_000) { done.await() }
        val saved = transactions.getAll().single { it.note == note }
        assertEquals(date, Instant.ofEpochMilli(saved.timestamp).atZone(ZoneId.systemDefault()).toLocalDate())
        assertEquals(-1.25, saved.amount, 0.001)
    }

    @Test
    fun incomeEditLoadsIncomeCategoriesAndRetainsSelectedClassification(): Unit = runBlocking {
        val original = transactions.getAll().first { it.amount > 0.0 }
        val vm = withContext(Dispatchers.Main) {
            AddEditViewModel(
                repo = transactions, categoryRepo = categories, assetRepo = assets, txId = original.id,
                externalScope = scope,
            )
        }
        withTimeout(10_000) {
            while (vm.loading || vm.liveCategories.none { it.categoryType == 1 }) delay(20)
        }
        val expected = categories.getAll().filter { it.categoryType == 1 && !it.isHiddenInBook(original.bookId) }
        assertTrue(vm.isIncome)
        assertEquals(expected, vm.liveCategories)
        assertEquals(original.parentCategoryId, vm.parentCategoryId)
        assertEquals(original.childCategoryId, vm.childCategoryId)
        assertEquals(original.timestamp, vm.timestamp)
    }

    @Test
    fun templateEditorCreatesAndUpdatesIncomeAndExpenseWithBookAccountTagsAndLock(): Unit = runBlocking {
        val templates = TemplateRepository(database.moduleBillDao(), transactions, categories, assets)
        val bookId = database.bookDao().insert(BookEntity(bookName = "S10模板账本"))
        val accountId = addAccount("S10模板账户", 500.0)
        val account = requireNotNull(database.assetDao().findById(accountId))
        for (income in listOf(false, true)) {
            val parent = categories.getParentCategories(if (income) 1 else 0).first()
            val child = categories.getChildCategories(parent.categoryId).firstOrNull()
            val vm = templateEditor(templates)
            awaitReady(vm)
            val name = "S10模板-${UUID.randomUUID()}"
            withContext(Dispatchers.Main) {
                vm.setType(income)
                vm.selectBook(bookId)
                vm.selectCategoryManually(parent.categoryName, parent.categoryId)
                child?.let { vm.selectSubCategory(it.categoryName, it.categoryId) }
                vm.selectAsset(account)
                vm.updateKeyboard(AmountKeyboardState("5.25"))
                vm.note = "S10模板备注"
                vm.tag = "S10模板标签"
                vm.templateLock = true
            }
            saveTemplate(vm, name)
            val created = database.moduleBillDao().getAll().single { it.templateName == name }
            assertEquals(bookId, created.bookId)
            assertEquals(accountId, created.assetId)
            assertEquals(parent.categoryId, created.parentCategoryId)
            assertEquals(child?.categoryId, created.childCategoryId)
            assertEquals("S10模板标签", created.tags)
            assertTrue(created.moduleLock)
            templates.updateTemplate(created.copy(notIntoBudget = true, notIntoTotal = true))

            val editor = templateEditor(templates, created.moduleBillId)
            awaitReady(editor)
            assertEquals(income, editor.isIncome)
            assertEquals(bookId, editor.bookId)
            assertEquals(accountId, editor.assetId)
            assertEquals("S10模板标签", editor.tag)
            assertTrue(editor.templateLock)
            withContext(Dispatchers.Main) {
                editor.updateKeyboard(AmountKeyboardState("7.25"))
                editor.tag = "S10更新标签"
            }
            saveTemplate(editor, name)
            val updated = requireNotNull(templates.getById(created.moduleBillId))
            assertEquals(1, database.moduleBillDao().getAll().count { it.templateName == name })
            assertEquals(7.25, updated.cost, 0.001)
            assertEquals("S10更新标签", updated.tags)
            assertTrue(updated.moduleLock && updated.notIntoBudget && updated.notIntoTotal)
            val time = System.currentTimeMillis()
            templates.applyTemplate(updated.moduleBillId, now = time)
            val saved = transactions.getAll().single { it.hash == "tpl_${updated.moduleBillId}_$time" }
            assertEquals(if (income) 7.25 else -7.25, saved.amount, 0.001)
            assertEquals(bookId, saved.bookId)
            assertEquals(accountId, saved.assetId)
            assertEquals("S10更新标签", saved.tag)

            val selectedBook = MutableStateFlow(bookId)
            val list = TemplateViewModel(templates, selectedBook, scope)
            withTimeout(10_000) { list.templates.first { entries -> entries.any { it.moduleBillId == updated.moduleBillId } } }
            selectedBook.value = 1L
            withTimeout(10_000) { list.templates.first { entries -> entries.none { it.moduleBillId == updated.moduleBillId } } }
        }
    }

    @Test
    fun transferTemplatePrefillEditAndSaveRetainServiceChargeAndBalances(): Unit = runBlocking {
        val templates = TemplateRepository(database.moduleBillDao(), transactions, categories, assets)
        val fromId = addAccount("S10转出账户", 500.0)
        val toId = addAccount("S10转入账户", 100.0)
        val vm = templateEditor(templates)
        awaitReady(vm)
        val name = "S10转账模板-${UUID.randomUUID()}"
        withContext(Dispatchers.Main) {
            vm.setTransferType()
            vm.selectAsset(requireNotNull(vm.liveAssets.firstOrNull { it.assetId == fromId }))
            vm.transferToAssetId = toId
            vm.transferServiceCharge = 2.5
            vm.updateKeyboard(AmountKeyboardState("30.00"))
            vm.templateLock = true
        }
        saveTemplate(vm, name)
        val created = database.moduleBillDao().getAll().single { it.templateName == name }
        assertEquals(1, created.moduleType)
        assertEquals(fromId, created.fromAssetId)
        assertEquals(toId, created.toAssetId)
        assertEquals(2.5, created.serviceCharge, 0.001)

        val editor = templateEditor(templates, created.moduleBillId)
        awaitReady(editor)
        assertEquals("TRANSFER", editor.transactionType)
        assertEquals(2.5, editor.transferServiceCharge, 0.001)
        assertEquals(toId, editor.transferToAssetId)
        assertTrue(editor.templateLock)
        withContext(Dispatchers.Main) { editor.transferServiceCharge = 3.5 }
        saveTemplate(editor, name)

        val application = withContext(Dispatchers.Main) {
            AddEditViewModel(
                repo = transactions, categoryRepo = categories, assetRepo = assets, templateRepo = templates,
                txId = 0L, externalScope = scope, initialTemplateId = created.moduleBillId,
            )
        }
        awaitReady(application)
        assertEquals(3.5, application.transferServiceCharge, 0.001)
        val done = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) { application.saveTransfer { done.complete(Unit) } }
        withTimeout(10_000) { done.await() }
        assertEquals(466.5, requireNotNull(database.assetDao().findById(fromId)).assetNumber, 0.001)
        assertEquals(130.0, requireNotNull(database.assetDao().findById(toId)).assetNumber, 0.001)
        val transfer = database.transferDao().getAll().single { it.fromAssetId == fromId && it.toAssetId == toId }
        assertEquals(3.5, transfer.serviceCharge, 0.001)
    }

    private suspend fun templateEditor(templates: TemplateRepository, id: Long = 0L): AddEditViewModel =
        withContext(Dispatchers.Main) {
            AddEditViewModel(
                repo = transactions, categoryRepo = categories, assetRepo = assets, templateRepo = templates,
                txId = 0L, externalScope = scope, initialTemplateId = id, editingTemplate = true,
            )
        }

    private suspend fun awaitReady(vm: AddEditViewModel) {
        withTimeout(10_000) {
            while (vm.loading || vm.liveCategories.isEmpty() || vm.liveAssets.isEmpty()) delay(20)
        }
    }

    private suspend fun saveTemplate(vm: AddEditViewModel, name: String) {
        val done = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) { vm.saveCurrentAsTemplate(name) { done.complete(Unit) } }
        withTimeout(10_000) { done.await() }
    }

    private suspend fun addAccount(name: String, balance: Double): Long {
        val id = (database.assetDao().getAll().maxOfOrNull { it.assetId } ?: 0L) + 1L
        return database.assetDao().insert(AssetEntity(assetId = id, assetName = name, assetNumber = balance))
    }
}
