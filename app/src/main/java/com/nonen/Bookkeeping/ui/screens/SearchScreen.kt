package com.nonen.Bookkeeping.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SearchScreen(vm: SearchViewModel, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val context = LocalContext.current
    AndroidView(
        // 原版 BillSearchActivity 非 edge-to-edge：search_input 实测 [163,140][1020,303]，工具栏从状态栏下开始
        modifier = Modifier.fillMaxSize().statusBarsPadding().semantics { testTagsAsResourceId = true },
        factory = { viewContext ->
            com.nonen.Bookkeeping.ui.components.YimuSearchNativeView(viewContext).apply {
                bind(object : com.nonen.Bookkeeping.ui.components.YimuSearchNativeView.Listener {
                    override fun onBack() = onBack()
                    override fun onFilter() {}
                })
                // 采集时需与原版一致弹出 IME（原版 BillSearchActivity adjustResize + 自动聚焦）
                post {
                    searchInput.requestFocus()
                    val imm = viewContext.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }
        },
        update = { view -> view.storeHistory(vm.searchHistory) },
    )
}
