package com.nonen.Bookkeeping.ui

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.BookEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.ui.screens.AddEditViewModel
import com.nonen.Bookkeeping.ui.screens.AmountKeyboardState
import com.nonen.Bookkeeping.ui.screens.StatisticsBillFilter
import com.nonen.Bookkeeping.ui.screens.StatisticsBillListViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StatisticsBillListInstrumentationTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var databaseName: String
    private lateinit var transactions: TransactionRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.nonen.Bookkeeping.uivalidation")
        val source = File(context.getExternalFilesDir(null), "yimu-validation-source.db")
        val expectedHash = requireNotNull(InstrumentationRegistry.getArguments().getString("source_sha256"))
        val actualHash = MessageDigest.getInstance("SHA-256").digest(source.readBytes())
            .joinToString("") { "%02x".format(it) }
        check(actualHash == expectedHash)
        databaseName = "yimu-statistics-${UUID.randomUUID()}.db"
        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).createFromFile(source).build()
        transactions = TransactionRepository(
            database.transactionDao(), database.categoryRuleDao(), database.merchantCategoryDao(),
            assetDao = database.assetDao(), transferDao = database.transferDao(), database = database,
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        if (::database.isInitialized) database.close()
        if (::databaseName.isInitialized) check(context.deleteDatabase(databaseName))
    }

    @Test
    fun categoryChildAndTagListsUseBookDirectionAndInclusiveDateRange(): Unit = runBlocking {
        val bookId = database.bookDao().insert(BookEntity(bookName = "S10统计账本"))
        val allCategories = database.categoryDao().getAll()
        val parent = allCategories.first { candidate ->
            candidate.parentId == 0 && candidate.categoryType == 0 &&
                allCategories.any { it.parentId == candidate.categoryId }
        }
        val child = database.categoryDao().getChildCategories(parent.categoryId).first()
        val incomeParent = database.categoryDao().getParentCategories(1).first()
        val start = LocalDate.of(2024, 2, 29)
        val end = LocalDate.of(2024, 3, 1)
        val zone = ZoneId.systemDefault()
        val firstMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val afterMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val tag = "S10统计标签&+测试"
        val account = database.assetDao().getAll().first()
        val base = TransactionEntity(
            amount = -3.0, category = parent.categoryName, subCategory = child.categoryName,
            parentCategoryId = parent.categoryId, childCategoryId = child.categoryId,
            bookId = bookId, assetId = account.assetId, account = account.assetName, tag = tag,
            timestamp = firstMillis, source = "manual", hash = UUID.randomUUID().toString(),
        )
        val first = insert(base)
        val last = insert(base.copy(timestamp = afterMillis - 1, hash = UUID.randomUUID().toString()))
        val withoutChild = insert(base.copy(subCategory = null, childCategoryId = null,
            hash = UUID.randomUUID().toString()))
        insert(base.copy(timestamp = firstMillis - 1, hash = UUID.randomUUID().toString()))
        insert(base.copy(timestamp = afterMillis, hash = UUID.randomUUID().toString()))
        insert(base.copy(bookId = 1L, hash = UUID.randomUUID().toString()))
        val income = insert(base.copy(amount = 10.0, category = incomeParent.categoryName,
            parentCategoryId = incomeParent.categoryId, subCategory = null, childCategoryId = null,
            transactionType = "INCOME", hash = UUID.randomUUID().toString()))
        val filter = StatisticsBillFilter(bookId, start, end, false, category = parent.categoryName)
        assertRecords(filter, setOf(first.id, last.id, withoutChild.id))
        assertRecords(filter.copy(subCategory = child.categoryName), setOf(first.id, last.id))
        assertRecords(filter.copy(subCategory = ""), setOf(withoutChild.id))
        assertRecords(filter.copy(category = null, tag = tag), setOf(first.id, last.id, withoutChild.id))
        assertRecords(filter.copy(category = null, tag = tag, income = true), setOf(income.id))
        val uri = Uri.parse(Routes.statisticsBills(filter.copy(tag = tag, subCategory = child.categoryName)))
        assertEquals(parent.categoryName, uri.getQueryParameter("category"))
        assertEquals(child.categoryName, uri.getQueryParameter("subCategory"))
        assertEquals(tag, uri.getQueryParameter("tag"))
    }

    @Test
    fun editingActualListRecordUpdatesTheObservedAmountAndDeletingRemovesIt(): Unit = runBlocking {
        val original = transactions.getAll().first { it.amount < 0.0 }
        val date = java.time.Instant.ofEpochMilli(original.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val filter = StatisticsBillFilter(original.bookId, date, date, false, category = original.category)
        val list = StatisticsBillListViewModel(transactions, filter, scope)
        withTimeout(10_000) { list.transactions.first { records -> records.any { it.id == original.id } } }
        val editor = withContext(Dispatchers.Main) {
            AddEditViewModel(
                repo = transactions, categoryRepo = CategoryRepository(database.categoryDao()),
                assetRepo = AssetRepository(database.assetDao(), database.transferDao(), database.transactionDao(), database),
                txId = original.id, externalScope = scope,
            )
        }
        withTimeout(10_000) { while (editor.loading) delay(20) }
        val done = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) {
            editor.updateKeyboard(AmountKeyboardState("2.37"))
            editor.save { done.complete(Unit) }
        }
        withTimeout(10_000) { done.await() }
        val updated = withTimeout(10_000) {
            list.transactions.first { records -> records.any { it.id == original.id && it.amount == -2.37 } }
        }.single { it.id == original.id }
        assertEquals(-2.37, updated.amount, 0.001)
        transactions.delete(updated)
        withTimeout(10_000) { list.transactions.first { records -> records.none { it.id == original.id } } }
        Unit
    }

    private suspend fun insert(record: TransactionEntity): TransactionEntity {
        assertTrue(transactions.insertIfNew(record))
        return transactions.getAll().single { it.hash == record.hash }
    }

    private suspend fun assertRecords(filter: StatisticsBillFilter, expectedIds: Set<Long>) {
        val vm = StatisticsBillListViewModel(transactions, filter, scope)
        val records = withTimeout(10_000) { vm.transactions.first { entries -> entries.isNotEmpty() } }
        assertEquals(expectedIds, records.map { it.id }.toSet())
    }
}
