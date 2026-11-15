package com.nonen.Bookkeeping.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeCatalogTest {

    @Test
    fun `default theme uses reference primary color`() {
        assertEquals(0xFF48AB93.toInt(), ThemeCatalog.colorFor(ThemeCatalog.DEFAULT))
    }

    @Test
    fun `solid theme colors match reference table`() {
        assertEquals(0xFF5AC5FC.toInt(), ThemeCatalog.colorFor("simpleblue"))
        assertEquals(0xFFF04A5E.toInt(), ThemeCatalog.colorFor("simplered"))
        assertEquals(0xFF000000.toInt(), ThemeCatalog.colorFor("simpleblack"))
        assertEquals(0xFFFD823F.toInt(), ThemeCatalog.colorFor("simplesix"))
        assertEquals(0xFF347497.toInt(), ThemeCatalog.colorFor("simpleone"))
    }

    @Test
    fun `season themes carry background resources`() {
        assertNotNull(ThemeCatalog.backgroundFor("spring"))
        assertNotNull(ThemeCatalog.backgroundFor("summer"))
        assertNotNull(ThemeCatalog.backgroundFor("autumn"))
        assertNotNull(ThemeCatalog.backgroundFor("winter"))
        assertNotNull(ThemeCatalog.backgroundFor(ThemeCatalog.DEFAULT))
        assertNotNull(ThemeCatalog.backgroundFor(ThemeCatalog.NIGHT))
    }

    @Test
    fun `solid themes keep default background`() {
        assertEquals(null, ThemeCatalog.backgroundFor("simpleblue"))
    }

    @Test
    fun `unknown theme falls back to default`() {
        assertEquals(ThemeCatalog.DEFAULT_PRIMARY, ThemeCatalog.colorFor("unknown"))
        assertEquals("一木", ThemeCatalog.nameFor("unknown"))
    }
}
