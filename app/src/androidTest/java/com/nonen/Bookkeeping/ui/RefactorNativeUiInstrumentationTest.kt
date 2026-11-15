package com.nonen.Bookkeeping.ui

import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.compose.setContent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.nonen.Bookkeeping.AppContainer
import com.nonen.Bookkeeping.BookkeepingApp
import com.nonen.Bookkeeping.MainActivity
import com.nonen.Bookkeeping.R
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.ui.theme.BookkeepingTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RefactorNativeUiInstrumentationTest {
    private lateinit var activity: MainActivity
    private lateinit var container: AppContainer

    @Before
    fun openApplication() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.nonen.Bookkeeping.uivalidation")
        val device = UiDevice.getInstance(instrumentation)
        device.wakeUp()
        val output = device.executeShellCommand(
            "am start -W -f 0x10008000 -n ${context.packageName}/com.nonen.Bookkeeping.MainActivity",
        )
        check("Status: ok" in output) { output }
        check(device.wait(Until.hasObject(By.pkg(context.packageName)), 15000))
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).single() as MainActivity
            container = (activity.application as BookkeepingApp).container
        }
        awaitVisibleView(R.id.bottom_nav_view)
    }

    @After
    fun closeApplication() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { activity.finish() }
    }

    @Test
    fun nativeKeyboardAndNoteSaveABillAndUpdateItsAccount() = runBlocking {
        val note = "重构验证-${UUID.randomUUID()}"
        val originalBalance = requireNotNull(container.assetDao.findById(1L)).assetNumber
        try {
            awaitVisibleView(R.id.add_bill_btn)
            onView(withId(R.id.add_bill_btn)).perform(click())
            awaitVisibleView(R.id.remark)
            onView(withId(R.id.remark)).perform(replaceText(note), closeSoftKeyboard())
            for (key in listOf(R.id.one, R.id.two, R.id.point, R.id.three, R.id.four)) {
                onView(withId(key)).perform(click())
            }
            onView(withId(R.id.cancel)).perform(click())

            val saved = awaitBill(note)
            assertEquals(-12.34, saved.amount, 0.0)
            assertEquals("manual", saved.source)
            assertEquals(1L, saved.assetId)
            assertEquals(note, saved.note)
            assertEquals(
                originalBalance - 12.34,
                requireNotNull(container.assetDao.findById(1L)).assetNumber,
                0.000001,
            )
            awaitVisibleView(R.id.bottom_nav_view)
        } finally {
            container.transactionRepository.getAll().filter { it.note == note }.forEach {
                container.transactionRepository.delete(it)
            }
        }
        assertEquals(originalBalance, requireNotNull(container.assetDao.findById(1L)).assetNumber, 0.000001)
    }

    @Test
    fun settingsLabelsEndBeforeTheirSwitchContainers() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity.setContent {
                BookkeepingTheme(darkTheme = false) {
                    AppNavHost(startDestination = Routes.SETTINGS)
                }
            }
        }
        awaitVisibleView(R.id.row_switch_lock)
        awaitVisibleView(R.id.row_switch_gesture)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (id in listOf(R.id.row_switch_lock, R.id.row_switch_gesture)) {
                val holder = activity.findViewById<FrameLayout>(id)
                val row = holder.parent as RelativeLayout
                val label = row.getChildAt(0) as LinearLayout
                assertEquals(1, holder.childCount)
                assertTrue(holder.width > 0)
                assertTrue(label.right <= holder.left)
            }
        }
    }

    private suspend fun awaitBill(note: String): TransactionEntity = withTimeout(10000) {
        var transaction = container.transactionRepository.getAll().singleOrNull { it.note == note }
        while (transaction == null) {
            delay(25)
            transaction = container.transactionRepository.getAll().singleOrNull { it.note == note }
        }
        transaction
    }
}
