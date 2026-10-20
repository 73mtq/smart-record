package com.nonen.Bookkeeping.ui

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.BookEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.ui.screens.AddEditViewModel
import com.nonen.Bookkeeping.ui.screens.YimuAddEditScreen
import com.nonen.Bookkeeping.ui.theme.BookkeepingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class YimuAddEditUiInstrumentationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context
    private lateinit var source: File
    private lateinit var sourceHash: String
    private lateinit var databaseName: String
    private lateinit var database: AppDatabase
    private lateinit var transactions: TransactionRepository
    private lateinit var categories: CategoryRepository
    private lateinit var assets: AssetRepository
    private lateinit var books: List<BookEntity>
    private lateinit var scope: CoroutineScope
    private lateinit var vm: AddEditViewModel
    private val backEvents = AtomicInteger()

    @Before
    fun setUp() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.nonen.Bookkeeping.uivalidation")
        source = File(context.getExternalFilesDir(null), "yimu-validation-source.db")
        check(source.isFile)
        sourceHash = requireNotNull(InstrumentationRegistry.getArguments().getString("source_sha256"))
        check(sha256(source) == sourceHash)
        databaseName = "yimu-add-edit-ui-${UUID.randomUUID()}.db"
        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .createFromFile(source)
            .build()
        transactions = TransactionRepository(
            database.transactionDao(), database.categoryRuleDao(), database.merchantCategoryDao(),
            assetDao = database.assetDao(), transferDao = database.transferDao(), database = database,
        )
        categories = CategoryRepository(database.categoryDao())
        assets = AssetRepository(database.assetDao(), database.transferDao(), database.transactionDao(), database)
        books = database.bookDao().getAll()
        check(books.any { it.bookId == 1L })
        assertEquals(1970, transactions.getAll().size)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val started = device.executeShellCommand(
            "am start -f 0x10008000 -n ${context.packageName}/com.nonen.Bookkeeping.MainActivity",
        )
        check("Error" !in started) { started }
        check(device.wait(Until.hasObject(By.pkg(context.packageName)), 15000)) { "主界面未出现" }
    }

    @After
    fun tearDown() {
        if (::scope.isInitialized) {
            scope.cancel()
            runBlocking { scope.coroutineContext[Job]?.join() }
        }
        if (::database.isInitialized) database.close()
        if (::databaseName.isInitialized) check(context.deleteDatabase(databaseName))
        if (::source.isInitialized && ::sourceHash.isInitialized) assertEquals(sourceHash, sha256(source))
    }

    @Test
    fun expenseAgainRetainsCategoryAndAccountAndClearsDraftFields() {
        val assetId = runBlocking { assets.createAsset("S10界面支出账户", 1000.0) }
        openScreen()
        selectAccount("S10界面支出账户")
        selectFood("早餐")
        replaceNote("S10界面早餐")
        setDialogValue("标签", "S10界面标签")
        enterAmount("12.50")
        compose.onNodeWithTag("add_bill_again").performClick()
        compose.waitUntil(10000) { !vm.saving && vm.amount.isEmpty() && vm.note.isEmpty() && vm.tag.isEmpty() }

        compose.runOnIdle {
            assertEquals(assetId, vm.assetId)
            assertEquals("S10界面支出账户", vm.account)
            assertEquals(7, vm.parentCategoryId)
            assertEquals(708, vm.childCategoryId)
            assertEquals("食品餐饮", vm.category)
            assertEquals("早餐", vm.subCategory)
            assertEquals(1L, vm.bookId)
            assertFalse(vm.isEdit)
            assertEquals(0, backEvents.get())
        }
        compose.onNodeWithTag("cost").assertTextEquals("0.00")
        compose.onNodeWithText("S10界面早餐").assertDoesNotExist()
        compose.onNodeWithText("S10界面标签").assertDoesNotExist()
        runBlocking {
            val first = transactions.getAll().single { it.assetId == assetId }
            assertEquals(-12.50, first.amount, 0.001)
            assertEquals("EXPENSE", first.transactionType)
            assertEquals(708, first.childCategoryId)
            assertEquals("S10界面早餐", first.note)
            assertEquals("S10界面标签", first.tag)
            assertEquals(987.50, balance(assetId), 0.001)
        }

        enterAmount("7.25")
        compose.onNodeWithTag("cancel").performClick()
        awaitBack()
        runBlocking {
            val saved = transactions.getAll().filter { it.assetId == assetId }
            assertEquals(2, saved.size)
            val next = saved.single { it.amount == -7.25 }
            assertEquals(7, next.parentCategoryId)
            assertEquals(708, next.childCategoryId)
            assertNull(next.note)
            assertNull(next.tag)
            assertEquals(980.25, balance(assetId), 0.001)
            assertEquals(1972, transactions.getAll().size)
        }
    }

    @Test
    fun incomeTabSavesSalaryWithSelectedAccount() {
        val assetId = runBlocking { assets.createAsset("S10界面收入账户", 100.0) }
        openScreen()
        compose.onNodeWithText("收入").performClick()
        compose.waitUntil(10000) { vm.isIncome && vm.liveCategories.any { it.categoryId == 904 } }
        compose.onNodeWithText("工资").performScrollTo().performClick()
        selectAccount("S10界面收入账户")
        replaceNote("S10界面工资")
        enterAmount("32.75")
        compose.onNodeWithTag("cancel").performClick()
        awaitBack()
        runBlocking {
            val saved = transactions.getAll().single { it.assetId == assetId }
            assertEquals(32.75, saved.amount, 0.001)
            assertEquals("INCOME", saved.transactionType)
            assertEquals("收入", saved.category)
            assertEquals("工资", saved.subCategory)
            assertEquals(9, saved.parentCategoryId)
            assertEquals(904, saved.childCategoryId)
            assertEquals("S10界面收入账户", saved.account)
            assertEquals("S10界面工资", saved.note)
            assertEquals(132.75, balance(assetId), 0.001)
            assertEquals(1971, transactions.getAll().size)
        }
    }

    @Test
    fun transferTabSavesPrincipalFeeAndBothBalances() {
        val from = runBlocking { assets.createAsset("S10界面转出账户", 1000.0) }
        val to = runBlocking { assets.createAsset("S10界面转入账户", 100.0) }
        openScreen()
        compose.onNodeWithText("转账").performClick()
        compose.onNodeWithText("转出账户").performClick()
        clickDialogText("S10界面转出账户")
        compose.onNodeWithText("转入账户").performClick()
        clickDialogText("S10界面转入账户")
        setDialogValue("手续费", "2.50")
        replaceNote("S10界面转账")
        enterAmount("200")
        compose.onNodeWithTag("cancel").performClick()
        awaitBack()
        runBlocking {
            val saved = database.transferDao().getAll().single { it.fromAssetId == from && it.toAssetId == to }
            assertEquals(200.0, saved.cost, 0.001)
            assertEquals(200.0, saved.toCost, 0.001)
            assertEquals(2.50, saved.serviceCharge, 0.001)
            assertEquals("S10界面转账", saved.remark)
            assertEquals(797.50, balance(from), 0.001)
            assertEquals(300.0, balance(to), 0.001)
            assertEquals(1970, transactions.getAll().size)
        }
    }

    @Test
    fun editScreenUpdatesAmountCategoryAccountAndBalances() {
        val from = runBlocking { assets.createAsset("S10界面原账户", 1000.0) }
        val to = runBlocking { assets.createAsset("S10界面新账户", 500.0) }
        val original = createEditableRecord(from)
        openScreen(original.id)
        compose.onNodeWithTag("cost").assertTextEquals("25.00")
        compose.onNodeWithText("午餐").performScrollTo().performClick()
        selectAccount("S10界面新账户")
        replaceNote("S10界面编辑后")
        compose.onNodeWithTag("delete_one").performTouchInput { longClick() }
        compose.onNodeWithTag("cost").assertTextEquals("0.00")
        enterAmount("40.75")
        compose.onNodeWithTag("cancel").performClick()
        awaitBack()
        runBlocking {
            val saved = requireNotNull(transactions.getById(original.id))
            assertEquals(-40.75, saved.amount, 0.001)
            assertEquals(7, saved.parentCategoryId)
            assertEquals(707, saved.childCategoryId)
            assertEquals("午餐", saved.subCategory)
            assertEquals(to, saved.assetId)
            assertEquals("S10界面新账户", saved.account)
            assertEquals("S10界面编辑后", saved.note)
            assertEquals(original.hash, saved.hash)
            assertEquals(1000.0, balance(from), 0.001)
            assertEquals(459.25, balance(to), 0.001)
            assertEquals(1971, transactions.getAll().size)
        }
    }

    @Test
    fun editScreenDeleteConfirmationRemovesRecordAndRestoresBalance() {
        val assetId = runBlocking { assets.createAsset("S10界面删除账户", 1000.0) }
        val original = createEditableRecord(assetId)
        openScreen(original.id)
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("删除这条账单？").assertExists()
        clickDialogText("删除")
        awaitBack()
        runBlocking {
            assertNull(transactions.getById(original.id))
            assertEquals(1000.0, balance(assetId), 0.001)
            assertEquals(1970, transactions.getAll().size)
        }
    }

    private fun openScreen(txId: Long = 0L) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            vm = AddEditViewModel(transactions, categoryRepo = categories, assetRepo = assets,
                txId = txId, externalScope = scope)
        }
        val host = resumedHost()
        host.runOnUiThread {
            host.setContent {
                BookkeepingTheme(darkTheme = false) {
                    YimuAddEditScreen(vm, books,
                        onCreateBook = { error("记账测试意外打开创建账本操作") },
                        onBack = { backEvents.incrementAndGet() },
                        onManageCategories = { error("记账测试意外打开分类管理") },
                        onManageAssets = { error("记账测试意外打开账户管理") })
                }
            }
        }
        compose.waitUntil(10000) { !vm.loading && vm.liveCategories.isNotEmpty() && vm.liveAssets.isNotEmpty() }
        compose.onNodeWithTag("number_key_board").assertExists()
    }

    private fun resumedHost(): ComponentActivity {
        var activity: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull { it is ComponentActivity }
        }
        return requireNotNull(activity) { "没有处于 RESUMED 的 ComponentActivity" } as ComponentActivity
    }

    private fun selectAccount(name: String) {
        compose.waitUntil(10000) { vm.liveAssets.any { it.assetName == name } && vm.account.isNotBlank() }
        compose.onNodeWithText(vm.account).performClick()
        clickDialogText(name)
        compose.onNodeWithText(name).assertExists()
    }

    private fun selectFood(child: String) {
        compose.onNodeWithText("食品餐饮").performScrollTo().performClick()
        compose.onNodeWithText(child).performScrollTo().performClick()
    }

    private fun replaceNote(value: String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(value)
        closeSoftKeyboard()
        compose.waitForIdle()
    }

    private fun setDialogValue(label: String, value: String) {
        compose.onNodeWithText(label).performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement(value)
        closeSoftKeyboard()
        clickDialogText("保存")
    }

    private fun clickDialogText(text: String) {
        compose.onNode(hasText(text) and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
    }

    private fun enterAmount(value: String) {
        val digits = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
        value.forEach { char ->
            val tag = if (char == '.') "point" else digits[char.digitToInt()]
            compose.onNodeWithTag(tag).performClick()
        }
        compose.onNodeWithTag("cost").assertTextEquals(value)
    }

    private fun awaitBack() {
        compose.waitUntil(10000) { backEvents.get() == 1 && !vm.saving }
        compose.runOnIdle { assertNull(vm.errorMessageRes) }
    }

    private fun createEditableRecord(assetId: Long): TransactionEntity = runBlocking {
        val record = TransactionEntity(amount = -25.0, category = "食品餐饮", subCategory = "早餐",
            parentCategoryId = 7, childCategoryId = 708, assetId = assetId,
            account = requireNotNull(assets.getById(assetId)).assetName, note = "S10界面编辑前",
            timestamp = System.currentTimeMillis(), source = "validation", hash = UUID.randomUUID().toString())
        assertTrue(transactions.insertIfNew(record))
        transactions.getAll().single { it.hash == record.hash }
    }

    private suspend fun balance(assetId: Long): Double = requireNotNull(assets.getById(assetId)).assetNumber

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
