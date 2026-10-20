package com.nonen.Bookkeeping.export

import android.content.Context
import androidx.room.withTransaction
import com.nonen.Bookkeeping.core.JsonUtil
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 本地全量数据完整备份与原子事务恢复服务。
 * 格式采用带格式版本与 SHA-256 摘要校验的 ZIP 压缩归档格式。
 */
class FullBackupService(
    private val database: AppDatabase,
    private val settingsStore: SettingsStore? = null,
    private val appContext: Context? = null,
    private val appVersionProvider: (() -> String)? = null,
    private val filesDirProvider: (() -> File)? = null,
    private val interruptionHook: ((stage: String) -> Unit)? = null,
) {

    suspend fun exportBackup(outputStream: OutputStream): FullBackupResult = withContext(Dispatchers.IO) {
        try {
            val transactions = database.transactionDao().getAll()
            val assets = database.assetDao().getAll()
            val books = database.bookDao().getAll()
            val categories = database.categoryDao().getAll()
            val transfers = database.transferDao().getAll()
            val refunds = database.refundDao().getAll()
            val reimbursements = database.reimbursementDao().getAll()
            val lends = database.lendDao().getAll()
            val instalments = database.instalmentDao().getAll()
            val tags = database.tagDao().getAll()
            val tagRefs = database.tagDao().getAllRefs()
            val budgets = database.budgetDao().getAllBudgets()
            val categoryBudgets = database.budgetDao().getAllCategoryBudgets()
            val cycles = database.cycleDao().getAllCycles()
            val cycleLogs = database.cycleDao().getAllCycleLogs()
            val moduleBills = database.moduleBillDao().getAll()
            val dreams = database.dreamDao().getAllDreams()
            val dreamLogs = database.dreamDao().getAllDreamLogs()
            val categoryRules = database.categoryRuleDao().getAll()
            val merchantCategories = database.merchantCategoryDao().getAll()

            val settingsMap = if (settingsStore != null) {
                val s = settingsStore.snapshot()
                mapOf(
                    "autoRecordEnabled" to s.autoRecordEnabled.toString(),
                    "listenScope" to s.listenScope.name,
                    "notifyOnRecord" to s.notifyOnRecord.toString(),
                    "learnOnEdit" to s.learnOnEdit.toString(),
                    "themeMode" to s.themeMode.name,
                    "dynamicColor" to s.dynamicColor.toString(),
                    "hideAmount" to s.hideAmount.toString(),
                    "appLockEnabled" to s.appLockEnabled.toString(),
                    "fontOption" to s.fontOption,
                    "navigationLabelsEnabled" to s.navigationLabelsEnabled.toString(),
                    "selectedBookId" to s.selectedBookId.toString(),
                    "noteFirst" to s.noteFirst.toString(),
                    "simpleCategory" to s.simpleCategory.toString(),
                    "showTransactionTime" to s.showTransactionTime.toString(),
                    "showAssetIcon" to s.showAssetIcon.toString(),
                    "templateTipHidden" to s.templateTipHidden.toString(),
                )
            } else emptyMap()

            val tableCounts = mapOf(
                "transactions" to transactions.size,
                "assets" to assets.size,
                "books" to books.size,
                "categories" to categories.size,
                "transfers" to transfers.size,
                "refunds" to refunds.size,
                "reimbursements" to reimbursements.size,
                "lends" to lends.size,
                "instalments" to instalments.size,
                "tags" to tags.size,
                "tagRefs" to tagRefs.size,
                "budgets" to budgets.size,
                "categoryBudgets" to categoryBudgets.size,
                "cycles" to cycles.size,
                "cycleLogs" to cycleLogs.size,
                "moduleBills" to moduleBills.size,
                "dreams" to dreams.size,
                "dreamLogs" to dreamLogs.size,
                "categoryRules" to categoryRules.size,
                "merchantCategories" to merchantCategories.size,
            )

            val dataJson = serializeData(
                transactions, assets, books, categories, transfers, refunds, reimbursements,
                lends, instalments, tags, tagRefs, budgets, categoryBudgets, cycles, cycleLogs,
                moduleBills, dreams, dreamLogs, categoryRules, merchantCategories, settingsMap,
            )
            val dataBytes = dataJson.toByteArray(StandardCharsets.UTF_8)
            val checksum = calculateSha256(dataBytes)

            val appVersion = appVersionProvider?.invoke() ?: runCatching {
                appContext?.packageManager?.getPackageInfo(appContext.packageName, 0)?.versionName
            }.getOrNull() ?: "0.2.3"

            val manifest = JSONObject().apply {
                put("formatVersion", FORMAT_VERSION)
                put("appVersion", appVersion)
                put("createdAt", System.currentTimeMillis())
                put("dataChecksum", checksum)
                val countsJson = JSONObject()
                tableCounts.forEach { (k, v) -> countsJson.put(k, v) }
                put("tableCounts", countsJson)
            }.toString()

            val zipOut = ZipOutputStream(outputStream)

            // 1. 写入 manifest.json
            zipOut.putNextEntry(ZipEntry(ENTRY_MANIFEST))
            zipOut.write(manifest.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()

            // 2. 写入 data.json
            zipOut.putNextEntry(ZipEntry(ENTRY_DATA))
            zipOut.write(dataBytes)
            zipOut.closeEntry()

            // 3. 写入 attachments 目录（若存在）
            val filesDir = filesDirProvider?.invoke() ?: appContext?.filesDir
            val attachmentsDir = filesDir?.let { File(it, ATTACHMENTS_DIR) }
            if (attachmentsDir != null && attachmentsDir.exists() && attachmentsDir.isDirectory) {
                attachmentsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relativePath = file.relativeTo(attachmentsDir).path.replace("\\", "/")
                    zipOut.putNextEntry(ZipEntry("$ATTACHMENTS_DIR/$relativePath"))
                    file.inputStream().use { input -> input.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }

            zipOut.finish()
            val totalRecords = tableCounts.values.sum()
            FullBackupResult(success = true, totalCount = totalRecords, message = "全量数据备份成功，共导出 $totalRecords 条记录")
        } catch (e: Exception) {
            FullBackupResult(success = false, error = e.message ?: "未知备份错误")
        }
    }

    suspend fun restoreBackup(inputStream: InputStream): FullBackupResult = withContext(Dispatchers.IO) {
        val baseDir = filesDirProvider?.invoke() ?: appContext?.filesDir
        if (baseDir != null && !baseDir.exists()) {
            baseDir.mkdirs()
        }
        val stagingDir = if (baseDir != null) File(baseDir, ATTACHMENTS_STAGING) else null
        val journalFile = if (baseDir != null) File(baseDir, RECOVERY_JOURNAL) else null
        val attachmentsDir = if (baseDir != null) File(baseDir, ATTACHMENTS_DIR) else null
        val backupDir = if (baseDir != null) File(baseDir, ATTACHMENTS_BACKUP) else null

        try {
            var manifestBytes: ByteArray? = null
            var dataBytes: ByteArray? = null
            val attachmentFiles = mutableMapOf<String, ByteArray>()

            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == ENTRY_MANIFEST -> manifestBytes = zipIn.readBytes()
                        name == ENTRY_DATA -> dataBytes = zipIn.readBytes()
                        name.startsWith("$ATTACHMENTS_DIR/") && !entry.isDirectory -> {
                            val subPath = name.removePrefix("$ATTACHMENTS_DIR/")
                            // 严厉防范 Zip Slip 路径越界
                            if (subPath.contains("..") || subPath.startsWith("/") || subPath.startsWith("\\")) {
                                throw IllegalArgumentException("发现非法附件路径，存在安全越界风险: $subPath")
                            }
                            attachmentFiles[subPath] = zipIn.readBytes()
                        }
                    }
                    entry = zipIn.nextEntry
                }
            }

            if (manifestBytes == null) {
                return@withContext FullBackupResult(success = false, error = "无效的备份文件：缺少清单描述文件")
            }
            if (dataBytes == null) {
                return@withContext FullBackupResult(success = false, error = "无效的备份文件：缺少核心数据内容")
            }

            val manifestObj = JSONObject(manifestBytes!!.toString(StandardCharsets.UTF_8))
            if (manifestObj.optInt("formatVersion", -1) != FORMAT_VERSION) {
                return@withContext FullBackupResult(success = false, error = "不支持的备份格式版本")
            }
            val expectedChecksum = manifestObj.optString("dataChecksum")
            val actualChecksum = calculateSha256(dataBytes!!)
            if (expectedChecksum != actualChecksum) {
                return@withContext FullBackupResult(success = false, error = "备份校验失败：数据文件校验码不匹配")
            }

            val dataObj = JSONObject(dataBytes!!.toString(StandardCharsets.UTF_8))
            val data = deserializeData(dataObj)

            // 1. 附件写入 staging 阶段目录
            if (attachmentFiles.isNotEmpty() && stagingDir != null) {
                if (stagingDir.exists()) stagingDir.deleteRecursively()
                stagingDir.mkdirs()
                attachmentFiles.forEach { (subPath, bytes) ->
                    val targetFile = File(stagingDir, subPath)
                    // 二次校验 canonicalPath 必须位于 stagingDir 内部
                    if (!targetFile.canonicalPath.startsWith(stagingDir.canonicalPath)) {
                        throw IllegalArgumentException("附件解压目标路径越界: $subPath")
                    }
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeBytes(bytes)
                }
            }

            // 2. 写入 recovery_journal 记录阶段 STAGED
            if (journalFile != null) {
                val settingsJson = JSONObject()
                data.settings.forEach { (k, v) -> settingsJson.put(k, v) }
                val journalContent = JSONObject().apply {
                    put("state", "STAGED")
                    put("timestamp", System.currentTimeMillis())
                    put("settings", settingsJson)
                }.toString()
                journalFile.writeText(journalContent, StandardCharsets.UTF_8)
            }

            // 测试中断 Hook：模拟在数据库事务提交前被杀
            interruptionHook?.invoke("STAGED")

            // 3. 原子真实 Room 事务执行恢复
            try {
                database.withTransaction {
                    // 按依赖反序清空既有数据
                    database.transactionDao().clearAll()
                    database.tagDao().clearAllRefs()
                    database.tagDao().clearAll()
                    database.transferDao().clearAll()
                    database.refundDao().clearAll()
                    database.reimbursementDao().clearAll()
                    database.lendDao().clearAll()
                    database.instalmentDao().clearAll()
                    database.budgetDao().clearAllCategoryBudgets()
                    database.budgetDao().clearAllBudgets()
                    database.cycleDao().clearAllCycleLogs()
                    database.cycleDao().clearAllCycles()
                    database.moduleBillDao().clearAll()
                    database.dreamDao().clearAllDreamLogs()
                    database.dreamDao().clearAllDreams()
                    database.categoryDao().clearAll()
                    database.bookDao().clearAll()
                    database.assetDao().clearAll()
                    database.categoryRuleDao().clearAll()
                    database.merchantCategoryDao().clearAll()

                    // 按拓扑依赖正序重灌数据
                    if (data.books.isNotEmpty()) database.bookDao().insertAll(data.books)
                    if (data.assets.isNotEmpty()) database.assetDao().insertAll(data.assets)
                    if (data.categories.isNotEmpty()) database.categoryDao().insertAll(data.categories)
                    if (data.tags.isNotEmpty()) database.tagDao().insertAll(data.tags)

                    if (data.transactions.isNotEmpty()) database.transactionDao().insertAll(data.transactions)
                    if (data.transactionTags.isNotEmpty()) database.tagDao().insertAllRefs(data.transactionTags)
                    if (data.transfers.isNotEmpty()) database.transferDao().insertAll(data.transfers)
                    if (data.refunds.isNotEmpty()) database.refundDao().insertAll(data.refunds)
                    if (data.reimbursements.isNotEmpty()) database.reimbursementDao().insertAll(data.reimbursements)
                    if (data.lends.isNotEmpty()) database.lendDao().insertAll(data.lends)
                    if (data.instalments.isNotEmpty()) database.instalmentDao().insertAll(data.instalments)

                    if (data.budgets.isNotEmpty()) database.budgetDao().insertBudgets(data.budgets)
                    if (data.categoryBudgets.isNotEmpty()) database.budgetDao().insertCategoryBudgets(data.categoryBudgets)
                    if (data.cycles.isNotEmpty()) database.cycleDao().insertCycles(data.cycles)
                    if (data.cycleLogs.isNotEmpty()) database.cycleDao().insertCycleLogs(data.cycleLogs)
                    if (data.moduleBills.isNotEmpty()) database.moduleBillDao().insertAll(data.moduleBills)
                    if (data.dreams.isNotEmpty()) database.dreamDao().insertDreams(data.dreams)
                    if (data.dreamLogs.isNotEmpty()) database.dreamDao().insertDreamLogs(data.dreamLogs)

                    if (data.categoryRules.isNotEmpty()) database.categoryRuleDao().insertAll(data.categoryRules)
                    if (data.merchantCategories.isNotEmpty()) database.merchantCategoryDao().insertAll(data.merchantCategories)
                }
            } catch (txError: Exception) {
                // 数据库事务异常回滚，清理 staging 与 journal，保持原状态
                stagingDir?.deleteRecursively()
                journalFile?.delete()
                throw txError
            }

            // 4. 数据库提交成功，更新 journal 状态为 DB_COMMITTED
            if (journalFile != null) {
                val settingsJson = JSONObject()
                data.settings.forEach { (k, v) -> settingsJson.put(k, v) }
                val journalContent = JSONObject().apply {
                    put("state", "DB_COMMITTED")
                    put("timestamp", System.currentTimeMillis())
                    put("settings", settingsJson)
                }.toString()
                journalFile.writeText(journalContent, StandardCharsets.UTF_8)
            }

            // 测试中断 Hook：模拟在数据库事务提交后、附件原子替换前被杀
            interruptionHook?.invoke("DB_COMMITTED")

            // 5. 原子替换附件目录
            if (stagingDir != null && attachmentsDir != null && stagingDir.exists()) {
                if (backupDir != null && backupDir.exists()) backupDir.deleteRecursively()
                if (attachmentsDir.exists() && backupDir != null) {
                    attachmentsDir.renameTo(backupDir)
                } else if (attachmentsDir.exists()) {
                    attachmentsDir.deleteRecursively()
                }
                val moved = stagingDir.renameTo(attachmentsDir)
                if (!moved) {
                    attachmentsDir.mkdirs()
                    stagingDir.copyRecursively(attachmentsDir, overwrite = true)
                    stagingDir.deleteRecursively()
                }
                backupDir?.deleteRecursively()
            }

            // 6. 完整恢复偏好配置
            if (settingsStore != null && data.settings.isNotEmpty()) {
                restoreSettingsFromMap(data.settings)
            }

            // 7. 清理 recovery journal
            journalFile?.delete()

            val totalRestored = data.transactions.size + data.assets.size + data.categories.size
            FullBackupResult(success = true, totalCount = totalRestored, message = "全量数据恢复成功，恢复了 ${data.transactions.size} 笔账单流水")
        } catch (e: Exception) {
            FullBackupResult(success = false, error = "恢复失败，数据已完整回滚：${e.message}")
        }
    }

    private suspend fun restoreSettingsFromMap(map: Map<String, String>) {
        if (settingsStore == null) return
        map["autoRecordEnabled"]?.toBooleanStrictOrNull()?.let { settingsStore.setAutoRecordEnabled(it) }
        map["listenScope"]?.let { settingsStore.setListenScope(com.nonen.Bookkeeping.data.prefs.ListenScope.from(it)) }
        map["notifyOnRecord"]?.toBooleanStrictOrNull()?.let { settingsStore.setNotifyOnRecord(it) }
        map["learnOnEdit"]?.toBooleanStrictOrNull()?.let { settingsStore.setLearnOnEdit(it) }
        map["themeMode"]?.let { settingsStore.setThemeMode(com.nonen.Bookkeeping.data.prefs.ThemeMode.from(it)) }
        map["dynamicColor"]?.toBooleanStrictOrNull()?.let { settingsStore.setDynamicColor(it) }
        map["hideAmount"]?.toBooleanStrictOrNull()?.let { settingsStore.setHideAmount(it) }
        map["appLockEnabled"]?.toBooleanStrictOrNull()?.let { settingsStore.setAppLockEnabled(it) }
        map["fontOption"]?.let { settingsStore.setFontOption(it) }
        settingsStore.setNavigationLabelsEnabled(map["navigationLabelsEnabled"]?.toBooleanStrict() ?: false)
        settingsStore.setSelectedBookId(map["selectedBookId"]?.toLong() ?: 1L)
        settingsStore.setNoteFirst(map["noteFirst"]?.toBooleanStrict() ?: false)
        settingsStore.setSimpleCategory(map["simpleCategory"]?.toBooleanStrict() ?: false)
        settingsStore.setShowTransactionTime(map["showTransactionTime"]?.toBooleanStrict() ?: false)
        settingsStore.setShowAssetIcon(map["showAssetIcon"]?.toBooleanStrict() ?: false)
        settingsStore.setTemplateTipHidden(map["templateTipHidden"]?.toBooleanStrict() ?: false)
    }

    suspend fun checkAndRepairPendingRecovery() = withContext(Dispatchers.IO) {
        val baseDir = filesDirProvider?.invoke() ?: appContext?.filesDir ?: return@withContext
        val journalFile = File(baseDir, RECOVERY_JOURNAL)
        if (!journalFile.exists()) return@withContext

        try {
            val journal = JSONObject(journalFile.readText(StandardCharsets.UTF_8))
            val state = journal.optString("state")
            val stagingDir = File(baseDir, ATTACHMENTS_STAGING)
            val attachmentsDir = File(baseDir, ATTACHMENTS_DIR)
            val backupDir = File(baseDir, ATTACHMENTS_BACKUP)

            when (state) {
                "STAGED" -> {
                    if (stagingDir.exists()) stagingDir.deleteRecursively()
                    if (backupDir.exists()) backupDir.deleteRecursively()
                    journalFile.delete()
                }
                "DB_COMMITTED" -> {
                    if (stagingDir.exists()) {
                        if (attachmentsDir.exists()) attachmentsDir.deleteRecursively()
                        val renamed = stagingDir.renameTo(attachmentsDir)
                        if (!renamed) {
                            attachmentsDir.mkdirs()
                            stagingDir.copyRecursively(attachmentsDir, overwrite = true)
                            stagingDir.deleteRecursively()
                        }
                    }
                    if (backupDir.exists()) backupDir.deleteRecursively()

                    val settingsObj = journal.optJSONObject("settings")
                    if (settingsObj != null && settingsStore != null) {
                        val map = mutableMapOf<String, String>()
                        val keys = settingsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            map[key] = settingsObj.getString(key)
                        }
                        restoreSettingsFromMap(map)
                    }
                    journalFile.delete()
                }
                else -> {
                    journalFile.delete()
                }
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val ENTRY_MANIFEST = "manifest.json"
        const val ENTRY_DATA = "data.json"
        const val ATTACHMENTS_DIR = "attachments"
        const val ATTACHMENTS_STAGING = "attachments_staging"
        const val RECOVERY_JOURNAL = "recovery_journal.json"
        const val ATTACHMENTS_BACKUP = "attachments_backup"

        const val JOURNAL_STAGE_STAGED = "STAGED"
        const val JOURNAL_STAGE_DB_COMMITTED = "DB_COMMITTED"
        const val JOURNAL_STAGE_ATTACHMENTS_MOVED = "ATTACHMENTS_MOVED"

        internal fun calculateSha256(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            return hash.joinToString("") { "%02x".format(it) }
        }

        internal fun serializeData(
        transactions: List<TransactionEntity>,
        assets: List<AssetEntity>,
        books: List<BookEntity>,
        categories: List<CategoryEntity>,
        transfers: List<TransferEntity>,
        refunds: List<RefundEntity>,
        reimbursements: List<ReimbursementEntity>,
        lends: List<LendEntity>,
        instalments: List<InstalmentEntity>,
        tags: List<TagEntity>,
        tagRefs: List<TransactionTagCrossRef>,
        budgets: List<BudgetEntity>,
        categoryBudgets: List<CategoryBudgetEntity>,
        cycles: List<CycleEntity>,
        cycleLogs: List<CycleLogEntity>,
        moduleBills: List<ModuleBillEntity>,
        dreams: List<DreamEntity>,
        dreamLogs: List<DreamLogEntity>,
        categoryRules: List<CategoryRuleEntity>,
        merchantCategories: List<MerchantCategoryEntity>,
        settings: Map<String, String>,
    ): String {
        val root = JSONObject()

        fun <T> putList(key: String, list: List<T>, serializer: (T) -> JSONObject) {
            val arr = JSONArray()
            list.forEach { arr.put(serializer(it)) }
            root.put(key, arr)
        }

        putList("transactions", transactions) { t ->
            JSONObject().apply {
                put("id", t.id)
                put("amount", t.amount)
                put("category", t.category)
                put("subCategory", t.subCategory)
                put("note", t.note)
                put("merchant", t.merchant)
                put("timestamp", t.timestamp)
                put("source", t.source)
                put("account", t.account)
                put("tag", t.tag)
                put("transactionType", t.transactionType)
                put("refundOriginalId", t.refundOriginalId)
                put("targetAccount", t.targetAccount)
                put("assetId", t.assetId)
                put("bookId", t.bookId)
                put("parentCategoryId", t.parentCategoryId)
                put("childCategoryId", t.childCategoryId)
                put("rawData", t.rawData)
                put("hash", t.hash)
                put("createdAt", t.createdAt)
                put("updatedAt", t.updatedAt)
            }
        }

        putList("assets", assets) { a ->
            JSONObject().apply {
                put("assetId", a.assetId)
                put("assetName", a.assetName)
                put("assetNumber", a.assetNumber)
                put("totalQuota", a.totalQuota)
                put("assetType", a.assetType)
                put("intoTotalAsset", a.intoTotalAsset)
                put("hide", a.hide)
                put("currency", a.currency)
                put("groupName", a.groupName)
                put("positionWeight", a.positionWeight)
                put("createdAt", a.createdAt)
                put("updatedAt", a.updatedAt)
            }
        }

        putList("books", books) { b ->
            JSONObject().apply {
                put("bookId", b.bookId)
                put("bookName", b.bookName)
                put("isDefault", b.isDefault)
                put("positionWeight", b.positionWeight)
                put("createdAt", b.createdAt)
            }
        }

        putList("categories", categories) { c ->
            JSONObject().apply {
                put("id", c.id)
                put("categoryId", c.categoryId)
                put("parentId", c.parentId)
                put("categoryName", c.categoryName)
                put("iconUrl", c.iconUrl)
                put("positionWeight", c.positionWeight)
                put("categoryType", c.categoryType)
                put("isCustom", c.isCustom)
                put("hide", c.hide)
                put("hideBook", c.hideBook)
                put("createdAt", c.createdAt)
            }
        }

        putList("transfers", transfers) { tf ->
            JSONObject().apply {
                put("transferId", tf.transferId)
                put("fromAssetId", tf.fromAssetId)
                put("toAssetId", tf.toAssetId)
                put("cost", tf.cost)
                put("toCost", tf.toCost)
                put("serviceCharge", tf.serviceCharge)
                put("billId", tf.billId)
                put("time", tf.time)
                put("remark", tf.remark)
                put("createdAt", tf.createdAt)
            }
        }

        putList("refunds", refunds) { rf ->
            JSONObject().apply {
                put("refundId", rf.refundId)
                put("billId", rf.billId)
                put("refundNum", rf.refundNum)
                put("refundInfos", rf.refundInfos)
                put("createdAt", rf.createdAt)
            }
        }

        putList("reimbursements", reimbursements) { rb ->
            JSONObject().apply {
                put("reimbursementId", rb.reimbursementId)
                put("billId", rb.billId)
                put("assetId", rb.assetId)
                put("reimbursementAssetId", rb.reimbursementAssetId)
                put("reimbursementNum", rb.reimbursementNum)
                put("reimbursementNumbers", rb.reimbursementNumbers)
                put("end", rb.end)
                put("createdAt", rb.createdAt)
            }
        }

        putList("lends", lends) { l ->
            JSONObject().apply {
                put("lendId", l.lendId)
                put("type", l.type)
                put("assetId", l.assetId)
                put("repaymentAssetId", l.repaymentAssetId)
                put("number", l.number)
                put("interest", l.interest)
                put("billId", l.billId)
                put("fromCost", l.fromCost)
                put("time", l.time)
                put("remark", l.remark)
                put("createdAt", l.createdAt)
            }
        }

        putList("instalments", instalments) { ins ->
            JSONObject().apply {
                put("instalmentId", ins.instalmentId)
                put("billId", ins.billId)
                put("assetId", ins.assetId)
                put("instalmentType", ins.instalmentType)
                put("totalNumber", ins.totalNumber)
                put("periods", ins.periods)
                put("serviceNumber", ins.serviceNumber)
                put("serviceType", ins.serviceType)
                put("remainderType", ins.remainderType)
                put("accountMonth", ins.accountMonth)
                put("createdAt", ins.createdAt)
            }
        }

        putList("tags", tags) { t ->
            JSONObject().apply {
                put("tagId", t.tagId)
                put("tagName", t.tagName)
                put("positionWeight", t.positionWeight)
                put("createdAt", t.createdAt)
            }
        }

        putList("tagRefs", tagRefs) { ref ->
            JSONObject().apply {
                put("transactionId", ref.transactionId)
                put("tagId", ref.tagId)
            }
        }

        putList("budgets", budgets) { b ->
            JSONObject().apply {
                put("budgetId", b.budgetId)
                put("bookId", b.bookId)
                put("budgetName", b.budgetName)
                put("year", b.year)
                put("month", b.month)
                put("type", b.type)
                put("startTime", b.startTime)
                put("endTime", b.endTime)
                put("num", b.num)
                put("addNum", b.addNum)
                put("positionWeight", b.positionWeight)
                put("updateTime", b.updateTime)
            }
        }

        putList("categoryBudgets", categoryBudgets) { cb ->
            JSONObject().apply {
                put("categoryBudgetId", cb.categoryBudgetId)
                put("budgetId", cb.budgetId)
                put("bookId", cb.bookId)
                put("year", cb.year)
                put("month", cb.month)
                put("parentCategory", cb.parentCategory)
                put("childCategory", cb.childCategory)
                put("num", cb.num)
                put("addNum", cb.addNum)
                put("positionWeight", cb.positionWeight)
                put("updateTime", cb.updateTime)
            }
        }

        putList("cycles", cycles) { c ->
            JSONObject().apply {
                put("cycleId", c.cycleId)
                put("bookId", c.bookId)
                put("cycleType", c.cycleType)
                put("cost", c.cost)
                put("serviceCharge", c.serviceCharge)
                put("parentCategoryId", c.parentCategoryId)
                put("childCategoryId", c.childCategoryId)
                put("assetId", c.assetId)
                put("fromAssetId", c.fromAssetId)
                put("toAssetId", c.toAssetId)
                put("dateMode", c.dateMode)
                put("weekdays", c.weekdays)
                put("monthDay", c.monthDay)
                put("monthLast", c.monthLast)
                put("num", c.num)
                put("remark", c.remark)
                put("tags", c.tags)
                put("addTime", c.addTime)
                put("lastAddTime", c.lastAddTime)
                put("nextTime", c.nextTime)
                put("addTimes", c.addTimes)
                put("cycleTimes", c.cycleTimes)
                put("endMode", c.endMode)
                put("cycleEndDate", c.cycleEndDate)
                put("pause", c.pause)
                put("end", c.end)
                put("notIntoBudget", c.notIntoBudget)
                put("notIntoTotal", c.notIntoTotal)
                put("updateTime", c.updateTime)
            }
        }

        putList("cycleLogs", cycleLogs) { cl ->
            JSONObject().apply {
                put("logId", cl.logId)
                put("cycleId", cl.cycleId)
                put("dedupKey", cl.dedupKey)
                put("transactionId", cl.transactionId)
                put("transferId", cl.transferId)
                put("executedTime", cl.executedTime)
                put("amount", cl.amount)
                put("status", cl.status)
            }
        }

        putList("moduleBills", moduleBills) { mb ->
            JSONObject().apply {
                put("moduleBillId", mb.moduleBillId)
                put("bookId", mb.bookId)
                put("moduleType", mb.moduleType)
                put("templateName", mb.templateName)
                put("cost", mb.cost)
                put("parentCategoryId", mb.parentCategoryId)
                put("childCategoryId", mb.childCategoryId)
                put("assetId", mb.assetId)
                put("fromAssetId", mb.fromAssetId)
                put("toAssetId", mb.toAssetId)
                put("serviceCharge", mb.serviceCharge)
                put("remark", mb.remark)
                put("tags", mb.tags)
                put("moduleLock", mb.moduleLock)
                put("notIntoBudget", mb.notIntoBudget)
                put("notIntoTotal", mb.notIntoTotal)
                put("positionWeight", mb.positionWeight)
                put("updateTime", mb.updateTime)
            }
        }

        putList("dreams", dreams) { d ->
            JSONObject().apply {
                put("dreamId", d.dreamId)
                put("dreamName", d.dreamName)
                put("iconUrl", d.iconUrl)
                put("totalNum", d.totalNum)
                put("startNum", d.startNum)
                put("addNum", d.addNum)
                put("startTime", d.startTime)
                put("endTime", d.endTime)
                put("remark", d.remark)
                put("addType", d.addType)
                put("assetMode", d.assetMode)
                put("assetList", d.assetList)
                put("positionWeight", d.positionWeight)
                put("updateTime", d.updateTime)
            }
        }

        putList("dreamLogs", dreamLogs) { dl ->
            JSONObject().apply {
                put("logId", dl.logId)
                put("dreamId", dl.dreamId)
                put("amount", dl.amount)
                put("assetId", dl.assetId)
                put("time", dl.time)
                put("remark", dl.remark)
            }
        }

        putList("categoryRules", categoryRules) { cr ->
            JSONObject().apply {
                put("id", cr.id)
                put("keyword", cr.keyword)
                put("category", cr.category)
                put("isCustom", cr.isCustom)
                put("createdAt", cr.createdAt)
            }
        }

        putList("merchantCategories", merchantCategories) { mc ->
            JSONObject().apply {
                put("id", mc.id)
                put("merchantKey", mc.merchantKey)
                put("category", mc.category)
                put("isIncome", mc.isIncome)
                put("hits", mc.hits)
                put("updatedAt", mc.updatedAt)
            }
        }

        val settingsObj = JSONObject()
        settings.forEach { (k, v) -> settingsObj.put(k, v) }
        root.put("settings", settingsObj)

        return root.toString()
    }

    internal fun deserializeData(jsonString: String): FullBackupData =
        deserializeData(JSONObject(jsonString))

    internal fun deserializeData(root: JSONObject): FullBackupData {
        fun <T> parseArray(key: String, parser: (JSONObject) -> T): List<T> {
            val arr = root.optJSONArray(key) ?: return emptyList()
            val list = mutableListOf<T>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                list.add(parser(item))
            }
            return list
        }

        val transactions = parseArray("transactions") { o ->
            TransactionEntity(
                id = o.optLong("id"),
                amount = o.getDouble("amount"),
                category = o.getString("category"),
                subCategory = o.optString("subCategory").ifEmpty { null },
                note = o.optString("note").ifEmpty { null },
                merchant = o.optString("merchant").ifEmpty { null },
                timestamp = o.getLong("timestamp"),
                source = o.optString("source", "manual"),
                account = o.optString("account").ifEmpty { null },
                tag = o.optString("tag").ifEmpty { null },
                transactionType = o.optString("transactionType", "EXPENSE"),
                refundOriginalId = if (o.has("refundOriginalId") && !o.isNull("refundOriginalId")) o.getLong("refundOriginalId") else null,
                targetAccount = o.optString("targetAccount").ifEmpty { null },
                assetId = o.optLong("assetId", 1L),
                bookId = o.optLong("bookId", 1L),
                parentCategoryId = if (o.has("parentCategoryId") && !o.isNull("parentCategoryId")) o.getInt("parentCategoryId") else null,
                childCategoryId = if (o.has("childCategoryId") && !o.isNull("childCategoryId")) o.getInt("childCategoryId") else null,
                rawData = o.optString("rawData").ifEmpty { null },
                hash = o.optString("hash", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            )
        }

        val assets = parseArray("assets") { o ->
            AssetEntity(
                assetId = o.getLong("assetId"),
                assetName = o.getString("assetName"),
                assetNumber = o.getDouble("assetNumber"),
                totalQuota = o.optDouble("totalQuota", 0.0),
                assetType = o.optInt("assetType", 1),
                intoTotalAsset = o.optBoolean("intoTotalAsset", true),
                hide = o.optBoolean("hide", false),
                currency = o.optString("currency", "CNY"),
                groupName = o.optString("groupName").ifEmpty { null },
                positionWeight = o.optInt("positionWeight", 1),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            )
        }

        val books = parseArray("books") { o ->
            BookEntity(
                bookId = o.getLong("bookId"),
                bookName = o.getString("bookName"),
                isDefault = o.optBoolean("isDefault", false),
                positionWeight = o.optInt("positionWeight", 1),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val categories = parseArray("categories") { o ->
            CategoryEntity(
                id = o.optLong("id"),
                categoryId = o.getInt("categoryId"),
                parentId = o.getInt("parentId"),
                categoryName = o.getString("categoryName"),
                iconUrl = o.optString("iconUrl", ""),
                positionWeight = o.optInt("positionWeight", 1),
                categoryType = o.optInt("categoryType", 0),
                isCustom = o.optBoolean("isCustom", false),
                hide = o.optBoolean("hide", false),
                hideBook = o.optString("hideBook").ifEmpty { null },
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val transfers = parseArray("transfers") { o ->
            TransferEntity(
                transferId = o.optLong("transferId"),
                fromAssetId = o.getLong("fromAssetId"),
                toAssetId = o.getLong("toAssetId"),
                cost = o.getDouble("cost"),
                toCost = o.optDouble("toCost", o.getDouble("cost")),
                serviceCharge = o.optDouble("serviceCharge", 0.0),
                billId = if (o.has("billId") && !o.isNull("billId")) o.getLong("billId") else null,
                time = o.getLong("time"),
                remark = o.optString("remark").ifEmpty { null },
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val refunds = parseArray("refunds") { o ->
            RefundEntity(
                refundId = o.optLong("refundId"),
                billId = o.getLong("billId"),
                refundNum = o.getDouble("refundNum"),
                refundInfos = o.optString("refundInfos", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val reimbursements = parseArray("reimbursements") { o ->
            ReimbursementEntity(
                reimbursementId = o.optLong("reimbursementId"),
                billId = o.getLong("billId"),
                assetId = o.getLong("assetId"),
                reimbursementAssetId = o.getLong("reimbursementAssetId"),
                reimbursementNum = o.getDouble("reimbursementNum"),
                reimbursementNumbers = o.optString("reimbursementNumbers", ""),
                end = o.optBoolean("end", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val lends = parseArray("lends") { o ->
            LendEntity(
                lendId = o.optLong("lendId"),
                type = o.getInt("type"),
                assetId = o.getLong("assetId"),
                repaymentAssetId = o.getLong("repaymentAssetId"),
                number = o.getDouble("number"),
                interest = o.optDouble("interest", 0.0),
                billId = if (o.has("billId") && !o.isNull("billId")) o.getLong("billId") else null,
                fromCost = o.optDouble("fromCost", o.getDouble("number")),
                time = o.getLong("time"),
                remark = o.optString("remark").ifEmpty { null },
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val instalments = parseArray("instalments") { o ->
            InstalmentEntity(
                instalmentId = o.optLong("instalmentId"),
                billId = o.getLong("billId"),
                assetId = o.getLong("assetId"),
                instalmentType = o.getInt("instalmentType"),
                totalNumber = o.getDouble("totalNumber"),
                periods = o.getInt("periods"),
                serviceNumber = o.optDouble("serviceNumber", 0.0),
                serviceType = o.optInt("serviceType", 0),
                remainderType = o.optInt("remainderType", 0),
                accountMonth = o.optString("accountMonth").ifEmpty { null },
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val tags = parseArray("tags") { o ->
            TagEntity(
                tagId = o.optLong("tagId"),
                tagName = o.getString("tagName"),
                positionWeight = o.optInt("positionWeight", 1),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val tagRefs = parseArray("tagRefs") { o ->
            TransactionTagCrossRef(
                transactionId = o.getLong("transactionId"),
                tagId = o.getLong("tagId"),
            )
        }

        val budgets = parseArray("budgets") { o ->
            BudgetEntity(
                budgetId = o.optLong("budgetId"),
                bookId = o.getLong("bookId"),
                budgetName = o.getString("budgetName"),
                year = o.getInt("year"),
                month = o.getInt("month"),
                type = o.optInt("type", 0),
                startTime = o.getLong("startTime"),
                endTime = o.getLong("endTime"),
                num = o.getDouble("num"),
                addNum = o.optDouble("addNum", 0.0),
                positionWeight = o.optInt("positionWeight", 1),
                updateTime = o.optLong("updateTime", System.currentTimeMillis()),
            )
        }

        val categoryBudgets = parseArray("categoryBudgets") { o ->
            CategoryBudgetEntity(
                categoryBudgetId = o.optLong("categoryBudgetId"),
                budgetId = o.getLong("budgetId"),
                bookId = o.getLong("bookId"),
                year = o.getInt("year"),
                month = o.getInt("month"),
                parentCategory = o.getInt("parentCategory"),
                childCategory = o.getInt("childCategory"),
                num = o.getDouble("num"),
                addNum = o.optDouble("addNum", 0.0),
                positionWeight = o.optInt("positionWeight", 1),
                updateTime = o.optLong("updateTime", System.currentTimeMillis()),
            )
        }

        val cycles = parseArray("cycles") { o ->
            CycleEntity(
                cycleId = o.optLong("cycleId"),
                bookId = o.getLong("bookId"),
                cycleType = o.getInt("cycleType"),
                cost = o.getDouble("cost"),
                serviceCharge = o.optDouble("serviceCharge", 0.0),
                parentCategoryId = if (o.has("parentCategoryId") && !o.isNull("parentCategoryId")) o.getInt("parentCategoryId") else null,
                childCategoryId = if (o.has("childCategoryId") && !o.isNull("childCategoryId")) o.getInt("childCategoryId") else null,
                assetId = o.getLong("assetId"),
                fromAssetId = o.optLong("fromAssetId", 1L),
                toAssetId = o.optLong("toAssetId", 1L),
                dateMode = o.getInt("dateMode"),
                weekdays = o.optString("weekdays").ifEmpty { null },
                monthDay = o.optInt("monthDay", 1),
                monthLast = o.optBoolean("monthLast", false),
                num = o.optInt("num", 1),
                remark = o.optString("remark").ifEmpty { null },
                tags = o.optString("tags").ifEmpty { null },
                addTime = o.getLong("addTime"),
                lastAddTime = o.optLong("lastAddTime", 0L),
                nextTime = o.getLong("nextTime"),
                addTimes = o.optInt("addTimes", 0),
                cycleTimes = o.optInt("cycleTimes", 0),
                endMode = o.optInt("endMode", 0),
                cycleEndDate = o.optLong("cycleEndDate", 0L),
                pause = o.optBoolean("pause", false),
                end = o.optBoolean("end", false),
                notIntoBudget = o.optBoolean("notIntoBudget", false),
                notIntoTotal = o.optBoolean("notIntoTotal", false),
                updateTime = o.optLong("updateTime", System.currentTimeMillis()),
            )
        }

        val cycleLogs = parseArray("cycleLogs") { o ->
            CycleLogEntity(
                logId = o.optLong("logId"),
                cycleId = o.getLong("cycleId"),
                dedupKey = o.getString("dedupKey"),
                transactionId = if (o.has("transactionId") && !o.isNull("transactionId")) o.getLong("transactionId") else null,
                transferId = if (o.has("transferId") && !o.isNull("transferId")) o.getLong("transferId") else null,
                executedTime = o.getLong("executedTime"),
                amount = o.getDouble("amount"),
                status = o.optInt("status", 1),
            )
        }

        val moduleBills = parseArray("moduleBills") { o ->
            ModuleBillEntity(
                moduleBillId = o.optLong("moduleBillId"),
                bookId = o.getLong("bookId"),
                moduleType = o.getInt("moduleType"),
                templateName = o.getString("templateName"),
                cost = o.getDouble("cost"),
                parentCategoryId = if (o.has("parentCategoryId") && !o.isNull("parentCategoryId")) o.getInt("parentCategoryId") else null,
                childCategoryId = if (o.has("childCategoryId") && !o.isNull("childCategoryId")) o.getInt("childCategoryId") else null,
                assetId = o.getLong("assetId"),
                fromAssetId = o.optLong("fromAssetId", 1L),
                toAssetId = o.optLong("toAssetId", 1L),
                serviceCharge = o.optDouble("serviceCharge", 0.0),
                remark = o.optString("remark").ifEmpty { null },
                tags = o.optString("tags").ifEmpty { null },
                moduleLock = o.optBoolean("moduleLock", false),
                notIntoBudget = o.optBoolean("notIntoBudget", false),
                notIntoTotal = o.optBoolean("notIntoTotal", false),
                positionWeight = o.optInt("positionWeight", 1),
                updateTime = o.optLong("updateTime", System.currentTimeMillis()),
            )
        }

        val dreams = parseArray("dreams") { o ->
            DreamEntity(
                dreamId = o.optLong("dreamId"),
                dreamName = o.getString("dreamName"),
                iconUrl = o.optString("iconUrl", ""),
                totalNum = o.getDouble("totalNum"),
                startNum = o.optDouble("startNum", 0.0),
                addNum = o.optDouble("addNum", 0.0),
                startTime = o.getLong("startTime"),
                endTime = o.getLong("endTime"),
                remark = o.optString("remark").ifEmpty { null },
                addType = o.optInt("addType", 1),
                assetMode = o.optInt("assetMode", 0),
                assetList = o.optString("assetList").ifEmpty { null },
                positionWeight = o.optInt("positionWeight", 1),
                updateTime = o.optLong("updateTime", System.currentTimeMillis()),
            )
        }

        val dreamLogs = parseArray("dreamLogs") { o ->
            DreamLogEntity(
                logId = o.optLong("logId"),
                dreamId = o.getLong("dreamId"),
                amount = o.getDouble("amount"),
                assetId = if (o.has("assetId") && !o.isNull("assetId")) o.getLong("assetId") else null,
                time = o.getLong("time"),
                remark = o.optString("remark").ifEmpty { null },
            )
        }

        val categoryRules = parseArray("categoryRules") { o ->
            CategoryRuleEntity(
                id = o.optLong("id"),
                keyword = o.getString("keyword"),
                category = o.getString("category"),
                isCustom = o.optBoolean("isCustom", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val merchantCategories = parseArray("merchantCategories") { o ->
            MerchantCategoryEntity(
                id = o.optLong("id"),
                merchantKey = o.getString("merchantKey"),
                category = o.getString("category"),
                isIncome = o.optBoolean("isIncome", false),
                hits = o.optInt("hits", 1),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            )
        }

        val settingsMap = mutableMapOf<String, String>()
        val settingsObj = root.optJSONObject("settings")
        if (settingsObj != null) {
            settingsObj.keys().forEach { key ->
                settingsMap[key] = settingsObj.optString(key)
            }
        }

        return FullBackupData(
            transactions = transactions,
            assets = assets,
            books = books,
            categories = categories,
            transfers = transfers,
            refunds = refunds,
            reimbursements = reimbursements,
            lends = lends,
            instalments = instalments,
            tags = tags,
            transactionTags = tagRefs,
            budgets = budgets,
            categoryBudgets = categoryBudgets,
            cycles = cycles,
            cycleLogs = cycleLogs,
            moduleBills = moduleBills,
            dreams = dreams,
            dreamLogs = dreamLogs,
            categoryRules = categoryRules,
            merchantCategories = merchantCategories,
            settings = settingsMap,
        )
    }
    }
}
