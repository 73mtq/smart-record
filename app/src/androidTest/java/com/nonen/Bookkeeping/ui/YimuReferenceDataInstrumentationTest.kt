package com.nonen.Bookkeeping.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nonen.Bookkeeping.BookkeepingApp
import com.nonen.Bookkeeping.parse.BackupExcelParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToLong

@RunWith(AndroidJUnit4::class)
class YimuReferenceDataInstrumentationTest {
    @Test
    fun prepareReferenceFromActualExport() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.nonen.Bookkeeping.uivalidation")
        val expectedHash = requireNotNull(InstrumentationRegistry.getArguments().getString("export_sha256"))
        val expectedCount = requireNotNull(InstrumentationRegistry.getArguments().getString("export_count")).toInt()
        val expectedExpense = requireNotNull(InstrumentationRegistry.getArguments().getString("export_expense_cents")).toLong()
        val expectedIncome = requireNotNull(InstrumentationRegistry.getArguments().getString("export_income_cents")).toLong()
        val file = File(context.getExternalFilesDir(null), "yimu-current-reference.xls")
        val bytes = file.readBytes()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(hash == expectedHash)
        val container = (context.applicationContext as BookkeepingApp).container
        check(container.assetDao.getAll().isEmpty())
        val books = container.bookDao.getAll()
        check(books.size == 1 && books.single().bookName == "日常账本")
        val categories = container.categoryDao.getAll()
        check(categories.size == 78)
        val rows = requireNotNull(BackupExcelParser.parse(bytes))
        assertEquals(expectedCount, rows.size)
        val categoryMap = categories.associate { it.categoryName to
            (if (it.parentId == 0) it.categoryId to null else it.parentId to it.categoryId) }
        val entities = rows.map { row -> requireNotNull(BackupExcelParser.toEntity(row,
            bookNameToId = books.associate { it.bookName to it.bookId }, categoryNameToIds = categoryMap)) }
        container.transactionDao.clearAll()
        entities.forEach { assertTrue(container.transactionRepository.insertIfNew(it)) }
        val restored = container.transactionDao.getAll()
        assertEquals(expectedCount, restored.size)
        assertEquals(expectedExpense, restored.filter { it.amount < 0 }.sumOf { (it.amount * 100).roundToLong() })
        assertEquals(expectedIncome, restored.filter { it.amount > 0 }.sumOf { (it.amount * 100).roundToLong() })
        println("YIMU_REFERENCE_DATA count=$expectedCount expense_cents=$expectedExpense income_cents=$expectedIncome source=$hash")
    }
}
