package com.nonen.Bookkeeping.ui

import android.os.SystemClock
import android.util.Base64
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Until
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class YimuCaptureInstrumentationTest {
    @Test(timeout = 60000)
    fun captureScreenThroughRealControls() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(instrumentation.targetContext.packageName == "com.nonen.Bookkeeping.uivalidation")
        val config = JSONObject(String(Base64.decode(
            requireNotNull(InstrumentationRegistry.getArguments().getString("capture_config")), Base64.DEFAULT,
        ), Charsets.UTF_8))
        val packageName = config.getString("package")
        require(packageName in setOf("com.wangc.bill", "com.nonen.Bookkeeping.uivalidation", "com.android.intentresolver",
            "bin.mt.plus.canary"))
        val screen = config.getString("id")
        require(screen.matches(Regex("[a-z0-9_]+")))
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "yimu-capture/$screen")
        check(directory.mkdirs() || directory.isDirectory)
        val device = UiDevice.getInstance(instrumentation)
        val serviceInfo = instrumentation.uiAutomation.serviceInfo
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        instrumentation.uiAutomation.serviceInfo = serviceInfo
        Configurator.getInstance().setWaitForIdleTimeout(1000).setWaitForSelectorTimeout(3000)
        val actions = config.getJSONArray("actions")
        for (index in 0 until actions.length()) {
            val action = actions.getJSONObject(index)
            println("YIMU_CAPTURE_ACTION $screen $index ${action.getString("type")}")
            when (action.getString("type")) {
                "launch" -> {
                    if (config.optBoolean("prelaunched", false)) continue
                    instrumentation.runOnMainSync {
                        ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED)
                            .forEach { it.finish() }
                    }
                    SystemClock.sleep(500)
                    val result = device.executeShellCommand(
                        "am start -f 0x10008000 -n $packageName/${config.getString("activity")}",
                    )
                    check("Starting:" in result && "Error" !in result) { result }
                    check(device.wait(Until.hasObject(By.pkg(packageName)), 15000)) { "目标应用未显示：$packageName" }
                }
                "wait" -> SystemClock.sleep(action.optLong("ms", 800))
                "tap" -> {
                    var selector = By.pkg(packageName)
                    if (action.has("id")) selector = selector.res(Pattern.compile("(?:.*:id/)?${Pattern.quote(action.getString("id"))}"))
                    if (action.has("text")) selector = selector.text(action.getString("text"))
                    if (action.has("desc")) selector = selector.desc(action.getString("desc"))
                    var objects = device.findObjects(selector)
                    val wanted = action.optInt("index", 0)
                    if (objects.size <= wanted && !action.optBoolean("optional", false) && action.optBoolean("scroll", true)) {
                        repeat(3) {
                            if (objects.size <= wanted) {
                                device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                                    device.displayWidth / 2, device.displayHeight * 35 / 100, 20)
                                objects = device.findObjects(selector)
                            }
                        }
                    }
                    if (objects.size > wanted) objects[wanted].click()
                    else check(action.optBoolean("optional", false)) { "找不到控件：$action" }
                }
                "swipe" -> {
                    val from = action.getJSONArray("from")
                    val to = action.getJSONArray("to")
                    device.swipe((from.getDouble(0) * device.displayWidth).toInt(),
                        (from.getDouble(1) * device.displayHeight).toInt(),
                        (to.getDouble(0) * device.displayWidth).toInt(),
                        (to.getDouble(1) * device.displayHeight).toInt(),
                        (action.optInt("duration_ms", 400) / 5).coerceAtLeast(1))
                }
                "long_press" -> {
                    var selector = By.pkg(packageName)
                    if (action.has("id")) selector = selector.res(Pattern.compile("(?:.*:id/)?${Pattern.quote(action.getString("id"))}"))
                    if (action.has("text")) selector = selector.text(action.getString("text"))
                    if (action.has("desc")) selector = selector.desc(action.getString("desc"))
                    val objects = device.findObjects(selector)
                    val wanted = action.optInt("index", 0)
                    if (objects.size <= wanted) {
                        device.takeScreenshot(File(directory, "_long_press_debug.png"))
                        device.dumpWindowHierarchy(File(directory, "_long_press_debug.xml"))
                        error("找不到长按控件：$action")
                    }
                    objects[wanted].longClick()
                }
                "accessibility_click" -> {
                    val root = instrumentation.uiAutomation.rootInActiveWindow
                    checkNotNull(root) { "没有活动窗口：$action" }
                    val nodes = when {
                        action.has("id") -> root.findAccessibilityNodeInfosByViewId(
                            "$packageName:id/${action.getString("id")}",
                        )
                        action.has("text") -> root.findAccessibilityNodeInfosByText(action.getString("text"))
                        else -> emptyList()
                    }
                    val wanted = action.optInt("index", 0)
                    check(nodes.size > wanted) { "找不到无障碍控件：$action" }
                    var node: AccessibilityNodeInfo? = nodes[wanted]
                    var clickable: AccessibilityNodeInfo? = null
                    while (node != null) {
                        if (node.isClickable) {
                            clickable = node
                            break
                        }
                        node = node.parent
                    }
                    checkNotNull(clickable) { "控件不可点击：$action" }
                    check(clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "无障碍点击失败：$action" }
                }
                "scroll_to_offset" -> {
                    val id = action.getString("id")
                    val offset = action.optInt("offset", 0)
                    var scrolled = false
                    instrumentation.runOnMainSync {
                        val activity = ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED)
                            .firstOrNull()
                        val resourceId = activity?.resources?.getIdentifier(id, "id", packageName) ?: 0
                        val view = activity?.window?.decorView?.findViewById<View>(resourceId)
                        if (view is RecyclerView) {
                            val manager = view.layoutManager as? LinearLayoutManager
                            val position = manager?.findFirstVisibleItemPosition() ?: -1
                            if (manager != null && position >= 0) {
                                view.stopScroll()
                                manager.scrollToPositionWithOffset(position, offset)
                                scrolled = true
                            }
                        }
                    }
                    check(scrolled) { "找不到可滚动控件：$action" }
                }
                "back" -> device.pressBack()
                "key" -> device.executeShellCommand("input keyevent ${action.getString("name")}")
                "capture" -> {
                    device.takeScreenshot(File(directory, "_precheck.png"))
                    device.dumpWindowHierarchy(File(directory, "_precheck.xml"))
                    println("YIMU_CAPTURE_FOREGROUND $screen ${device.currentPackageName}")
                    check(device.currentPackageName == packageName) { "当前页面：${device.currentPackageName}" }
                    val required = config.optJSONArray("required_ids")
                    check(required != null && required.length() > 0) { "必要控件清单为空：$screen" }
                    for (requiredIndex in 0 until required.length()) {
                        val id = required.getString(requiredIndex)
                        check(device.wait(
                            Until.hasObject(By.pkg(packageName).res(Pattern.compile("(?:.*:id/)?${Pattern.quote(id)}"))),
                            3000,
                        )) {
                            "页面缺少必要控件：$screen $id"
                        }
                    }
                    check(device.takeScreenshot(File(directory, "first.png")))
                    SystemClock.sleep(300)
                    check(device.takeScreenshot(File(directory, "second.png")))
                    device.dumpWindowHierarchy(File(directory, "window.xml"))
                }
                else -> error("未知操作：$action")
            }
        }
    }
}
