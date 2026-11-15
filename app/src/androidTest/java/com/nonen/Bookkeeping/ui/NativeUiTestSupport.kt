package com.nonen.Bookkeeping.ui

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nonen.Bookkeeping.MainActivity
import com.nonen.Bookkeeping.R

internal fun awaitVisibleView(id: Int): View {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val deadline = SystemClock.uptimeMillis() + 10000
    var stateDescription = ""
    while (SystemClock.uptimeMillis() < deadline) {
        var visibleView: View? = null
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).single() as MainActivity
            val view = activity.findViewById<View>(id)
            val navigation = activity.findViewById<BottomNavigationView>(R.id.bottom_nav_view)
            stateDescription = "页签=${navigation?.selectedItemId}，控件=${view?.javaClass?.simpleName}，" +
                "尺寸=${view?.width}x${view?.height}"
            if (view != null && view.isShown && view.getGlobalVisibleRect(Rect())) visibleView = view
        }
        visibleView?.let { return it }
        SystemClock.sleep(100)
    }
    throw AssertionError("页面控件未显示：$id；$stateDescription")
}
