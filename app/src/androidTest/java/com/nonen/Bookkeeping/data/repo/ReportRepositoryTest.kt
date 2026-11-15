package com.nonen.Bookkeeping.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nonen.Bookkeeping.data.RoomRepositoryTest
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.stats.StatsPeriod
import com.nonen.Bookkeeping.stats.StatsQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ReportRepositoryTest : RoomRepositoryTest() {
    private fun repository() = ReportRepository(database.transactionDao(), database.assetDao())

    @Test
    fun calendarMonthAggregatesPersistedTransactions() = runBlocking {
        database.transactionDao().insertAll(
            listOf(bill(-45.0, "calendar-first", day = 8), bill(-55.0, "calendar-second", day = 8)),
        )

        val days = repository().observeCalendarMonth(YearMonth.of(2026, 10)).first()

        assertEquals(31, days.size)
        val day = days.single { it.date.dayOfMonth == 8 }
        assertEquals(100.0, day.totalExpense, 0.001)
        assertEquals(2, day.transactionCount)
    }

    @Test
    fun subcategoryQueryRetainsAmountsAndPercentages() = runBlocking {
        database.transactionDao().insertAll(
            listOf(
                bill(-60.0, "subcategory-dinner").copy(subCategory = "晚餐"),
                bill(-40.0, "subcategory-lunch").copy(subCategory = "午餐"),
            ),
        )

        val result = repository().getSubCategoryBreakdown("食品餐饮", 0L, Long.MAX_VALUE)

        assertEquals(listOf("晚餐", "午餐"), result.map { it.name })
        assertEquals(listOf(60.0, 40.0), result.map { it.amount })
        assertEquals(0.6f, result[0].percentInParent, 0.001f)
        assertEquals(0.4f, result[1].percentInParent, 0.001f)
    }

    @Test
    fun statisticsQueryFiltersTheAccountInTheDatabase() = runBlocking {
        database.transactionDao().insertAll(
            listOf(
                bill(-100.0, "stats-account-one").copy(assetId = 1L),
                bill(-200.0, "stats-account-two").copy(assetId = 2L),
            ),
        )

        val result = requireNotNull(
            repository().getStats(
                StatsQuery(period = StatsPeriod.MONTH, isIncome = false, assetId = 1L),
                today = LocalDate.of(2026, 10, 15),
            ),
        )

        assertEquals(100.0, result.total, 0.001)
        assertEquals(1, result.count)
    }

    private fun bill(amount: Double, hash: String, day: Int = 15) = TransactionEntity(
        amount = amount, category = "食品餐饮", source = "manual", hash = hash,
        timestamp = LocalDate.of(2026, 10, day).atTime(12, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
}
