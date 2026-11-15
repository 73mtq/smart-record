package com.nonen.Bookkeeping.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.prefs.ThemeMode
import com.nonen.Bookkeeping.ui.components.YimuThemeNativeView

/** 主题外观页：原版 ThemeActivity（从「我的-主题外观」进入）。 */
@Composable
fun YimuThemeScreen(onBack: () -> Unit, onThemeMode: (ThemeMode) -> Unit = {}) {
    val density = LocalDensity.current
    val themeModeCallback = onThemeMode
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val navigationBarPx = WindowInsets.navigationBars.getBottom(density)
    AndroidView(
        modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        factory = { viewContext ->
            YimuThemeNativeView(viewContext).apply {
                bind(object : YimuThemeNativeView.Listener {
                    override fun onBack() = onBack()

                    override fun onUnsupported(action: String) {
                        Toast.makeText(viewContext, R.string.yimu_local_unsupported, Toast.LENGTH_SHORT).show()
                    }

                    override fun onThemeMode(mode: ThemeMode) = themeModeCallback(mode)
                })
            }
        },
        update = { view ->
            view.applyToolbarInset(statusBarPx)
            view.applyNavigationInset(navigationBarPx)
        },
    )
}
