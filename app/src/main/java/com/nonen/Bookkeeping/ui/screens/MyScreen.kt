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
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val nicknameColor = context.getColor(R.color.colorPrimaryText)
    val unsupported = { Toast.makeText(context, R.string.yimu_local_unsupported, Toast.LENGTH_SHORT).show() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth()) {
            Image(painterResource(R.mipmap.bg_my_two), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            Box(Modifier.padding(start = 15.dp, end = 15.dp, top = 83.dp, bottom = 15.dp).fillMaxWidth().aspectRatio(980f / 376f)) {
                Image(painterResource(R.mipmap.bg_my_one), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                AndroidView(modifier = Modifier.align(Alignment.Center).fillMaxWidth(), factory = { nativeContext ->
                    val density = nativeContext.resources.displayMetrics.density
                    LinearLayout(nativeContext).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(0, (30 * density).toInt(), 0, 0)
                        addView(TextView(nativeContext).apply { id = R.id.nick_name; setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f); setTypeface(typeface, Typeface.BOLD) },
                            LinearLayout.LayoutParams(-2, -2))
                        addView(TextView(nativeContext).apply { id = R.id.record_day_num; gravity = Gravity.CENTER; setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f); letterSpacing = 0.2f; setLineSpacing(3 * density, 1f) },
                            LinearLayout.LayoutParams(-1, -2).apply { topMargin = (10 * density).toInt(); marginStart = (20 * density).toInt(); marginEnd = (20 * density).toInt() })
                    }
                }, update = { layout ->
                    (layout.getChildAt(0) as TextView).apply { text = nickname; setTextColor(nicknameColor) }
                    (layout.getChildAt(1) as TextView).apply { text = recordText; setTextColor(textColor) }
                })
            }
            Image(
                painterResource(R.mipmap.ic_launcher_no_color), null,
                Modifier.padding(top = 50.dp).size(70.dp).clip(CircleShape).align(Alignment.TopCenter),
            )
            AndroidView(
                modifier = Modifier.padding(top = 104.dp, start = 52.dp).size(18.dp).align(Alignment.TopCenter),
                factory = { nativeContext -> ImageView(nativeContext).apply {
                    id = R.id.vip_icon
                    setImageResource(R.mipmap.ic_vip)
                    scaleType = ImageView.ScaleType.FIT_XY
                } },
            )
        }
        Column(Modifier.padding(horizontal = 15.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface).padding(vertical = 25.dp)) {
            Row(Modifier.fillMaxWidth()) {
                MyGridItem(R.string.yimu_auto, R.mipmap.ic_auto, Modifier.weight(1f), unsupported)
                MyGridItem(R.string.yimu_cycle, R.mipmap.ic_cycle, Modifier.weight(1f), onPeriodic)
                MyGridItem(R.string.yimu_dream, R.mipmap.ic_dream_list, Modifier.weight(1f), onDream)
                MyGridItem(R.string.yimu_keyword, R.mipmap.ic_ai_type, Modifier.weight(1f), onRules)
            }
            Row(Modifier.fillMaxWidth().padding(top = 25.dp)) {
                MyGridItem(R.string.yimu_book, R.mipmap.ic_book_manager, Modifier.weight(1f), onBook)
                MyGridItem(R.string.yimu_tag, R.mipmap.ic_tag_manager, Modifier.weight(1f), unsupported)
                MyGridItem(R.string.yimu_category, R.mipmap.ic_category_manager, Modifier.weight(1f), onCategory)
                MyGridItem(R.string.yimu_settings, R.mipmap.ic_setting_manager, Modifier.weight(1f), onSettings)
            }
        }
        Spacer(Modifier.height(15.dp))
        MyGroup(R.string.yimu_data_management) {
            MyAction(R.string.yimu_cloud_sync, R.mipmap.ic_remote, unsupported, R.string.yimu_local_data, Color(0xFFF44336))
            MyAction(R.string.yimu_data_backup, R.mipmap.ic_backup, onBackup)
            MyAction(R.string.yimu_import_export, R.mipmap.ic_import_manager, onImportExport)
        }
        Spacer(Modifier.height(15.dp))
        MyGroup(R.string.yimu_other) {
            MyAction(R.string.yimu_theme, R.mipmap.ic_user_theme, onAppearance)
            MyAction(R.string.yimu_widget, R.mipmap.ic_widget, unsupported)
            MyAction(R.string.yimu_manual, R.mipmap.ic_user_guide, unsupported)
            MyAction(R.string.yimu_invite, R.mipmap.ic_invitation, unsupported)
            MyAction(R.string.yimu_feedback, R.mipmap.ic_my_feedback, unsupported)
            MyAction(R.string.yimu_rate, R.mipmap.ic_my_evaluate, unsupported)
            MyAction(R.string.yimu_about, R.mipmap.ic_my_about, onSettings)
        }
        Spacer(Modifier.height(15.dp))
    }
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
