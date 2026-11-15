package com.nonen.Bookkeeping.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Before
    fun checkValidationPackage() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName == "com.nonen.Bookkeeping.uivalidation")
    }

    @Test
    fun versionThreeFinancialMigrationPreservesTheOriginalBill() {
        helper.createDatabase("migration-financial", 3).use { database ->
            database.execSQL(
                "INSERT INTO transactions (id, amount, category, timestamp, source, transactionType, hash, createdAt, updatedAt) " +
                    "VALUES (1, -35.0, '餐饮', 1700000000, 'manual', 'EXPENSE', 'migration-bill', 100, 200)",
            )
        }

        helper.runMigrationsAndValidate("migration-financial", 4, true, AppDatabase.MIGRATION_3_4).use { database ->
            database.query("SELECT amount, hash, assetId, bookId FROM transactions WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(-35.0, cursor.getDouble(0), 0.0)
                assertEquals("migration-bill", cursor.getString(1))
                assertEquals(1L, cursor.getLong(2))
                assertEquals(1L, cursor.getLong(3))
            }
            database.query("SELECT COUNT(*) FROM categories").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(76, cursor.getInt(0))
            }
            database.query("SELECT bookName FROM books WHERE bookId = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("默认账本", cursor.getString(0))
            }
            database.query("SELECT assetName FROM assets WHERE assetId = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("现金", cursor.getString(0))
            }
        }
    }

    @Test
    fun versionFourCategoryMigrationRetainsCustomCategory() {
        helper.createDatabase("migration-category", 4).use { database ->
            database.execSQL(
                "INSERT INTO categories (categoryId, parentId, categoryName, iconUrl, positionWeight, categoryType, " +
                    "isCustom, hide, createdAt) VALUES (1001, 0, '自定义分类', '', 1, 0, 1, 0, 100)",
            )
        }

        helper.runMigrationsAndValidate("migration-category", 5, true, AppDatabase.MIGRATION_4_5).use { database ->
            database.query("SELECT categoryName, hideBook FROM categories WHERE categoryId = 1001").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("自定义分类", cursor.getString(0))
                assertTrue(cursor.isNull(1))
            }
        }
    }

    @Test
    fun versionFivePlanMigrationCreatesQueryableTablesAndUniqueIndexes() {
        helper.createDatabase("migration-plans", 5).close()

        helper.runMigrationsAndValidate("migration-plans", 6, true, AppDatabase.MIGRATION_5_6).use { database ->
            for (table in listOf("budgets", "category_budgets", "cycles", "cycle_logs", "module_bills", "dreams", "dream_logs")) {
                database.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(0))
                }
            }
            database.query("PRAGMA index_list('cycle_logs')").use { cursor ->
                var uniqueDedupIndex = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "index_cycle_logs_dedupKey") {
                        uniqueDedupIndex = cursor.getInt(cursor.getColumnIndexOrThrow("unique")) == 1
                    }
                }
                assertTrue(uniqueDedupIndex)
            }
        }
    }
}
