package com.nonen.Bookkeeping.ui.screens

import android.widget.ImageView
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.util.TypedValue
import android.graphics.Typeface
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject
import kotlin.math.ceil
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun MyScreen(
    container: AppContainer,
    onSettings: () -> Unit,
    onCategory: () -> Unit,
    onRules: () -> Unit,
    onPeriodic: () -> Unit,
    onDream: () -> Unit,
    onTemplate: () -> Unit,
    onBudget: () -> Unit,
    onBook: () -> Unit,
    onAppearance: () -> Unit,
    onImportExport: () -> Unit,
    onBackup: () -> Unit,
) {
    val context = LocalContext.current
    val records by remember(container) {
        container.transactionRepository.observeRange(0L, Long.MAX_VALUE)
    }.collectAsState(initial = emptyList())
    val profile = remember(context) { context.assets.open("yimu-profile.json").bufferedReader().use { JSONObject(it.readText()) } }
    val days = ceil((System.currentTimeMillis() - profile.getLong("registerTime")) / 86_400_000.0).toInt().coerceAtLeast(1)
    val nickname = profile.getString("name")
    val recordText = stringResource(R.string.yimu_record_days, days, records.size)
    val unsupported = { Toast.makeText(context, R.string.yimu_local_unsupported, Toast.LENGTH_SHORT).show() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val refreshObserver = remember {
        androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // 从设置/分类等页面返回时刷新记录数（原生视图只在此重建文案）
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(refreshObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(refreshObserver) }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        factory = { viewContext -> com.nonen.Bookkeeping.ui.components.YimuMyNativeView(viewContext) },
        update = { view ->
            view.bind(nickname, recordText, showVip = true,
                listener = object : com.nonen.Bookkeeping.ui.components.YimuMyNativeView.Listener {
                    override fun onAuto() = unsupported()
                    override fun onCycle() = onPeriodic()
                    override fun onDream() = onDream()
                    override fun onRules() = onRules()
                    override fun onBook() = onBook()
                    override fun onTag() = unsupported()
                    override fun onCategory() = onCategory()
                    override fun onSettings() = onSettings()
                    override fun onCloudSync() = unsupported()
                    override fun onDataBackup() = onBackup()
                    override fun onImportExport() = onImportExport()
                    override fun onTheme() = onAppearance()
                    override fun onWidget() = unsupported()
                    override fun onGuide() = unsupported()
                    override fun onInvitation() = unsupported()
                    override fun onFeedback() = unsupported()
                    override fun onEvaluate() = unsupported()
                    override fun onAbout() = onSettings()
                })
        },
    )
}

@Composable
private fun MyGridItem(@StringRes title: Int, @DrawableRes icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        val background = when (icon) {
            R.mipmap.ic_auto -> R.mipmap.ic_auto_bg
            R.mipmap.ic_cycle -> R.mipmap.ic_cycle_bg
            R.mipmap.ic_dream_list -> R.mipmap.ic_dream_list_bg
            R.mipmap.ic_ai_type -> R.mipmap.ic_ai_type_bg
            R.mipmap.ic_book_manager -> R.mipmap.ic_book_manager_bg
            R.mipmap.ic_tag_manager -> R.mipmap.ic_tag_manager_bg
            R.mipmap.ic_category_manager -> R.mipmap.ic_category_manager_bg
            R.mipmap.ic_setting_manager -> R.mipmap.ic_setting_manager_bg
            else -> error("grid icon background is required")
        }
        Box(Modifier.size(21.dp)) {
            Icon(painterResource(background), null, Modifier.matchParentSize(), tint = MaterialTheme.colorScheme.onSurface)
            Icon(painterResource(icon), null, Modifier.matchParentSize(), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(title), fontSize = 12.sp)
    }
}

@Composable
private fun MyGroup(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 15.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)) {
        Text(stringResource(title), Modifier.padding(start = 15.dp, top = 15.dp, bottom = 5.dp),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun MyAction(@StringRes title: Int, @DrawableRes icon: Int, onClick: () -> Unit, @StringRes subtitle: Int? = null, subtitleColor: Color? = null) {
    Row(Modifier.fillMaxWidth().height(55.dp).clickable(onClick = onClick).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(15.dp))
        Text(stringResource(title), fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        if (subtitle != null) {
            Text(stringResource(subtitle), fontSize = 12.sp,
                color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(5.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
