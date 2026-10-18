package com.nonen.Bookkeeping.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nonen.Bookkeeping.core.DefaultCategories
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `migration 3 to 4 executes all required DDL and DML operations`() {
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

        AppDatabase.MIGRATION_3_4.migrate(fakeDb)

        // 验证执行了关键 DDL 建表
        val requiredTables = listOf(
            "assets",
            "transfers",
            "refunds",
            "reimbursements",
            "lends",
            "instalments",
            "categories",
            "books",
            "tags",
            "transaction_tag_refs",
        )
        for (table in requiredTables) {
            assertTrue("Expected CREATE TABLE for $table", executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS `$table`") })
        }

        // 验证 transactions 扩充字段
        assertTrue(executedSqls.any { it.contains("ALTER TABLE `transactions` ADD COLUMN `assetId`") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE `transactions` ADD COLUMN `bookId`") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE `transactions` ADD COLUMN `parentCategoryId`") })
        assertTrue(executedSqls.any { it.contains("ALTER TABLE `transactions` ADD COLUMN `childCategoryId`") })

        // 验证索引创建
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS `index_transactions_assetId`") })
        assertTrue(executedSqls.any { it.contains("CREATE INDEX IF NOT EXISTS `index_transactions_bookId`") })

        // 验证默认账本与默认资产注入
        assertTrue(executedSqls.any { it.contains("INSERT OR IGNORE INTO `books`") && it.contains("默认账本") })
        assertTrue(executedSqls.any { it.contains("INSERT OR IGNORE INTO `assets`") && it.contains("现金") })

        // 验证历史数据回填
        assertTrue(executedSqls.any { it.contains("UPDATE `transactions` SET `assetId` = 1") })
        assertTrue(executedSqls.any { it.contains("UPDATE `transactions` SET `bookId` = 1") })

        // 验证 76 个分类全部插入
        val categoryInsertSqls = executedSqls.filter { it.contains("INSERT OR IGNORE INTO `categories`") }
        assertEquals(76, categoryInsertSqls.size)
    }
}
