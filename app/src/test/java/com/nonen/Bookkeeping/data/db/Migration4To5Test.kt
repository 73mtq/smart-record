package com.nonen.Bookkeeping.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration4To5Test {

    @Test
    fun `migration 4 to 5 adds hideBook column to categories table`() {
        val executedSqls = mutableListOf<String>()

        val handler = InvocationHandler { _: Any?, method: Method, args: Array<out Any>? ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSqls.add(args[0] as String)
            }
            null
        }

        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler,
        ) as SupportSQLiteDatabase

        AppDatabase.MIGRATION_4_5.migrate(fakeDb)

        assertTrue(
            "Expected ALTER TABLE categories ADD COLUMN hideBook",
            executedSqls.any { it.contains("ALTER TABLE `categories` ADD COLUMN `hideBook` TEXT") },
        )
    }

    @Test
    fun `CategoryEntity isHiddenInBook respects global hide and book-specific hideBook`() {
        val normalCategory = CategoryEntity(
            categoryId = 708,
            parentId = 7,
            categoryName = "早餐",
            iconUrl = "ic_restaurant_breakfast",
            positionWeight = 708,
            categoryType = 0,
            hide = false,
            hideBook = null,
        )
        assertFalse(normalCategory.isHiddenInBook(1L))
        assertFalse(normalCategory.isHiddenInBook(2L))

        val globalHiddenCategory = normalCategory.copy(hide = true)
        assertTrue(globalHiddenCategory.isHiddenInBook(1L))
        assertTrue(globalHiddenCategory.isHiddenInBook(2L))

        val bookHiddenCategory = normalCategory.copy(hideBook = "1,2")
        assertTrue(bookHiddenCategory.isHiddenInBook(1L))
        assertTrue(bookHiddenCategory.isHiddenInBook(2L))
        assertFalse(bookHiddenCategory.isHiddenInBook(3L))
    }

    @Test
    fun `CategoryEntity withBookHidden correctly modifies hideBook list`() {
        val cat = CategoryEntity(
            categoryId = 708,
            parentId = 7,
            categoryName = "早餐",
            iconUrl = "ic_restaurant_breakfast",
            positionWeight = 708,
            categoryType = 0,
            hide = false,
            hideBook = null,
        )

        val hiddenInBook1 = cat.withBookHidden(1L, true)
        assertEquals("1", hiddenInBook1.hideBook)
        assertTrue(hiddenInBook1.isHiddenInBook(1L))

        val hiddenInBook1And2 = hiddenInBook1.withBookHidden(2L, true)
        assertTrue(hiddenInBook1And2.isHiddenInBook(1L))
        assertTrue(hiddenInBook1And2.isHiddenInBook(2L))

        val unhiddenInBook1 = hiddenInBook1And2.withBookHidden(1L, false)
        assertFalse(unhiddenInBook1.isHiddenInBook(1L))
        assertTrue(unhiddenInBook1.isHiddenInBook(2L))
        assertEquals("2", unhiddenInBook1.hideBook)

        val unhiddenAll = unhiddenInBook1.withBookHidden(2L, false)
        assertFalse(unhiddenAll.isHiddenInBook(2L))
        assertEquals(null, unhiddenAll.hideBook)
    }
}
