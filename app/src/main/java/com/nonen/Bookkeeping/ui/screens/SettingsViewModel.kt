package com.nonen.Bookkeeping.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.core.AccessibilityUtil
import com.nonen.Bookkeeping.core.HashUtil
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.prefs.ListenScope
import com.nonen.Bookkeeping.data.prefs.ThemeMode
import com.nonen.Bookkeeping.export.BackupExporter
import com.nonen.Bookkeeping.parse.AlipayBillParser
import com.nonen.Bookkeeping.parse.BackupExcelParser
import com.nonen.Bookkeeping.parse.WechatBillParser
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 设置页状态与业务动作。
 * 与 UI 解耦：所有耗时/IO 操作都在这里，Composable 只读状态、发事件。
 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val settings = container.settings

    /**
     * Application context：仅用于读取字符串资源与 ContentResolver。
     * lint 的 StaticFieldLeak 针对 Activity/Fragment context，Application 实例与进程同生命周期，
     * 不构成泄漏。
     */
    @Suppress("StaticFieldLeak")
    private val ctx = container.appContext

    var autoRecord by mutableStateOf(true)
    var listenScope by mutableStateOf(ListenScope.ALL)
    var notifyOnRecord by mutableStateOf(true)
    var learnOnEdit by mutableStateOf(true)
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var dynamicColor by mutableStateOf(true)
    var hideAmount by mutableStateOf(false)
    var appLockEnabled by mutableStateOf(false)
    var fontOption by mutableStateOf("DEFAULT")
    var navigationLabelsEnabled by mutableStateOf(false)
    var noteFirst by mutableStateOf(false)
    var simpleCategory by mutableStateOf(false)
    var showTransactionTime by mutableStateOf(false)
    var showAssetIcon by mutableStateOf(false)
    var importing by mutableStateOf(false)
        private set
    /** -1 = 解析文件中（不定态进度），0..1 = 逐行导入进度 */
    var importProgress by mutableFloatStateOf(-1f)
        private set
    var statusMessage by mutableStateOf<String?>(null)
    var accessibilityEnabled by mutableStateOf(false)
        private set
    var notificationAccessEnabled by mutableStateOf(false)
        private set
    var overlayPermissionEnabled by mutableStateOf(false)
        private set
    var versionName by mutableStateOf("")
        private set
    var reclassifying by mutableStateOf(false)
        private set
    var reclassifyResult by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            val s = settings.snapshot()
            autoRecord = s.autoRecordEnabled
            listenScope = s.listenScope
            notifyOnRecord = s.notifyOnRecord
            learnOnEdit = s.learnOnEdit
            themeMode = s.themeMode
            dynamicColor = s.dynamicColor
            hideAmount = s.hideAmount
            appLockEnabled = s.appLockEnabled
            fontOption = s.fontOption
            navigationLabelsEnabled = s.navigationLabelsEnabled
            noteFirst = s.noteFirst
            simpleCategory = s.simpleCategory
            showTransactionTime = s.showTransactionTime
            showAssetIcon = s.showAssetIcon
        }
        refreshRuntimeState()
        versionName = runCatching {
            container.appContext.packageManager
                .getPackageInfo(container.appContext.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

    /** 重新读取权限与服务的实时状态（从系统设置返回时调用） */
    fun refreshRuntimeState() {
        accessibilityEnabled = AccessibilityUtil.isServiceEnabled(container.appContext)
        notificationAccessEnabled = androidx.core.app.NotificationManagerCompat
            .getEnabledListenerPackages(container.appContext)
            .contains(container.appContext.packageName)
        overlayPermissionEnabled = android.provider.Settings.canDrawOverlays(container.appContext)
    }

    fun updateAutoRecord(v: Boolean) {
        autoRecord = v
        viewModelScope.launch { settings.setAutoRecordEnabled(v) }
    }

    fun updateListenScope(v: ListenScope) {
        listenScope = v
        viewModelScope.launch { settings.setListenScope(v) }
    }

    fun updateNotify(v: Boolean) {
        notifyOnRecord = v
        viewModelScope.launch { settings.setNotifyOnRecord(v) }
    }

    fun updateLearn(v: Boolean) {
        learnOnEdit = v
        viewModelScope.launch { settings.setLearnOnEdit(v) }
    }

    fun updateThemeMode(v: ThemeMode) {
        themeMode = v
        viewModelScope.launch { settings.setThemeMode(v) }
    }

    fun updateDynamicColor(v: Boolean) {
        dynamicColor = v
        viewModelScope.launch { settings.setDynamicColor(v) }
    }

    fun updateHideAmount(v: Boolean) {
        hideAmount = v
        viewModelScope.launch { settings.setHideAmount(v) }
    }

    fun updateAppLockEnabled(v: Boolean) {
        appLockEnabled = v
        viewModelScope.launch { settings.setAppLockEnabled(v) }
    }

    fun updateFontOption(v: String) {
        fontOption = v
        viewModelScope.launch { settings.setFontOption(v) }
    }

    fun updateNavigationLabels(v: Boolean) {
        navigationLabelsEnabled = v
        viewModelScope.launch { settings.setNavigationLabelsEnabled(v) }
    }

    fun updateNoteFirst(value: Boolean) { noteFirst = value; viewModelScope.launch { settings.setNoteFirst(value) } }
    fun updateSimpleCategory(value: Boolean) { simpleCategory = value; viewModelScope.launch { settings.setSimpleCategory(value) } }
    fun updateShowTransactionTime(value: Boolean) { showTransactionTime = value; viewModelScope.launch { settings.setShowTransactionTime(value) } }
    fun updateShowAssetIcon(value: Boolean) { showAssetIcon = value; viewModelScope.launch { settings.setShowAssetIcon(value) } }

    /** 按当前分类规则重算全部历史账单的分类（覆盖手动改过的分类，学习规则优先） */
    fun reclassifyAll() {
        if (reclassifying) return
        viewModelScope.launch {
            reclassifying = true
            val changed = container.transactionRepository.reclassifyAll()
            reclassifying = false
            reclassifyResult = ctx.getString(R.string.status_reclassified, changed)
        }
    }

    /** 导入微信/支付宝官方账单文件 */
    fun importBillFile(uri: Uri, source: String) {
        viewModelScope.launch {
            importing = true
            importProgress = -1f
            statusMessage = runCatching {
                val bytes = readBytesOrThrow(uri)
                val rows = when (source) {
                    WechatBillParser.SOURCE -> WechatBillParser.parse(bytes)
                    else -> AlipayBillParser.parse(bytes)
                }
                if (rows.none { !it.skipped }) {
                    ctx.getString(R.string.status_import_empty)
                } else {
                    importProgress = 0f
                    val total = rows.size
                    val r = container.billImporter.import(rows, source, onProgress = { done, _ ->
                        importProgress = done.toFloat() / total
                    })
                    importProgress = 1f
                    ctx.getString(R.string.status_import_done, r.success, r.duplicates, r.failed, r.skipped)
                }
            }.getOrElse { ctx.getString(R.string.status_import_failed, it.message.orEmpty()) }
            importing = false
        }
    }

    /** 导出一木 23 列标准 Excel 格式 */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            statusMessage = runCatching {
                val all = container.transactionRepository.getAll()
                val assetMap = container.assetDao.getAll().associate { it.assetId to it.assetName }
                val bookMap = container.bookDao.getAll().associate { it.bookId to it.bookName }
                val catMap = container.categoryDao.getAll().associate { it.categoryId to it.categoryName }
                val bytes = BackupExporter.buildXlsx(all, assetMap, bookMap, catMap)
                container.appContext.contentResolver.openOutputStream(uri)
                    ?.use { it.write(bytes) }
                    ?: error(ctx.getString(R.string.error_cannot_write_file))
                ctx.getString(R.string.status_backup_exported, all.size)
            }.getOrElse { ctx.getString(R.string.status_backup_export_failed, it.message.orEmpty()) }
        }
    }

    /** 导出一木 23 列标准 CSV 格式 */
    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            statusMessage = runCatching {
                val all = container.transactionRepository.getAll()
                val assetMap = container.assetDao.getAll().associate { it.assetId to it.assetName }
                val bookMap = container.bookDao.getAll().associate { it.bookId to it.bookName }
                val catMap = container.categoryDao.getAll().associate { it.categoryId to it.categoryName }
                val bytes = BackupExporter.buildCsv(all, assetMap, bookMap, catMap)
                container.appContext.contentResolver.openOutputStream(uri)
                    ?.use { it.write(bytes) }
                    ?: error(ctx.getString(R.string.error_cannot_write_file))
                "成功导出 ${all.size} 条账单至 CSV 文件"
            }.getOrElse { "CSV 导出失败：${it.message}" }
        }
    }

    /** 导入一木 23 列 Excel 账单 */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            importing = true
            importProgress = -1f
            statusMessage = runCatching {
                val bytes = readBytesOrThrow(uri)
                val rows = BackupExcelParser.parse(bytes)
                    ?: error(ctx.getString(R.string.error_not_backup_format))
                if (rows.isEmpty()) error(ctx.getString(R.string.error_backup_empty))
                importProgress = 0f
                val total = rows.size
                var success = 0
                var duplicates = 0
                var failed = 0

                val assetMap = container.assetDao.getAll().associate { it.assetName to it.assetId }
                val bookMap = container.bookDao.getAll().associate { it.bookName to it.bookId }
                val catMap = container.categoryDao.getAll().associate {
                    it.categoryName to (if (it.parentId == 0) it.categoryId to null else it.parentId to it.categoryId)
                }

                rows.forEachIndexed { index, row ->
                    val entity = BackupExcelParser.toEntity(
                        row = row,
                        assetNameToId = assetMap,
                        bookNameToId = bookMap,
                        categoryNameToIds = catMap,
                    )
                    if (entity == null) {
                        failed++
                    } else {
                        if (container.transactionRepository.insertIfNew(entity)) success++ else duplicates++
                    }
                    importProgress = (index + 1).toFloat() / total
                }
                importProgress = 1f
                ctx.getString(R.string.status_backup_imported, success, duplicates, failed)
            }.getOrElse { ctx.getString(R.string.status_import_failed, it.message.orEmpty()) }
            importing = false
        }
    }

    /** 导出包含全量数据、配置与附件的完整备份包（ZIP） */
    fun exportFullBackup(uri: Uri) {
        viewModelScope.launch {
            importing = true
            importProgress = -1f
            statusMessage = runCatching {
                val outStream = container.appContext.contentResolver.openOutputStream(uri)
                    ?: error(ctx.getString(R.string.error_cannot_write_file))
                val result = outStream.use { stream ->
                    container.fullBackupService.exportBackup(stream)
                }
                if (result.success) result.message else error(result.error ?: "备份导出异常")
            }.getOrElse { "全量备份失败：${it.message}" }
            importing = false
        }
    }

    /** 校验并原子恢复全量备份包（ZIP） */
    fun importFullBackup(uri: Uri) {
        viewModelScope.launch {
            importing = true
            importProgress = -1f
            statusMessage = runCatching {
                val inStream = container.appContext.contentResolver.openInputStream(uri)
                    ?: error(ctx.getString(R.string.error_cannot_read_file))
                val result = inStream.use { stream ->
                    container.fullBackupService.restoreBackup(stream)
                }
                if (result.success) result.message else error(result.error ?: "备份恢复异常")
            }.getOrElse { "全量恢复中断：${it.message}" }
            importing = false
        }
    }

    private fun readBytesOrThrow(uri: Uri): ByteArray =
        container.appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error(ctx.getString(R.string.error_cannot_read_file))
}
