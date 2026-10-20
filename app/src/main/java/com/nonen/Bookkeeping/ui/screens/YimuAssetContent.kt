package com.nonen.Bookkeeping.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.ui.theme.YimuTokens
import com.nonen.Bookkeeping.ui.components.YimuAssetNativeView
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YimuAssetContent(vm: AssetManageViewModel) {
    val assets by vm.displayAssets.collectAsState()
    val summary by vm.summary.collectAsState()
    val hidden by vm.showHidden.collectAsState()
    var menu by remember { mutableStateOf(false) }
    var assetMenu by remember { mutableStateOf<AssetEntity?>(null) }
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize().statusBarsPadding().semantics { testTagsAsResourceId = true },
        factory = { ctx -> YimuAssetNativeView(ctx) },
        update = { view ->
            view.render(
                netAsset = summary.netAsset,
                totalAsset = summary.totalAsset,
                totalLiability = summary.totalLiability,
                assets = assets,
                onAdd = vm::openAddDialog,
                onSettings = { menu = true },
                onAssetClick = { vm.openEditDialog(it) },
                onAssetLongClick = { assetMenu = it },
            )
        },
    )
    if (menu) {
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text(if (hidden) "显示普通账户" else "显示隐藏账户") }, onClick = { vm.setShowHidden(!hidden); menu = false })
            DropdownMenuItem(text = { Text("转账") }, onClick = { vm.openTransferDialog(); menu = false })
        }
    }
    assetMenu?.let { asset ->
        AlertDialog(onDismissRequest = { assetMenu = null }, title = { Text(asset.assetName) },
            text = { Column {
                TextButton(onClick = { assetMenu = null; vm.openAdjustDialog(asset) }) { Text("余额校准") }
                TextButton(onClick = { assetMenu = null; vm.toggleHide(asset) }) { Text(if (asset.hide) "显示账户" else "隐藏账户") }
                TextButton(onClick = { assetMenu = null; vm.moveUp(asset.assetId) }) { Text("向上移动") }
                TextButton(onClick = { assetMenu = null; vm.moveDown(asset.assetId) }) { Text("向下移动") }
                TextButton(onClick = { assetMenu = null; vm.openDeleteDialog(asset) }) { Text("删除账户") }
            } }, confirmButton = { TextButton(onClick = { assetMenu = null }) { Text("取消") } })
    }
}

private fun assetMoney(value: Double) = String.format(Locale.US, "%,.2f", value)
