package com.nonen.Bookkeeping.ui

import android.view.View
import android.view.ViewGroup
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import android.os.ParcelFileDescriptor
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nonen.Bookkeeping.MainActivity
import com.nonen.Bookkeeping.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YimuNavigationInstrumentationTest {
    @Test(timeout = 120000) fun fiveNativeTabsOpenTheirActualPages() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(instrumentation.targetContext.packageName == "com.nonen.Bookkeeping.uivalidation")
        val output = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "am start -W -n com.nonen.Bookkeeping.uivalidation/com.nonen.Bookkeeping.MainActivity",
        )).bufferedReader().use { it.readText() }
        check("Status: ok" in output) { output }
        awaitText("添加一条新记账")
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single() as MainActivity
            val bar = requireNotNull(activity.findViewById<ViewGroup>(R.id.bottom_nav_view))
            assertTrue(bar.isShown)
        }
        assertTrue("底栏需要出现在辅助功能层级", hasResourceId(instrumentation.uiAutomation.rootInActiveWindow, "tab_my"))
        onView(withId(R.id.tab_my)).perform(click())
        awaitText("我爱记账")
        onView(withId(R.id.tab_statistics)).perform(click())
        awaitText("近7日统计")
        onView(withId(R.id.tab_asset)).perform(click())
        awaitText("净资产(元)")
        onView(withId(R.id.tab_calendar)).perform(click())
        awaitText("月/周")
        onView(withId(R.id.tab_main)).perform(click())
        awaitText("添加一条新记账")
    }

    private fun awaitText(text: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < deadline) {
            val root = automation.rootInActiveWindow
            if (root?.packageName == "com.nonen.Bookkeeping.uivalidation" && hasText(root, text)) return
            SystemClock.sleep(100)
        }
        throw AssertionError("页面文字未出现：$text")
    }

    private fun hasText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.text?.toString() == text) return true
        return (0 until node.childCount).any { hasText(node.getChild(it), text) }
    }

    private fun hasResourceId(node: AccessibilityNodeInfo?, id: String): Boolean {
        if (node == null) return false
        if (node.viewIdResourceName?.endsWith(":id/$id") == true) return true
        return (0 until node.childCount).any { hasResourceId(node.getChild(it), id) }
    }
}
