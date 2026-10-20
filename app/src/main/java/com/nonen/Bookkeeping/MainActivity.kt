package com.nonen.Bookkeeping

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nonen.Bookkeeping.data.prefs.ThemeMode
import com.nonen.Bookkeeping.ui.AppNavHost
import com.nonen.Bookkeeping.ui.Routes
import com.nonen.Bookkeeping.ui.theme.BookkeepingTheme

class MainActivity : ComponentActivity() {
    private var targetRoute by mutableStateOf(Routes.MAIN)

    private var appUnlockedState = mutableStateOf(false)
    private var isAppInForeground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetRoute = resolveShortcutRoute(intent)

        setContent {
            val app = application as BookkeepingApp
            val mode by app.container.settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val appLockEnabled by app.container.settings.appLockEnabled.collectAsState(initial = false)

            val darkTheme = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // 若未开启应用锁，自动视为已解锁
            val isUnlocked = if (!appLockEnabled) true else appUnlockedState.value

            val authLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    appUnlockedState.value = true
                }
            }

            @Suppress("DEPRECATION")
            fun requestUnlock() {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (keyguardManager.isDeviceSecure) {
                    val authIntent = keyguardManager.createConfirmDeviceCredentialIntent(
                        getString(R.string.app_name),
                        "请验证设备密码或指纹以解锁记账本",
                    )
                    if (authIntent != null) {
                        authLauncher.launch(authIntent)
                    } else {
                        appUnlockedState.value = true
                    }
                } else {
                    // 设备未设密码，自动通过
                    appUnlockedState.value = true
                }
            }

            LaunchedEffect(appLockEnabled) {
                if (appLockEnabled && !appUnlockedState.value) {
                    requestUnlock()
                }
            }

            BookkeepingTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (isUnlocked) {
                        AppNavHost(startDestination = targetRoute)
                    } else {
                        LockScreen(onUnlockClick = { requestUnlock() })
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
        // 离开应用切到后台后，重置解锁状态
        val app = application as? BookkeepingApp
        if (app != null) {
            // 切后台重置解锁状态，下次切回前台必须重新校验
            appUnlockedState.value = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetRoute = resolveShortcutRoute(intent)
    }

    private fun resolveShortcutRoute(intent: Intent?): String {
        val target = intent?.getStringExtra("shortcut_target")
            ?: intent?.data?.getQueryParameter("target")
        return when (target) {
            "record" -> Routes.add(0L)
            "asset_manage" -> Routes.mainTab(2)
            "calendar" -> Routes.mainTab(1)
            "budget" -> Routes.BUDGET
            "periodic" -> Routes.PERIODIC
            else -> Routes.MAIN
        }
    }
}

@Composable
private fun LockScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "应用已锁定",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "记账本已锁定",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "为保护您的财务数据隐私，请使用设备凭据解锁",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onUnlockClick) {
                Text("点击解锁")
            }
        }
    }
}
