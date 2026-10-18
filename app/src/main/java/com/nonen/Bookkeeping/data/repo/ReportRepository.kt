package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.AssetDao
import com.nonen.Bookkeeping.data.db.CategoryDao
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.stats.AssetTrendPoint
import com.nonen.Bookkeeping.stats.CalendarDaySummary
import com.nonen.Bookkeeping.stats.StatsCalculator
import com.nonen.Bookkeeping.stats.StatsData
import com.nonen.Bookkeeping.stats.StatsQuery
import com.nonen.Bookkeeping.stats.SubCategoryStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 报表与统计核算业务仓库：
 * 负责日历收支汇总、二级分类穿透明细、资产走势逆推与多维统计过滤。
 */
class ReportRepository(
    private val transactionDao: TransactionDao,
    private val assetDao: AssetDao,
    private val categoryDao: CategoryDao? = null,
) {
    private val zone: ZoneId = ZoneId.systemDefault()

    private fun startOfDayMillis(d: LocalDate): Long =
        d.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun endOfDayMillis(d: LocalDate): Long =
        d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    /**
     * 响应式监听指定自然月日历汇总。
     */
    fun observeCalendarMonth(
        yearMonth: YearMonth,
        bookId: Long? = null,
        assetId: Long? = null,
    ): Flow<List<CalendarDaySummary>> {
        val startMillis = startOfDayMillis(yearMonth.atDay(1))
        val endMillis = endOfDayMillis(yearMonth.atEndOfMonth())
        return transactionDao.observeFilteredRange(startMillis, endMillis, bookId, assetId).map { txList ->
            StatsCalculator.computeCalendarMonth(yearMonth, txList, zone)
        }
    }

    /**
     * 直接查询指定月份日历汇总。
     */
    suspend fun getCalendarMonth(
        yearMonth: YearMonth,
        bookId: Long? = null,
        assetId: Long? = null,
    ): List<CalendarDaySummary> {
        val startMillis = startOfDayMillis(yearMonth.atDay(1))
        val endMillis = endOfDayMillis(yearMonth.atEndOfMonth())
        val txList = transactionDao.getFilteredRange(startMillis, endMillis, bookId, assetId)
        return StatsCalculator.computeCalendarMonth(yearMonth, txList, zone)
    }

    /**
     * 响应式监听指定日期的全部账单。
     */
    fun observeDayTransactions(
        date: LocalDate,
        bookId: Long? = null,
        assetId: Long? = null,
    ): Flow<List<TransactionEntity>> {
        val startMillis = startOfDayMillis(date)
        val endMillis = endOfDayMillis(date)
        return transactionDao.observeFilteredRange(startMillis, endMillis, bookId, assetId)
    }

    /**
     * 依据统计条件执行全面核算。
     */
    suspend fun getStats(query: StatsQuery, today: LocalDate = LocalDate.now()): StatsData? {
        return StatsCalculator.compute(
            query = query,
            fetch = { start, end ->
                transactionDao.getFilteredRange(start, end, query.bookId, query.assetId)
            },
            today = today,
        )
    }

    /**
     * 推算指定日期区间的资产变动趋势。
     */
    suspend fun getAssetTrend(startDate: LocalDate, endDate: LocalDate): List<AssetTrendPoint> {
        val currentAssets = assetDao.getAll()
        val allTx = transactionDao.getAll()
        return StatsCalculator.computeAssetTrend(startDate, endDate, currentAssets, allTx, zone)
    }

    /**
     * 查询指定一级分类下属二级分类明细。
     */
    suspend fun getSubCategoryBreakdown(
        parentName: String,
        startMillis: Long,
        endMillis: Long,
        bookId: Long? = null,
    ): List<SubCategoryStat> {
        val list = transactionDao.getFilteredRange(startMillis, endMillis, bookId = bookId)
        return StatsCalculator.computeSubCategoryBreakdown(parentName, list)
    }
}
