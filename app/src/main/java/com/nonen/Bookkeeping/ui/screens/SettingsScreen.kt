package com.nonen.Bookkeeping.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.prefs.ThemeMode
import com.nonen.Bookkeeping.parse.AlipayBillParser
import com.nonen.Bookkeeping.parse.WechatBillParser
import java.time.LocalDate

/** GitHub Releases 页（自动重定向到最新版本），由系统浏览器打开，应用自身不联网 */
private const val RELEASE_PAGE_URL = "https://github.com/ZnonEn/Lite_Bookkeeping/releases/latest"

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onRules: () -> Unit,
    onCategoryManage: () -> Unit = {},
    onAssetManage: () -> Unit = {},
    onBudget: () -> Unit = {},
    onPeriodic: () -> Unit = {},
    onTemplate: () -> Unit = {},
    onSavingsGoal: () -> Unit = {},
    onBack: () -> Unit = {},
    page: SettingsPage = SettingsPage.ROOT,
    onAppearance: () -> Unit = {},
    onImportExport: () -> Unit = {},
    onBackup: () -> Unit = {},
) {
    val context = LocalContext.current
    var guideSource by remember { mutableStateOf<String?>(null) }
    var pendingSource by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showReclassifyDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refreshRuntimeState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val src = pendingSource
        if (uri != null && src != null) vm.importBillFile(uri, src)
        pendingSource = null
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri ->
        uri?.let { vm.exportBackup(it) }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let { vm.exportCsv(it) }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importBackup(it) }
    }
    var pendingFullRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val fullExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let { vm.exportFullBackup(it) }
    }
    val fullImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingFullRestoreUri = it }
    }

    YimuSettingsPage(
        page, vm, onBack, onAppearance, onImportExport, onBackup,
        onWechat = { guideSource = WechatBillParser.SOURCE },
        onAlipay = { guideSource = AlipayBillParser.SOURCE },
        onImport = { backupLauncher.launch(arrayOf("*/*")) },
        onExport = { exportLauncher.launch("bookkeeping_${LocalDate.now()}.xlsx") },
        onExportCsv = { exportCsvLauncher.launch("bookkeeping_${LocalDate.now()}.csv") },
        onFullExport = { fullExportLauncher.launch("smart_record_${LocalDate.now()}.zip") },
        onFullImport = { fullImportLauncher.launch(arrayOf("application/zip", "*/*")) },
    )

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(stringResource(R.string.dialog_update_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_update_current_version, vm.versionName))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.dialog_update_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    context.startActivity(Intent(Intent.ACTION_VIEW, RELEASE_PAGE_URL.toUri()))
                }) { Text(stringResource(R.string.action_go_releases)) }
            },
        )
    }

    if (showReclassifyDialog) {
        AlertDialog(
            onDismissRequest = { showReclassifyDialog = false },
            title = { Text(stringResource(R.string.dialog_reclassify_title)) },
            text = { Text(stringResource(R.string.dialog_reclassify_message)) },
            dismissButton = { TextButton(onClick = { showReclassifyDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
            confirmButton = {
                TextButton(onClick = {
                    showReclassifyDialog = false
                    vm.reclassifyAll()
                }) { Text(stringResource(R.string.action_confirm)) }
            },
        )
    }

    guideSource?.let { source ->
        AlertDialog(
            onDismissRequest = { guideSource = null },
            title = {
                Text(
                    stringResource(
                        if (source == WechatBillParser.SOURCE) R.string.dialog_wechat_export_title
                        else R.string.dialog_alipay_export_title,
                    ),
                )
            },
            text = { Text(guideText(source)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingSource = source
                    importLauncher.launch(arrayOf("*/*"))
                    guideSource = null
                }) { Text(stringResource(R.string.action_choose_file)) }
            },
            dismissButton = { TextButton(onClick = { guideSource = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    pendingFullRestoreUri?.let { restoreUri ->
        AlertDialog(
            onDismissRequest = { pendingFullRestoreUri = null },
            title = { Text("确认恢复本地全量备份？") },
            text = {
                Text(
                    "该操作将通过备份包完整覆盖现有的全部账单流水、资产账户、分类设置、预算与附件数据。\n\n" +
                        "系统在恢复前会自动执行完整性校验与格式检查；若校验失败将自动终止并完整保留现有数据。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = restoreUri
                    pendingFullRestoreUri = null
                    vm.importFullBackup(target)
                }) { Text("确认覆盖恢复", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingFullRestoreUri = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** 外观与显示分组：主题模式、动态色彩、金额打码、应用安全锁 */
@Composable
private fun AppearanceSection(vm: SettingsViewModel) {
    CollapsibleSection(title = stringResource(R.string.section_appearance), emoji = "🎨") {
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(
                "主题模式",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ThemeMode.entries.forEach { mode ->
                RadioRow(
                    label = stringResource(mode.labelRes),
                    selected = vm.themeMode == mode,
                    onClick = { vm.updateThemeMode(mode) },
                )
            }
            SectionDivider()
            ToggleRow(
                title = stringResource(R.string.yimu_navigation_labels),
                subtitle = "显示底部导航文字",
                checked = vm.navigationLabelsEnabled,
                onChecked = vm::updateNavigationLabels,
            )
            SectionDivider()
            ToggleRow(
                title = "首屏敏感金额打码",
                subtitle = "首页账单金额默认以星号保护隐私",
                checked = vm.hideAmount,
                onChecked = vm::updateHideAmount,
            )
            SectionDivider()
            ToggleRow(
                title = "应用安全锁",
                subtitle = "切入后台或重新打开时需验证指纹或锁屏密码",
                checked = vm.appLockEnabled,
                onChecked = vm::updateAppLockEnabled,
            )
        }
    }
}


/** 账单导入分组 */
@Composable
private fun BillImportSection(
    importing: Boolean,
    importProgress: Float,
    statusMessage: String?,
    onImportWechat: () -> Unit,
    onImportAlipay: () -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.bill_import_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ActionButtonRow {
            SettingsActionButton(stringResource(R.string.action_import_wechat), onImportWechat)
            SettingsActionButton(stringResource(R.string.action_import_alipay), onImportAlipay)
        }
    }
    if (importing) {
        ImportProgressRow(
            progress = importProgress,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
        )
    }
    statusMessage?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
        )
    }
}

/** 分类规则分组 */
@Composable
private fun CategoryRuleSection(
    reclassifying: Boolean,
    result: String?,
    onManage: () -> Unit,
    onCategoryManage: () -> Unit,
    onReclassify: () -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        ActionButtonRow {
            SettingsActionButton("分类管理", onCategoryManage)
            SettingsActionButton(stringResource(R.string.action_manage_rules), onManage)
        }
        Spacer(Modifier.height(8.dp))
        SettingsActionButton(
            text = stringResource(if (reclassifying) R.string.action_reclassifying else R.string.action_reclassify),
            onClick = onReclassify,
            enabled = !reclassifying,
        )
        result?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** 数据备份与导出分组 */
@Composable
private fun BackupSection(
    importing: Boolean,
    importProgress: Float,
    statusMessage: String?,
    onExport: () -> Unit,
    onExportCsv: () -> Unit,
    onImport: () -> Unit,
    onFullExport: () -> Unit,
    onFullImport: () -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        Text(
            "一木记账 23 列标准格式账单导出与导入：",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "支持将流水导出为 23 列标准 Excel (.xlsx) 或 CSV 文件；支持导入官方或本应用导出的 23 列账单，按哈希自动去重。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        ActionButtonRow {
            SettingsActionButton("导出 Excel 账单", onExport)
            SettingsActionButton("导出 CSV 账单", onExportCsv)
        }
        Spacer(Modifier.height(8.dp))
        SettingsActionButton("导入 23 列 Excel 账单", onImport)

        Spacer(Modifier.height(20.dp))
        Text(
            "本地全量数据完整备份与恢复：",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "完整打包全部账单、资产账户、多账本、分类、预算、周期、模板、储蓄目标、偏好配置与本地附件；恢复过程带完整性校验与事务保护。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        ActionButtonRow {
            SettingsActionButton("创建本地完整备份", onFullExport)
            SettingsActionButton("恢复本地完整备份", onFullImport)
        }

        if (importing) {
            Spacer(Modifier.height(14.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { importProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        statusMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** 关于分组 */
@Composable
private fun AboutSection(versionName: String, onCheckUpdate: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.about_version, versionName), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    TextButton(onClick = onCheckUpdate, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(stringResource(R.string.action_check_update))
    }
}

@Composable
private fun guideText(source: String): String = if (source == WechatBillParser.SOURCE) {
    stringResource(R.string.dialog_wechat_export_steps)
} else {
    stringResource(R.string.dialog_alipay_export_steps)
}
