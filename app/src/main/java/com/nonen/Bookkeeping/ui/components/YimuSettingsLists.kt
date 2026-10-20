package com.nonen.Bookkeeping.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.kyleduo.switchbutton.SwitchButton
import com.nonen.Bookkeeping.R

class YimuImportExportNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onImport()
        fun onExport()
        fun onExportCsv()
        fun onUnsupported()
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.yimu_import_export, this, true)
    }

    fun bind(listener: Listener) {
        click(listener::onUnsupported, R.id.import_screenshot, R.id.import_manager, R.id.import_guide,
            R.id.import_module_download, R.id.import_scheme, R.id.transfer_export_excel, R.id.lend_export_excel,
            R.id.stock_export_excel, R.id.asset_export_excel, R.id.export_search, R.id.export_guide)
        click(listener::onImport, R.id.import_excel)
        click(listener::onExport, R.id.bill_export_excel)
    }

    private fun click(action: () -> Unit, vararg ids: Int) {
        ids.forEach { id -> findViewById<View>(id)?.setOnClickListener { action() } }
    }
}

class YimuBackupNativeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    interface Listener {
        fun onFullExport()
        fun onFullImport()
        fun onUnsupported()
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.yimu_backup, this, true)
    }

    fun bind(listener: Listener) {
        listOf(R.id.switch_remote, R.id.switch_many_device, R.id.switch_file_auto).forEach { id ->
            findViewById<SwitchButton>(id)?.apply {
                isChecked = false
                isEnabled = false
                setBackColor(stateColors(0xFFDDF0EB.toInt(), 0xFFE5E5E5.toInt()))
                setThumbColor(stateColors(0xFF48AB93.toInt(), 0xFFCAC2C2.toInt()))
            }
        }
        click(listener::onFullExport, R.id.local_backup)
        click(listener::onFullImport, R.id.restore_backup, R.id.restore_backup_more)
        click(listener::onUnsupported,
            R.id.data_sync_all, R.id.data_sync_all_more, R.id.data_sync_download, R.id.data_sync_download_more,
            R.id.many_device_tip, R.id.data_clear, R.id.local_data_clear, R.id.history_data, R.id.local_backup_move,
            R.id.local_backup_path, R.id.local_backup_file_manager, R.id.auto_backup_tip, R.id.auto_backup,
            R.id.backup_path, R.id.backup_path_info, R.id.webdav_setup, R.id.webdav_status, R.id.webdav_setup_more,
            R.id.webdav_backup, R.id.webdav_ic_more, R.id.restore_webdav_backup, R.id.remote_backup_file_manager,
            R.id.auto_webdav_backup, R.id.webdav_backup_num_layout, R.id.webdav_backup_num, R.id.webdav_backup_more)
    }

    private fun stateColors(checked: Int, unchecked: Int): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(checked, unchecked))

    private fun click(action: () -> Unit, vararg ids: Int) {
        ids.forEach { id -> findViewById<View>(id)?.setOnClickListener { action() } }
    }
}
