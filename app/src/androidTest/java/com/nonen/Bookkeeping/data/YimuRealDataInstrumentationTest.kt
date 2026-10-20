package com.nonen.Bookkeeping.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import com.nonen.Bookkeeping.data.prefs.ThemeMode
import com.nonen.Bookkeeping.export.FullBackupService
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.TemplateRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.stats.StatsCalculator
import com.nonen.Bookkeeping.ui.screens.AddEditViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class YimuRealDataInstrumentationTest {
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
        val actualHash = MessageDigest.getInstance("SHA-256").digest(source.readBytes()).joinToString("") { "%02x".format(it) }
        check(actualHash == expectedHash)
        databaseName = "yimu-real-${UUID.randomUUID()}.db"
        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).createFromFile(source).build()
        transactions = TransactionRepository(database.transactionDao(), database.categoryRuleDao(), database.merchantCategoryDao(),
            assetDao = database.assetDao(), transferDao = database.transferDao(), database = database)
        categories = CategoryRepository(database.categoryDao())
        assets = AssetRepository(database.assetDao(), database.transferDao(), database.transactionDao(), database)
    }

    @After
    fun tearDown() {
        scope.cancel()
        if (::database.isInitialized) database.close()
        if (::databaseName.isInitialized) check(context.deleteDatabase(databaseName))
    }

    private suspend fun account(name: String, balance: Double): Long =
        database.assetDao().insert(AssetEntity(assetId = (database.assetDao().getAll().maxOfOrNull { it.assetId } ?: 0L) + 1,
            assetName = name, assetNumber = balance))

    private suspend fun balance(id: Long) = requireNotNull(database.assetDao().findById(id)).assetNumber

    @Test
    fun transactionInsertEditMoveDeleteAndDuplicatePreserveBalances() = runBlocking {
        assertEquals(1970, transactions.getAll().size)
        val from = account("S09现金", 1000.0)
        val to = account("S09零钱", 500.0)
        val expense = TransactionEntity(amount = -100.0, category = "食品餐饮", assetId = from,
            source = "validation", timestamp = System.currentTimeMillis(), hash = UUID.randomUUID().toString())
        assertTrue(transactions.insertIfNew(expense))
        assertFalse(transactions.insertIfNew(expense))
        assertEquals(900.0, balance(from), 0.001)
        assertTrue(transactions.insertIfNew(expense.copy(amount = 300.0, assetId = to, transactionType = "INCOME",
            hash = UUID.randomUUID().toString())))
        val saved = transactions.getAll().single { it.hash == expense.hash }
        transactions.update(saved.copy(amount = -150.0))
        assertEquals(850.0, balance(from), 0.001)
        transactions.update(saved.copy(amount = -150.0, assetId = to))
        assertEquals(1000.0, balance(from), 0.001)
        assertEquals(650.0, balance(to), 0.001)
        transactions.delete(requireNotNull(transactions.getById(saved.id)))
        assertEquals(800.0, balance(to), 0.001)
    }

    @Test
    fun transfersAndHistoricalAssetTrendUseRealTransferRows() = runBlocking {
        val from = account("S09储蓄", 500.0)
        val to = account("S09零钱", 100.0)
        val zone = ZoneId.systemDefault()
        val latest = (transactions.getAll().map { it.timestamp } + database.transferDao().getAll().map { it.time })
            .maxOrNull() ?: 0L
        val date = Instant.ofEpochMilli(latest).atZone(zone).toLocalDate().plusDays(2)
        val before = assets.calculateSummary(assets.getAll()).netAsset
        val transfer = transactions.createTransfer(from, to, 200.0, serviceCharge = 2.0,
            time = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli())
        assertEquals(298.0, balance(from), 0.001)
        assertEquals(300.0, balance(to), 0.001)
        val trend = StatsCalculator.computeAssetTrend(date.minusDays(1), date, assets.getAll(), transactions.getAll(),
            zone, database.transferDao().getAll())
        assertEquals(before, trend.first().netAsset, 0.001)
        assertEquals(before - 2.0, trend.last().netAsset, 0.001)
        assertTrue(transactions.deleteTransfer(transfer))
        assertEquals(500.0, balance(from), 0.001)
        assertEquals(100.0, balance(to), 0.001)
        assertFalse(transactions.deleteTransfer(transfer))
    }

    @Test
    fun templateIncomeRetainsClassificationAccountAndTags() = runBlocking {
        val account = account("S09模板账户", 100.0)
        val parent = categories.getParentCategories(1).first()
        val child = categories.getChildCategories(parent.categoryId).first()
        val templates = TemplateRepository(database.moduleBillDao(), transactions, categories, assets)
        val id = templates.createTemplate(ModuleBillEntity(templateName = "S09收入模板", cost = 300.0,
            parentCategoryId = parent.categoryId, childCategoryId = child.categoryId, assetId = account,
            tags = "S09标签", remark = "S09备注", moduleLock = true))
        val time = System.currentTimeMillis()
        assertNotNull(templates.applyTemplate(id, now = time))
        assertNull(templates.applyTemplate(id, now = time))
        val saved = transactions.getAll().single { it.hash == "tpl_${id}_$time" }
        assertEquals(300.0, saved.amount, 0.001)
        assertEquals(parent.categoryName, saved.category)
        assertEquals(child.categoryName, saved.subCategory)
        assertEquals("S09模板账户", saved.account)
        assertEquals("S09标签", saved.tag)
        assertEquals(400.0, balance(account), 0.001)
        templates.deleteTemplate(id)
        assertNull(templates.getById(id))
    }

    @Test
    fun customCategoryCreationDeletionPreservesTransactionText() = runBlocking {
        val child = categories.createCustomCategory("S09下午茶", 7, 0, "ic_restaurant_snacks")
        assertTrue(child.isCustom)
        assertEquals(child, categories.getById(child.id))
        val account = account("S09分类账户", 50.0)
        val record = TransactionEntity(amount = -15.5, category = "食品餐饮", subCategory = child.categoryName,
            parentCategoryId = 7, childCategoryId = child.categoryId, assetId = account,
            source = "validation", timestamp = System.currentTimeMillis(), hash = UUID.randomUUID().toString())
        assertTrue(transactions.insertIfNew(record))
        val saved = transactions.getAll().single { it.hash == record.hash }
        categories.deleteCustomCategory(child.id, child.categoryId, unlinkTransactions = true)
        val retained = requireNotNull(transactions.getById(saved.id))
        assertEquals(record.subCategory, retained.subCategory)
        assertEquals(record.amount, retained.amount, 0.001)
        assertNull(retained.childCategoryId)
        assertNull(categories.getById(child.id))
    }

    @Test
    fun addEditViewModelPersistsActualCategoryIds() = runBlocking {
        val account = account("S09表单账户", 100.0)
        val vm = AddEditViewModel(transactions, categoryRepo = categories, assetRepo = assets, txId = 0,
            externalScope = scope)
        withTimeout(10000) { while (vm.liveCategories.isEmpty()) kotlinx.coroutines.delay(20) }
        vm.selectAsset(requireNotNull(assets.getById(account)))
        vm.selectCategoryManually("食品餐饮", 7)
        vm.selectSubCategory("早餐", 708)
        vm.setAmountInput("15.5")
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()
        vm.save { done.complete(Unit) }
        withTimeout(10000) { done.await() }
        val record = transactions.getAll().single { it.assetId == account }
        assertEquals(7, record.parentCategoryId)
        assertEquals(708, record.childCategoryId)
        assertEquals(84.5, balance(account), 0.001)
    }

    @Test
    fun fullBackupRestoresOriginalRecordsAndAllDisplaySettings() = runBlocking {
        val directory = File(context.filesDir, databaseName.removeSuffix(".db"))
        check(directory.mkdir())
        val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(context, PreferenceDataStoreFactory.create(scope = settingsScope,
            produceFile = { File(directory, "settings.preferences_pb") }))
        settings.setThemeMode(ThemeMode.DARK)
        settings.setNavigationLabelsEnabled(true)
        settings.setNoteFirst(true)
        settings.setSimpleCategory(true)
        settings.setShowTransactionTime(true)
        settings.setShowAssetIcon(true)
        settings.setSelectedBookId(1L)
        val snapshot = settings.snapshot()
        val sourceTransactions = transactions.getAll()
        val sourceCategories = categories.getAll()
        val sourceAssets = assets.getAll()
        val service = FullBackupService(database, settings, context, filesDirProvider = { directory })
        try {
            val bytes = ByteArrayOutputStream()
            val exported = service.exportBackup(bytes)
            assertTrue(exported.message, exported.success)
            database.clearAllTables()
            settings.setThemeMode(ThemeMode.LIGHT)
            settings.setNavigationLabelsEnabled(false)
            val restored = service.restoreBackup(ByteArrayInputStream(bytes.toByteArray()))
            assertTrue(restored.message, restored.success)
            assertEquals(sourceTransactions, transactions.getAll())
            assertEquals(sourceCategories, categories.getAll())
            assertEquals(sourceAssets, assets.getAll())
            assertEquals(snapshot, settings.snapshot())
        } finally {
            settingsScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            settingsScope.coroutineContext[kotlinx.coroutines.Job]?.join()
            check(directory.deleteRecursively())
        }
    }
}
