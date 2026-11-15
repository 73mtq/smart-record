package com.nonen.Bookkeeping.data.migration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nonen.Bookkeeping.core.DefaultCategories
import com.nonen.Bookkeeping.data.RoomRepositoryTest
import com.nonen.Bookkeeping.data.db.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryMigrationServiceTest : RoomRepositoryTest() {
    @Test
    fun categoryMigrationPreservesBillsAndIsIdempotent() = runBlocking {
        database.categoryDao().insertAll(DefaultCategories.ALL)
        val original = listOf(
            bill(1L, -35.0, "餐饮").copy(subCategory = "早餐"),
            bill(2L, -120.0, "服饰美容"),
            bill(3L, 8000.0, "工资"),
            bill(4L, -15.0, "交通").copy(subCategory = "打车", parentCategoryId = 6, childCategoryId = 607),
        )
        database.transactionDao().insertAll(original)
        val service = CategoryMigrationService(database)

        val result = service.migrate()

        assertEquals(3, result.migratedCount)
        assertEquals(1, result.alreadyMigratedCount)
        assertEquals(4, result.totalTransactions)
        assertTrue(result.customCategoriesSeeded > 0)
        val migrated = database.transactionDao().getAll().associateBy { it.id }
        assertEquals(7 to 708, requireNotNull(migrated[1L]).let { it.parentCategoryId to it.childCategoryId })
        assertEquals(8 to 891, requireNotNull(migrated[2L]).let { it.parentCategoryId to it.childCategoryId })
        assertEquals(9 to 904, requireNotNull(migrated[3L]).let { it.parentCategoryId to it.childCategoryId })
        for (before in original) {
            val after = requireNotNull(migrated[before.id])
            assertEquals(before, after.copy(parentCategoryId = before.parentCategoryId, childCategoryId = before.childCategoryId))
        }

        val repeated = service.migrate()
        assertEquals(0, repeated.migratedCount)
        assertEquals(4, repeated.alreadyMigratedCount)
        assertEquals(0, repeated.customCategoriesSeeded)
    }

    private fun bill(id: Long, amount: Double, category: String) = TransactionEntity(
        id = id, amount = amount, category = category, timestamp = 1700000000L + id,
        source = "manual", hash = "category-migration-$id", createdAt = 100L, updatedAt = 200L,
    )
}
