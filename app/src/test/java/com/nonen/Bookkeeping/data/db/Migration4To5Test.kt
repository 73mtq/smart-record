package com.nonen.Bookkeeping.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Migration4To5Test {

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
