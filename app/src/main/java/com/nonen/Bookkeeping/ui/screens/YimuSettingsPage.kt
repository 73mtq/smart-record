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
    val unsupported = { Toast.makeText(context, R.string.yimu_local_unsupported, Toast.LENGTH_SHORT).show() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).semantics { testTagsAsResourceId = true }) {
        YimuTopAppBar(title = { Text(when (page) { SettingsPage.ROOT -> "设置"; SettingsPage.APPEARANCE -> "主题外观"; SettingsPage.IMPORT_EXPORT -> "导入导出"; SettingsPage.BACKUP -> "数据备份" }) },
            navigationIcon = { AndroidView(modifier = Modifier.size(50.dp), factory = { viewContext ->
                ImageView(viewContext).apply {
                    id = R.id.btn_back
                    setImageResource(R.mipmap.ic_back)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding((16 * resources.displayMetrics.density).toInt(), (16 * resources.displayMetrics.density).toInt(),
                        (16 * resources.displayMetrics.density).toInt(), (16 * resources.displayMetrics.density).toInt())
                    imageTintList = ColorStateList.valueOf(viewContext.getColor(R.color.black))
                    setOnClickListener { onBack() }
                }
            }) })
        val scrollable = page == SettingsPage.ROOT || page == SettingsPage.APPEARANCE
        Column(Modifier.weight(1f).then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(bottom = 20.dp)) {
            when (page) {
                SettingsPage.ROOT -> {
                    SettingGroup("安全锁") {
                        SettingSwitch("生物解锁", "打开应用时执行指纹、人脸等安全认证", "switch_lock", vm.appLockEnabled, onChecked = vm::updateAppLockEnabled)
                        SettingSwitch("手势解锁", "启动应用时或指纹识别失败时进行认证", "switch_gesture", false) { unsupported() }
                    }
                    SettingGroup("账单设置") {
                        SettingSwitch("多币种", "为账单、账户设置币种", "switch_currency", false) { unsupported() }
                        SettingSwitch("备注优先显示", "将备注作为标题，分类作为描述", "switch_remark_main", vm.noteFirst, onChecked = vm::updateNoteFirst)
                        SettingSwitch("分类精简", "拥有二级分类的账单将不显示一级分类", "switch_category_simple", vm.simpleCategory, onChecked = vm::updateSimpleCategory)
                        SettingSwitch("显示账单时间", "在账单流水中显示账单的具体时间", "switch_show_time", vm.showTransactionTime, onChecked = vm::updateShowTransactionTime)
                        SettingSwitch("显示原金额", "退款、优惠账单显示原金额", "switch_show_old_cost", true) { unsupported() }
                        SettingSwitch("显示账户图标", "在流水中显示账户图标", "switch_show_asset_icon", vm.showAssetIcon, onChecked = vm::updateShowAssetIcon)
                        SettingSwitch("自动定位", "在记账时，会自动记录记账所在地点", "switch_location", false) { unsupported() }
                        SettingAction("金额颜色", "支出红色，收入绿色", "bill_num_color", unsupported)
                    }
                    SettingGroup("界面设置") {
                        SettingSwitch("导航文字", "显示底部导航文字", "navigation_labels", vm.navigationLabelsEnabled, onChecked = vm::updateNavigationLabels)
                        SettingAction("主题外观", tag = "appearance", onClick = onAppearance)
                    }
                    SettingGroup("数据管理") {
                        SettingAction("数据备份", tag = "backup", onClick = onBackup)
                        SettingAction("导入导出", tag = "import_export", onClick = onImportExport)
                    }
                }
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
                    factory = { YimuImportExportNativeView(it) },
                    update = { view ->
                        view.bind(object : YimuImportExportNativeView.Listener {
                            override fun onImport() = onImport()
                            override fun onExport() = onExport()
                            override fun onExportCsv() = onExportCsv()
                            override fun onUnsupported() = unsupported()
                        })
                    },
                )
                SettingsPage.BACKUP -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { YimuBackupNativeView(it) },
                    update = { view ->
                        view.bind(object : YimuBackupNativeView.Listener {
                            override fun onFullExport() = onFullExport()
                            override fun onFullImport() = onFullImport()
                            override fun onUnsupported() = unsupported()
                        })
                    },
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
        Text(title, Modifier.padding(start = 15.dp, top = 15.dp, bottom = 5.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        AndroidView(modifier = Modifier.size(40.dp, 24.dp).testTag(tag), factory = { SwitchButton(it) }, update = {
            it.setOnCheckedChangeListener(null)
            it.setBackColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(0xFFDDF0EB.toInt(), 0xFFE5E5E5.toInt())))
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
            if (subtitle.isNotBlank()) { Spacer(Modifier.height(3.dp)); Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
    }
}
