package com.nonen.Bookkeeping.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class Migration5To6Test {

    @Test
    fun `migration 5 to 6 creates all seven tables and indexes`() {
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

        AppDatabase.MIGRATION_5_6.migrate(fakeDb)

        assertTrue(
            "Expected CREATE TABLE budgets",
            executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS `budgets`") },
        )
        assertTrue(
            "Expected CREATE TABLE category_budgets",
            executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS `category_budgets`") },
        )
        assertTrue(
            "Expected CREATE TABLE cycles",
            executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS `cycles`") },
        )
        assertTrue(
            "Expected CREATE TABLE cycle_logs",
            executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS `cycle_logs`") },
        )
        assertTrue(
            "Expected CREATE TABLE module_bills",
            executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS `module_bills`") },
        )
        assertTrue(
            "Expected CREATE TABLE dreams",
            executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS `dreams`") },
        )
        assertTrue(
            "Expected CREATE TABLE dream_logs",
            executedSqls.any { it.contains("CREATE TABLE IF NOT EXISTS `dream_logs`") },
        )

        // 索引验证
        assertTrue(
            "Expected UNIQUE index on cycle_logs dedupKey",
            executedSqls.any { it.contains("index_cycle_logs_dedupKey") },
        )
        assertTrue(
            "Expected UNIQUE index on budgets (bookId, year, month, type)",
            executedSqls.any { it.contains("index_budgets_bookId_year_month_type") },
        )
    }
}
