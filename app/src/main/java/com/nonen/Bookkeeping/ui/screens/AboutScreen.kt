package com.nonen.Bookkeeping.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonen.Bookkeeping.R

/** 关于我们页：原版 AboutUsActivity（activity_ablout_us.xml）。 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenDocument: (document: String, title: String) -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }
    val copy = { label: String, value: String ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "已复制$label", Toast.LENGTH_SHORT).show()
    }
    val openUrl = { url: String ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "没有可打开该链接的应用", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        AboutToolbar(onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.mipmap.yimu_logo_launcher),
                contentDescription = "应用图标",
                modifier = Modifier.padding(top = 20.dp).size(100.dp),
            )
            Spacer(Modifier.height(20.dp))
            AboutGroup {
                AboutRow("小红书", "一木宇宙", height = 60.dp) { copy("小红书账号", "一木宇宙") }
                AboutRow("邮箱", "yimuyuzhou@163.com") { copy("邮箱", "yimuyuzhou@163.com") }
                AboutRow("QQ群", "824186564") { copy("QQ群", "824186564") }
                AboutRow("官网", "www.yimuapp.com") { openUrl("https://www.yimuapp.com") }
                AboutRow("公众号", "一木宇宙") { copy("公众号", "一木宇宙") }
            }
            Spacer(Modifier.height(15.dp))
            AboutGroup {
                AboutRow("软件版本", "v$versionName")
                AboutRow("备案号", "浙ICP备2022020351号-3A", arrow = true) {
                    copy("备案号", "浙ICP备2022020351号-3A")
                    openUrl("https://beian.miit.gov.cn")
                }
                AboutRow("用户协议", arrow = true) { onOpenDocument("user_agreement.html", "用户协议") }
                AboutRow("隐私政策", arrow = true) { onOpenDocument("privacy_policy.html", "隐私政策") }
                AboutRow("个人信息收集清单", arrow = true) { onOpenDocument("personal_info.html", "个人信息收集清单") }
                AboutRow("第三方共享个人信息清单", arrow = true) { onOpenDocument("third_party.html", "第三方共享个人信息清单") }
                AboutRow("开源协议", arrow = true) { onOpenDocument("open_source.html", "开源协议") }
            }
            Spacer(Modifier.height(30.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                Text("我们的应用", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(10.dp))
                AppRecommendCard(
                    name = "一木记物",
                    description = "全品类物品生活管家。数码家电、日常用品、首饰、会员订阅轻松记录。保修期、保质期、有效期到期提醒",
                )
                Spacer(Modifier.height(10.dp))
                AppRecommendCard(
                    name = "一木清单",
                    description = "智能待办清单，制定待办事项、出行计划、重要笔记、习惯打卡，使用番茄钟和专注计时，轻松规划每一天",
                )
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun AboutToolbar(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回", Modifier.size(22.dp))
        }
        Text("关于我们", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AboutGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp)
            .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

@Composable
private fun AboutRow(
    title: String,
    value: String? = null,
    height: androidx.compose.ui.unit.Dp = 50.dp,
    arrow: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(height)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        if (value != null) {
            Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.width(5.dp))
        }
        if (arrow) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppRecommendCard(name: String, description: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.mipmap.yimu_logo_launcher),
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
            )
        }
    }
}
