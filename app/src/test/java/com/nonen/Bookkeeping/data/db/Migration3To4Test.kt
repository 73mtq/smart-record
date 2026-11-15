package com.nonen.Bookkeeping.data.db

import com.nonen.Bookkeeping.core.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Test

class Migration3To4Test {

    @Test
    fun `default categories has exactly 76 items with 10 parents and 66 children`() {
        val all = DefaultCategories.ALL
        assertEquals(76, all.size)

        val parents = all.filter { it.parentId == 0 }
        assertEquals(10, parents.size)

        val children = all.filter { it.parentId > 0 }
        assertEquals(66, children.size)

        val expenseParents = parents.filter { it.categoryType == 0 }
        assertEquals(9, expenseParents.size)

        val incomeParents = parents.filter { it.categoryType == 1 }
        assertEquals(1, incomeParents.size)
        assertEquals("收入", incomeParents.first().categoryName)
        assertEquals(9, incomeParents.first().categoryId)

        val incomeChildren = children.filter { it.categoryType == 1 }
        assertEquals(11, incomeChildren.size)
    }

}
