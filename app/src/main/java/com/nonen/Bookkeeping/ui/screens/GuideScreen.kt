package com.nonen.Bookkeeping.ui.screens

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 内置文档页：原版 UserGuideActivity 使用 WebView 加载在线页面，
 * 本地版本没有网络权限，改为加载 assets/yimu/legal 下的静态文档。
 */
@Composable
fun GuideScreen(document: String, title: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回", Modifier.size(22.dp))
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = true
                    settings.loadsImagesAutomatically = true
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    loadUrl("file:///android_asset/yimu/legal/$document")
                }
            },
        )
    }
}
