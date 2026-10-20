package com.nonen.Bookkeeping.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.BookEntity
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.db.CategoryRuleEntity
import com.nonen.Bookkeeping.data.db.InstalmentEntity
import com.nonen.Bookkeeping.data.db.LendEntity
import com.nonen.Bookkeeping.data.db.MerchantCategoryEntity
import com.nonen.Bookkeeping.data.db.RefundEntity
import com.nonen.Bookkeeping.data.db.ReimbursementEntity
import com.nonen.Bookkeeping.data.db.TagEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.TransactionTagCrossRef
import com.nonen.Bookkeeping.data.db.TransferEntity
import com.nonen.Bookkeeping.data.entity.BudgetEntity
import com.nonen.Bookkeeping.data.entity.CategoryBudgetEntity
import com.nonen.Bookkeeping.data.entity.CycleEntity
import com.nonen.Bookkeeping.data.entity.CycleLogEntity
import com.nonen.Bookkeeping.data.entity.DreamEntity
import com.nonen.Bookkeeping.data.entity.DreamLogEntity
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import com.nonen.Bookkeeping.data.prefs.ListenScope
import com.nonen.Bookkeeping.data.prefs.ThemeMode
import com.nonen.Bookkeeping.data.service.PeriodicTransactionExecutor
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class FullBackupServiceInstrumentationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var settingsStore: SettingsStore
    private lateinit var testWorkDir: File
    private lateinit var testAttachmentsDir: File
    private lateinit var service: FullBackupService
    private lateinit var settingsScope: CoroutineScope

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        check(context.packageName == "com.nonen.Bookkeeping.uivalidation")
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        testWorkDir = File(context.filesDir, "test_backup_sandbox")
        if (testWorkDir.exists()) testWorkDir.deleteRecursively()
        testWorkDir.mkdirs()
        settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settingsStore = SettingsStore(context, PreferenceDataStoreFactory.create(scope = settingsScope,
            produceFile = { File(testWorkDir, "settings.preferences_pb") }))

        testAttachmentsDir = File(testWorkDir, FullBackupService.ATTACHMENTS_DIR)
        testAttachmentsDir.mkdirs()

        service = FullBackupService(
            database = db,
            settingsStore = settingsStore,
            appContext = context,
            appVersionProvider = { "0.2.3-test" },
            filesDirProvider = { testWorkDir },
        )
    }

    @After
    fun tearDown() {
        db.close()
        settingsScope.cancel()
        if (testWorkDir.exists()) {
            testWorkDir.deleteRecursively()
        }
    }

    @Test
    fun testSuccessfulFullBackupAndRestore() = runBlocking {
        val now = System.currentTimeMillis()
        // 1. 在真实 Room 数据库中插入核心数据
        val book = BookEntity(bookId = 1L, bookName = "测试真实账本", isDefault = true, createdAt = now)
        db.bookDao().insert(book)

        val asset = AssetEntity(assetId = 1L, assetName = "招商银行", assetNumber = 5000.0, totalQuota = 0.0, assetType = 1, createdAt = now, updatedAt = now)
        db.assetDao().insert(asset)

        val category = CategoryEntity(categoryId = 101, parentId = 0, categoryName = "餐饮美食", iconUrl = "icon_food", positionWeight = 1, categoryType = 0, isCustom = false, createdAt = now)
        db.categoryDao().insert(category)

        val tx = TransactionEntity(
            id = 1001L,
            amount = -88.50,
            category = "餐饮美食",
            note = "真实晚餐记账",
            merchant = "海底捞",
            timestamp = now,
            source = "manual",
            assetId = 1L,
            bookId = 1L,
            parentCategoryId = 101,
            childCategoryId = null,
            transactionType = "EXPENSE",
            hash = "hash_1001",
            createdAt = now,
            updatedAt = now,
        )
        db.transactionDao().insert(tx)

        val budget = BudgetEntity(budgetId = 1L, bookId = 1L, budgetName = "10月总预算", year = 2026, month = 10, startTime = now, endTime = now + 86400000, num = 3000.0)
        db.budgetDao().insertBudgets(listOf(budget))

        val cycle = CycleEntity(cycleId = 1L, bookId = 1L, cost = 25.0, assetId = 1L, remark = "每月宽带扣费")
        db.cycleDao().insertCycle(cycle)

        val dream = DreamEntity(dreamId = 1L, dreamName = "买新电脑", iconUrl = "icon_pc", totalNum = 12000.0, startNum = 1000.0, addNum = 1000.0, startTime = now, endTime = now + 86400000L * 30)
        db.dreamDao().insertDreams(listOf(dream))

        db.assetDao().insert(asset.copy(assetId = 2L, assetName = "现金", assetNumber = 200.0))
        db.transferDao().insert(TransferEntity(
            transferId = 11L, fromAssetId = 1L, toAssetId = 2L, cost = 100.0,
            toCost = 99.0, serviceCharge = 1.0, billId = tx.id, time = now, remark = "转账手续费",
        ))
        db.refundDao().insert(RefundEntity(
            refundId = 12L, billId = tx.id, refundNum = 8.5, refundInfos = "[{\"amount\":8.5}]",
        ))
        db.reimbursementDao().insert(ReimbursementEntity(
            reimbursementId = 13L, billId = tx.id, assetId = 1L, reimbursementAssetId = 2L,
            reimbursementNum = 20.0, reimbursementNumbers = "[{\"amount\":20.0}]", end = true,
        ))
        db.lendDao().insert(LendEntity(
            lendId = 14L, type = 1, assetId = 1L, repaymentAssetId = 2L, number = 30.0,
            interest = 1.5, billId = tx.id, fromCost = 31.5, time = now, remark = "测试借款",
        ))
        db.instalmentDao().insert(InstalmentEntity(
            instalmentId = 15L, billId = tx.id, assetId = 1L, instalmentType = 2,
            totalNumber = 300.0, periods = 3, serviceNumber = 3.0, serviceType = 1,
            remainderType = 3, accountMonth = "2026-10",
        ))
        db.tagDao().insert(TagEntity(tagId = 16L, tagName = "聚餐", positionWeight = 2, createdAt = now))
        db.tagDao().insertRef(TransactionTagCrossRef(transactionId = tx.id, tagId = 16L))
        db.budgetDao().insertCategoryBudgets(listOf(CategoryBudgetEntity(
            categoryBudgetId = 17L, budgetId = budget.budgetId, bookId = book.bookId,
            year = 2026, month = 10, parentCategory = category.categoryId, num = 600.0, addNum = 30.0,
        )))
        db.cycleDao().insertCycleLogs(listOf(CycleLogEntity(
            logId = 18L, cycleId = cycle.cycleId, dedupKey = "backup-cycle-2026-10",
            transactionId = tx.id, transferId = 11L, executedTime = now, amount = 25.0,
        )))
        db.moduleBillDao().insert(ModuleBillEntity(
            moduleBillId = 19L, bookId = book.bookId, moduleType = ModuleBillEntity.MODULE_TYPE_TRANSFER,
            templateName = "测试转账模板", cost = 100.0, parentCategoryId = category.categoryId,
            assetId = 1L, fromAssetId = 1L, toAssetId = 2L, serviceCharge = 1.0,
            remark = "模板备注", tags = "16", moduleLock = true, notIntoBudget = true, notIntoTotal = true,
        ))
        db.dreamDao().insertDreamLogs(listOf(DreamLogEntity(
            logId = 20L, dreamId = dream.dreamId, amount = 1000.0, assetId = 2L,
            time = now, remark = "真实储蓄流水",
        )))
        db.categoryRuleDao().insert(CategoryRuleEntity(
            id = 21L, keyword = "海底捞", category = category.categoryName, isCustom = true, createdAt = now,
        ))
        db.merchantCategoryDao().insertAll(listOf(MerchantCategoryEntity(
            id = 22L, merchantKey = "海底捞", category = category.categoryName,
            isIncome = false, hits = 4, updatedAt = now,
        )))
        val originalTables = tableSnapshot()
        assertEquals(20, originalTables.size)
        originalTables.forEach { (table, rows) -> assertTrue("$table 应包含数据", rows.isNotEmpty()) }

        // 2. 写入真实附件文件
        val attachmentFile = File(testAttachmentsDir, "receipt_1001.txt")
        attachmentFile.writeText("小票明细：海底捞消费 88.50 元", StandardCharsets.UTF_8)

        // 3. 设定真实 SettingsStore 配置
        settingsStore.setThemeMode(ThemeMode.DARK)
        settingsStore.setAutoRecordEnabled(false)
        settingsStore.setListenScope(ListenScope.WECHAT_ONLY)
        settingsStore.setNotifyOnRecord(false)
        settingsStore.setLearnOnEdit(false)
        settingsStore.setFontOption("SYSTEM")
        settingsStore.setDynamicColor(false)
        settingsStore.setHideAmount(true)
        settingsStore.setAppLockEnabled(true)
        settingsStore.setNavigationLabelsEnabled(true)
        settingsStore.setSelectedBookId(1L)
        settingsStore.setNoteFirst(true)
        settingsStore.setSimpleCategory(true)
        settingsStore.setShowTransactionTime(true)
        settingsStore.setShowAssetIcon(true)
        val originalSettings = settingsStore.snapshot()

        // 4. 导出全量备份包
        val backupOut = ByteArrayOutputStream()
        val exportResult = service.exportBackup(backupOut)
        assertTrue(exportResult.success)
        assertTrue(backupOut.size() > 0)
        assertEquals(originalTables.values.sumOf { it.size }, exportResult.totalCount)
        val manifest = ZipInputStream(ByteArrayInputStream(backupOut.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null && entry.name != FullBackupService.ENTRY_MANIFEST) {
                entry = zip.nextEntry
            }
            check(entry != null) { "备份缺少 manifest.json" }
            JSONObject(zip.readBytes().toString(StandardCharsets.UTF_8))
        }
        val counts = manifest.getJSONObject("tableCounts")
        assertEquals(originalTables.keys, counts.keys().asSequence().toSet())
        originalTables.forEach { (table, rows) -> assertEquals(table, rows.size, counts.getInt(table)) }

        // 5. 清理当前数据库和附件，模拟新环境
        db.clearAllTables()
        tableSnapshot().forEach { (table, rows) -> assertTrue("$table 应已清空", rows.isEmpty()) }
        attachmentFile.delete()
        settingsStore.setThemeMode(ThemeMode.LIGHT)
        settingsStore.setAutoRecordEnabled(true)
        settingsStore.setListenScope(ListenScope.ALIPAY_ONLY)
        settingsStore.setNotifyOnRecord(true)
        settingsStore.setLearnOnEdit(true)
        settingsStore.setFontOption("DEFAULT")
        settingsStore.setDynamicColor(true)
        settingsStore.setHideAmount(false)
        settingsStore.setAppLockEnabled(false)
        settingsStore.setNavigationLabelsEnabled(false)
        settingsStore.setSelectedBookId(2L)
        settingsStore.setNoteFirst(false)
        settingsStore.setSimpleCategory(false)
        settingsStore.setShowTransactionTime(false)
        settingsStore.setShowAssetIcon(false)

        assertEquals(0, db.transactionDao().getAll().size)
        assertEquals(0, db.bookDao().getAll().size)
        assertFalse(attachmentFile.exists())

        // 6. 执行全量恢复
        val backupBytes = backupOut.toByteArray()
        val restoreResult = service.restoreBackup(ByteArrayInputStream(backupBytes))
        assertTrue(restoreResult.message, restoreResult.success)
        assertEquals(originalTables, tableSnapshot())

        // 7. 验证真实 Room 数据库中的数据已完整恢复
        val restoredBooks = db.bookDao().getAll()
        assertEquals(1, restoredBooks.size)
        assertEquals("测试真实账本", restoredBooks[0].bookName)

        val restoredAssets = db.assetDao().getAll()
        assertEquals(2, restoredAssets.size)
        assertEquals("招商银行", restoredAssets[0].assetName)
        assertEquals(5000.0, restoredAssets[0].assetNumber, 0.001)

        val restoredTxs = db.transactionDao().getAll()
        assertEquals(1, restoredTxs.size)
        assertEquals(-88.50, restoredTxs[0].amount, 0.001)
        assertEquals("真实晚餐记账", restoredTxs[0].note)
        assertEquals(1L, restoredTxs[0].assetId)

        val restoredBudgets = db.budgetDao().getAllBudgets()
        assertEquals(1, restoredBudgets.size)
        assertEquals(3000.0, restoredBudgets[0].num, 0.001)

        val restoredCycles = db.cycleDao().getAllCycles()
        assertEquals(1, restoredCycles.size)
        assertEquals(25.0, restoredCycles[0].cost, 0.001)

        val restoredDreams = db.dreamDao().getAllDreams()
        assertEquals(1, restoredDreams.size)
        assertEquals("买新电脑", restoredDreams[0].dreamName)

        // 8. 验证真实附件已还原
        assertTrue("附件未恢复", attachmentFile.exists())
        assertEquals("小票明细：海底捞消费 88.50 元", attachmentFile.readText(StandardCharsets.UTF_8))

        // 9. 验证设置已还原
        val snapshot = settingsStore.snapshot()
        assertEquals(ThemeMode.DARK, snapshot.themeMode)
        assertFalse(snapshot.dynamicColor)
        assertTrue(snapshot.hideAmount)
        assertTrue(snapshot.appLockEnabled)
        assertEquals(originalSettings, snapshot)
    }

    @Test
    fun testTamperedChecksumFailsAndLeavesDataIntact() = runBlocking {
        val now = System.currentTimeMillis()
        val book = BookEntity(bookId = 1L, bookName = "受保护的账本", isDefault = true, createdAt = now)
        db.bookDao().insert(book)

        // 构建一个摘要被篡改的 ZIP 包
        val fakeManifest = JSONObject().apply {
            put("formatVersion", FullBackupService.FORMAT_VERSION)
            put("appVersion", "0.2.3")
            put("dataChecksum", "0000000000000000000000000000000000000000000000000000000000000000")
        }.toString()

        val fakeData = JSONObject().apply {
            put("books", org.json.JSONArray().put(JSONObject().apply {
                put("bookId", 999L)
                put("bookName", "恶意覆盖的账本")
                put("isDefault", true)
                put("positionWeight", 1)
                put("createdAt", now)
            }))
        }.toString()

        val badZipOut = ByteArrayOutputStream()
        ZipOutputStream(badZipOut).use { zos ->
            zos.putNextEntry(ZipEntry(FullBackupService.ENTRY_MANIFEST))
            zos.write(fakeManifest.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(FullBackupService.ENTRY_DATA))
            zos.write(fakeData.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val result = service.restoreBackup(ByteArrayInputStream(badZipOut.toByteArray()))
        assertFalse(result.success)
        assertTrue("应包含校验失败提示", result.error?.contains("校验失败") == true)

        // 验证原数据库数据未受破坏
        val books = db.bookDao().getAll()
        assertEquals(1, books.size)
        assertEquals("受保护的账本", books[0].bookName)
    }

    @Test
    fun testZipSlipPathTraversalBlockedAndDataIntact() = runBlocking {
        val now = System.currentTimeMillis()
        val book = BookEntity(bookId = 1L, bookName = "路径安全账本", isDefault = true, createdAt = now)
        db.bookDao().insert(book)

        val evilZipOut = ByteArrayOutputStream()
        val emptyData = "{}"
        val checksum = java.security.MessageDigest.getInstance("SHA-256")
            .digest(emptyData.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        val manifest = JSONObject().apply {
            put("formatVersion", FullBackupService.FORMAT_VERSION)
            put("appVersion", "0.2.3")
            put("dataChecksum", checksum)
        }.toString()

        ZipOutputStream(evilZipOut).use { zos ->
            zos.putNextEntry(ZipEntry(FullBackupService.ENTRY_MANIFEST))
            zos.write(manifest.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(FullBackupService.ENTRY_DATA))
            zos.write(emptyData.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 注入 Zip Slip 恶意路径
            zos.putNextEntry(ZipEntry("attachments/../../evil_override.txt"))
            zos.write("malicious payload".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val result = service.restoreBackup(ByteArrayInputStream(evilZipOut.toByteArray()))
        assertFalse(result.success)
        assertTrue(result.error, result.error?.contains("安全越界") == true || result.error?.contains("非法附件路径") == true
            || result.error?.contains("Invalid zip entry path") == true)

        // 验证原数据未被清空
        assertEquals(1, db.bookDao().getAll().size)
    }

    @Test
    fun testPendingRecoveryJournalRepair() = runBlocking {
        // 验证 STAGED 状态的 journal 清理。
        val journalFile = File(testWorkDir, FullBackupService.RECOVERY_JOURNAL)
        val stagingDir = File(testWorkDir, FullBackupService.ATTACHMENTS_STAGING)
        stagingDir.mkdirs()
        File(stagingDir, "abandoned.txt").writeText("孤立文件", StandardCharsets.UTF_8)

        journalFile.writeText(
            JSONObject().apply {
                put("state", "STAGED")
                put("timestamp", System.currentTimeMillis())
            }.toString(),
            StandardCharsets.UTF_8,
        )

        // 触发自愈
        service.checkAndRepairPendingRecovery()
        assertFalse("STAGED 状态自愈后 journal 文件应被清除", journalFile.exists())
        assertFalse("STAGED 状态自愈后 staging 临时目录应被清除", stagingDir.exists())

        // 验证 DB_COMMITTED 状态的附件与设置补全。
        stagingDir.mkdirs()
        File(stagingDir, "recovered_attachment.txt").writeText("成功补全的附件", StandardCharsets.UTF_8)

        val settingsJson = JSONObject().apply {
            put("themeMode", "DARK")
            put("hideAmount", "true")
        }
        journalFile.writeText(
            JSONObject().apply {
                put("state", "DB_COMMITTED")
                put("timestamp", System.currentTimeMillis())
                put("settings", settingsJson)
            }.toString(),
            StandardCharsets.UTF_8,
        )

        // 触发自愈
        service.checkAndRepairPendingRecovery()
        assertFalse("DB_COMMITTED 状态自愈后 journal 文件应被清除", journalFile.exists())

        val finalAttachmentsDir = File(testWorkDir, FullBackupService.ATTACHMENTS_DIR)
        val recoveredFile = File(finalAttachmentsDir, "recovered_attachment.txt")
        assertTrue("DB_COMMITTED 状态下附件应被原子移动到位", recoveredFile.exists())
        assertEquals("成功补全的附件", recoveredFile.readText(StandardCharsets.UTF_8))

        val s = settingsStore.snapshot()
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertTrue(s.hideAmount)
    }

    private suspend fun tableSnapshot(): Map<String, List<*>> = linkedMapOf(
        "transactions" to db.transactionDao().getAll(),
        "assets" to db.assetDao().getAll(),
        "books" to db.bookDao().getAll(),
        "categories" to db.categoryDao().getAll(),
        "transfers" to db.transferDao().getAll(),
        "refunds" to db.refundDao().getAll(),
        "reimbursements" to db.reimbursementDao().getAll(),
        "lends" to db.lendDao().getAll(),
        "instalments" to db.instalmentDao().getAll(),
        "tags" to db.tagDao().getAll(),
        "tagRefs" to db.tagDao().getAllRefs(),
        "budgets" to db.budgetDao().getAllBudgets(),
        "categoryBudgets" to db.budgetDao().getAllCategoryBudgets(),
        "cycles" to db.cycleDao().getAllCycles(),
        "cycleLogs" to db.cycleDao().getAllCycleLogs(),
        "moduleBills" to db.moduleBillDao().getAll(),
        "dreams" to db.dreamDao().getAllDreams(),
        "dreamLogs" to db.dreamDao().getAllDreamLogs(),
        "categoryRules" to db.categoryRuleDao().getAll(),
        "merchantCategories" to db.merchantCategoryDao().getAll(),
    )

    @Test
    fun testPeriodicTransactionRealRoomIdempotency() = runBlocking {
        val now = System.currentTimeMillis()
        val cycle = CycleEntity(
            cycleId = 1L,
            bookId = 1L,
            cycleType = CycleEntity.CYCLE_TYPE_BILL,
            cost = 15.0,
            dateMode = CycleEntity.MODE_EVERY_DAY,
            assetId = 1L,
            remark = "每日早餐扣款",
            addTime = now - 86400000L * 2, // 2天前建立
            addTimes = 0,
            cycleTimes = 1,
            pause = false,
            end = false,
        )
        db.cycleDao().insertCycle(cycle)

        val executor = PeriodicTransactionExecutor(
            database = db,
            cycleDao = db.cycleDao(),
            transactionDao = db.transactionDao(),
            transferDao = db.transferDao(),
            assetDao = db.assetDao(),
        )

        // 首次执行：应扫描并入账
        val firstRunCount = executor.executePendingCycles(now)
        assertEquals(1, firstRunCount)
        assertEquals(1, db.transactionDao().getAll().size)
        assertEquals(1, db.cycleDao().getAllCycleLogs().size)

        // 第二次执行：幂等防重机制拦截，不重复记账
        val secondRunCount = executor.executePendingCycles(now)
        assertEquals(0, secondRunCount)
        assertEquals(1, db.transactionDao().getAll().size)
        assertEquals(1, db.cycleDao().getAllCycleLogs().size)
    }

    @Test
    fun testPeriodicTransferRealRoomBalanceUpdateAndLimits() = runBlocking {
        val now = System.currentTimeMillis()
        val asset1 = AssetEntity(assetId = 10L, assetName = "招行", assetNumber = 2000.0, totalQuota = 0.0, assetType = 1, createdAt = now, updatedAt = now)
        val asset2 = AssetEntity(assetId = 20L, assetName = "微信", assetNumber = 500.0, totalQuota = 0.0, assetType = 1, createdAt = now, updatedAt = now)
        db.assetDao().insert(asset1)
        db.assetDao().insert(asset2)

        val cycle = CycleEntity(
            cycleId = 202L,
            bookId = 1L,
            cost = 200.0,
            cycleType = CycleEntity.CYCLE_TYPE_TRANSFER,
            dateMode = CycleEntity.MODE_EVERY_DAY,
            fromAssetId = 10L,
            toAssetId = 20L,
            remark = "自动转账零钱",
            addTime = now - 86400000L * 3,
            addTimes = 0,
            cycleTimes = 2,
            pause = false,
            end = false,
        )
        db.cycleDao().insertCycle(cycle)

        val executor = PeriodicTransactionExecutor(
            database = db,
            cycleDao = db.cycleDao(),
            transactionDao = db.transactionDao(),
            transferDao = db.transferDao(),
            assetDao = db.assetDao(),
        )

        val count = executor.executePendingCycles(now)
        assertEquals(2, count)

        val updatedAsset1 = db.assetDao().findById(10L)
        val updatedAsset2 = db.assetDao().findById(20L)
        assertNotNull(updatedAsset1)
        assertNotNull(updatedAsset2)
        assertEquals(1600.0, updatedAsset1!!.assetNumber, 0.001)
        assertEquals(900.0, updatedAsset2!!.assetNumber, 0.001)

        val updatedCycle = db.cycleDao().getCycleById(202L)
        assertNotNull(updatedCycle)
        assertTrue(updatedCycle!!.end)
        assertEquals(2, updatedCycle.addTimes)

        val countRepeat = executor.executePendingCycles(now)
        assertEquals(0, countRepeat)
    }
}
