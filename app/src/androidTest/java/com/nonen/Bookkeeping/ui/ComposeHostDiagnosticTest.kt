package com.nonen.Bookkeeping.ui

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeHostDiagnosticTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test(timeout = 60000)
    fun existingActivityHostsComposeContent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.nonen.Bookkeeping.uivalidation")
        val device = UiDevice.getInstance(instrumentation)
        val started = device.executeShellCommand(
            "am start -f 0x10008000 -n ${context.packageName}/com.nonen.Bookkeeping.MainActivity",
        )
        check("Starting:" in started && "Error" !in started) { started }
        check(device.wait(Until.hasObject(By.pkg(context.packageName)), 15000)) { "主界面未出现" }

        var activity: Activity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull { it is ComponentActivity }
        }
        val host = requireNotNull(activity) { "没有处于 RESUMED 的 ComponentActivity" } as ComponentActivity
        host.runOnUiThread {
            host.setContent { Text("S11_HOST_OK") }
        }
        compose.waitForIdle()
        compose.onNodeWithText("S11_HOST_OK").assertExists()
    }
}
