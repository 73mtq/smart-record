package com.nonen.Bookkeeping.ui.screens

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.TemplateRepository
import com.nonen.Bookkeeping.ui.components.NativeTemplateState
import com.nonen.Bookkeeping.ui.components.bindNativeTemplateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 模板记账页：按原版 activity_module_manager 结构还原工具栏、提示条、列表与空态。
 * 新增入口提供「模板记账」「模板转账」两项，分别以模块类型 0 与 1 进入模板编辑。
 */
@Composable
fun TemplateScreen(
    viewModel: TemplateViewModel,
    categoryRepository: CategoryRepository,
    assetRepository: AssetRepository,
    templateRepository: TemplateRepository,
    settings: SettingsStore,
    onBack: () -> Unit,
    onCreate: (Int) -> Unit,
    onEdit: (Long) -> Unit,
    onApply: (Long) -> Unit,
) {
    val templates by viewModel.templates.collectAsState()
    val tipHidden by settings.templateTipHidden.collectAsState(initial = false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var chooseType by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var assets by remember { mutableStateOf<List<AssetEntity>>(emptyList()) }
    LaunchedEffect(Unit) {
        categories = categoryRepository.getAll()
        assets = assetRepository.getAll()
    }
    val colors = MaterialTheme.colorScheme
    val state = NativeTemplateState(
        templates = templates,
        categories = categories,
        assets = assets,
        text = colors.onSurface.toArgb(),
        secondary = colors.onSurfaceVariant.toArgb(),
    )
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                LayoutInflater.from(viewContext).inflate(R.layout.view_yimu_template_manager, null).apply {
                    bindChrome(viewContext, this, onBack, { chooseType = true }, settings, scope, snackbarHostState)
                }
            },
            update = { root ->
                val list = root.findViewById<RecyclerView>(R.id.module_bill_list)
                bindNativeTemplateList(
                    list = list,
                    state = state,
                    onApply = onApply,
                    onEdit = onEdit,
                    onLockedApply = { id ->
                        scope.launch {
                            val name = requireNotNull(templateRepository.getById(id)).templateName
                            viewModel.applyTemplate(id) {
                                scope.launch { snackbarHostState.showSnackbar("已根据模板「$name」快速记账成功") }
                            }
                        }
                    },
                )
                root.findViewById<LinearLayout>(R.id.no_data_layout).visibility =
                    if (templates.isEmpty()) View.VISIBLE else View.GONE
                root.findViewById<RelativeLayout>(R.id.text_tip_layout).visibility =
                    if (tipHidden) View.GONE else View.VISIBLE
            },
        )
        SnackbarHost(snackbarHostState)
    }
    if (chooseType) {
        AlertDialog(
            onDismissRequest = { chooseType = false },
            title = { Text("新增模板") },
            text = {
                Column {
                    TextButton(onClick = { chooseType = false; onCreate(0) }) { Text("模板记账") }
                    TextButton(onClick = { chooseType = false; onCreate(1) }) { Text("模板转账") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseType = false }) { Text("取消") } },
        )
    }
}

private fun bindChrome(
    context: android.content.Context,
    root: View,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    settings: SettingsStore,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
) {
    root.findViewById<TextView>(R.id.title).text = context.getString(R.string.yimu_template_manager_title)
    root.findViewById<TextView>(R.id.right_text).text = context.getString(R.string.yimu_template_add)
    root.findViewById<TextView>(R.id.yimu_template_hint_text).text = context.getString(R.string.yimu_template_hint)
    root.findViewById<TextView>(R.id.yimu_template_empty_text).text = context.getString(R.string.yimu_template_empty)
    root.findViewById<ImageView>(R.id.btn_back).setOnClickListener { onBack() }
    root.findViewById<ImageView>(R.id.tip).setOnClickListener {
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.yimu_template_help)) }
    }
    root.findViewById<TextView>(R.id.right_text).setOnClickListener { onAdd() }
    root.findViewById<ImageView>(R.id.tip_close).setOnClickListener { close ->
        scope.launch {
            settings.setTemplateTipHidden(true)
            (close.parent as View).visibility = View.GONE
        }
    }
}
