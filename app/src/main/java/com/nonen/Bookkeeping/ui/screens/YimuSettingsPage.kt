package com.nonen.Bookkeeping.ui.screens

import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import android.content.res.ColorStateList
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyleduo.switchbutton.SwitchButton
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.prefs.ThemeMode
import com.nonen.Bookkeeping.ui.components.YimuTopAppBar
import com.nonen.Bookkeeping.ui.components.YimuImportExportNativeView
import com.nonen.Bookkeeping.ui.components.YimuBackupNativeView
import com.nonen.Bookkeeping.ui.theme.YimuTokens

enum class SettingsPage { ROOT, APPEARANCE, IMPORT_EXPORT, BACKUP }

@Composable
fun YimuSettingsPage(
    page: SettingsPage,
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onAppearance: () -> Unit,
    onImportExport: () -> Unit,
    onBackup: () -> Unit,
    onWechat: () -> Unit,
    onAlipay: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onExportCsv: () -> Unit,
    onFullExport: () -> Unit,
    onFullImport: () -> Unit,
) {
    val context = LocalContext.current
    val statusBarPx = WindowInsets.statusBars.getTop(LocalDensity.current)
    val unsupported = { Toast.makeText(context, R.string.yimu_local_unsupported, Toast.LENGTH_SHORT).show() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).semantics { testTagsAsResourceId = true }) {
        if (page == SettingsPage.APPEARANCE) {
            val barTitle = "主题外观"
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { viewContext ->
                    com.nonen.Bookkeeping.ui.components.YimuTopBarNativeView(viewContext).apply {
                        setTitle(barTitle)
                        this.onBack = { onBack() }
                    }
                },
                update = { view -> view.applyToolbarInset(statusBarPx) },
            )
        }
        val scrollable = page == SettingsPage.ROOT || page == SettingsPage.APPEARANCE
        // 全屏原生二级页（导入导出/数据备份）不能吃底部 20dp 留白，否则内容被导航条区域截断 65px
        val contentModifier = if (scrollable) Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 20.dp)
            else Modifier.weight(1f)
        Column(contentModifier) {
            when (page) {
                SettingsPage.ROOT -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext -> com.nonen.Bookkeeping.ui.components.YimuSettingNativeView(viewContext) },
                    update = { view ->
                        view.applyToolbarInset(statusBarPx)
                        view.bind(object : com.nonen.Bookkeeping.ui.components.YimuSettingNativeView.Listener {
                            override fun onBack() = onBack()
                            override fun onSwitchChanged(key: String, checked: Boolean) {
                                when (key) {
                                    "switch_lock" -> vm.updateAppLockEnabled(checked)
                                    "switch_remark_main" -> vm.updateNoteFirst(checked)
                                    "switch_category_simple" -> vm.updateSimpleCategory(checked)
                                    "switch_show_time" -> vm.updateShowTransactionTime(checked)
                                    "switch_show_asset_icon" -> vm.updateShowAssetIcon(checked)
                                    "navigation_labels" -> vm.updateNavigationLabels(checked)
                                    else -> unsupported()
                                }
                            }
                            override fun onAction(key: String) {
                                when (key) {
                                    "appearance" -> onAppearance()
                                    "backup" -> onBackup()
                                    "import_export" -> onImportExport()
                                    else -> unsupported()
                                }
                            }
                            override fun switchState(key: String): Boolean = when (key) {
                                "switch_lock" -> vm.appLockEnabled
                                "switch_remark_main" -> vm.noteFirst
                                "switch_category_simple" -> vm.simpleCategory
                                "switch_show_time" -> vm.showTransactionTime
                                "switch_show_old_cost" -> true
                                "switch_show_asset_icon" -> vm.showAssetIcon
                                "navigation_labels" -> vm.navigationLabelsEnabled
                                else -> false
                            }
                        })
                    },
                )
                SettingsPage.APPEARANCE -> SettingGroup("主题模式") {
                    ThemeMode.entries.forEach { mode ->
                        Row(Modifier.fillMaxWidth().height(55.dp).clickable { vm.updateThemeMode(mode) }.padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(vm.themeMode == mode, { vm.updateThemeMode(mode) })
                            Text(stringResource(mode.labelRes), fontSize = 15.sp)
                        }
                    }
                    SettingSwitch("敏感金额保护", "隐藏首页金额", "hide_amount", vm.hideAmount, onChecked = vm::updateHideAmount)
                }
                SettingsPage.IMPORT_EXPORT -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        com.nonen.Bookkeeping.ui.components.YimuTopBarNativeView(viewContext).apply {
                            setTitle("导入/导出")
                            this.onBack = { onBack() }
                            setContent(YimuImportExportNativeView(viewContext).apply {
                                bind(object : YimuImportExportNativeView.Listener {
                                    override fun onImport() = onImport()
                                    override fun onExport() = onExport()
                                    override fun onExportCsv() = onExportCsv()
                                    override fun onUnsupported() = unsupported()
                                })
                            })
                        }
                    },
                    update = { view -> view.applyToolbarInset(statusBarPx) },
                )
                SettingsPage.BACKUP -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        com.nonen.Bookkeeping.ui.components.YimuTopBarNativeView(viewContext).apply {
                            setTitle("数据备份")
                            this.onBack = { onBack() }
                            setContent(YimuBackupNativeView(viewContext).apply {
                                bind(object : YimuBackupNativeView.Listener {
                                    override fun onFullExport() = onFullExport()
                                    override fun onFullImport() = onFullImport()
                                    override fun onUnsupported() = unsupported()
                                })
                            })
                        }
                    },
                    update = { view -> view.applyToolbarInset(statusBarPx) },
                )
            }
            if (vm.importing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(15.dp))
            vm.statusMessage?.let { Text(it, Modifier.padding(15.dp), fontSize = 13.sp) }
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(start = 15.dp, end = 15.dp, top = 15.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)) {
        // 原版 activity_setting.xml 分组标题 12sp @color/grey(#999999)
        Text(title, Modifier.padding(start = 15.dp, top = 15.dp, bottom = 5.dp), fontSize = 12.sp, color = YimuTokens.TextTertiary)
        content()
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, tag: String, checked: Boolean, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    val color = MaterialTheme.colorScheme.primary.toArgb()
    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                // 原版 activity_setting.xml 副标题 12sp @color/grey(#999999)
                Text(subtitle, fontSize = 12.sp, color = YimuTokens.TextTertiary, maxLines = 1)
            }
        }
        AndroidView(modifier = Modifier.size(40.dp, 24.dp).testTag(tag), factory = { SwitchButton(it) }, update = {
            it.setOnCheckedChangeListener(null)
            // 原版 switch 未自定轨道色，走库默认：选中 #DDF0EB / 未选中 #DFDFDF（参考实测 221,240,235 与 223,223,223）
            it.setBackColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(0xFFDDF0EB.toInt(), 0xFFDFDFDF.toInt())))
            it.setThumbColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(color, 0xFFCAC2C2.toInt())))
            it.isChecked = checked
            it.isEnabled = enabled
            it.setOnCheckedChangeListener { _, value -> onChecked(value) }
        })
    }
}

@Composable
private fun SettingAction(title: String, subtitle: String = "", tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp).testTag(tag).clickable(onClick = onClick).padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp)
            if (subtitle.isNotBlank()) { Spacer(Modifier.height(3.dp)); Text(subtitle, fontSize = 12.sp, color = YimuTokens.TextTertiary) }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
    }
}
