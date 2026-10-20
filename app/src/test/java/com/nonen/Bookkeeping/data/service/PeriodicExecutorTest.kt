package com.nonen.Bookkeeping.data.service

import com.nonen.Bookkeeping.data.entity.CycleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * 周期任务匹配与推算纯算法单元测试。
 * 遵循无 Fake DAO、无 Mock 规范，直接验证纯函数算法的准确性。
 */
class PeriodicExecutorTest {

    @Test
    fun `test isCycleMatching for day month and week`() {
        val cycleDaily = CycleEntity(cost = 10.0, dateMode = CycleEntity.MODE_EVERY_DAY)
        val cal = Calendar.getInstance()
        assertTrue(PeriodicTransactionExecutor.isCycleMatching(cycleDaily, cal))

        // 每月 15 号
        cal.set(2026, Calendar.OCTOBER, 15)
        val cycleMonthly = CycleEntity(cost = 100.0, dateMode = CycleEntity.MODE_EVERY_MONTH, monthDay = 15)
        assertTrue(PeriodicTransactionExecutor.isCycleMatching(cycleMonthly, cal))

        cal.set(2026, Calendar.OCTOBER, 16)
        assertFalse(PeriodicTransactionExecutor.isCycleMatching(cycleMonthly, cal))

        // 月末最后一天：10月有 31 天
        cal.set(2026, Calendar.OCTOBER, 31)
        val cycleMonthLast = CycleEntity(cost = 200.0, dateMode = CycleEntity.MODE_EVERY_MONTH, monthLast = true)
        assertTrue(PeriodicTransactionExecutor.isCycleMatching(cycleMonthLast, cal))

        cal.set(2026, Calendar.OCTOBER, 30)
        assertFalse(PeriodicTransactionExecutor.isCycleMatching(cycleMonthLast, cal))

        // 工作日匹配验证（周六与周日不满足）
        cal.set(2026, Calendar.OCTOBER, 17) // 2026-10-17 是周六
        val cycleWorkDay = CycleEntity(cost = 50.0, dateMode = CycleEntity.MODE_EVERY_WORK_DAY)
        assertFalse(PeriodicTransactionExecutor.isCycleMatching(cycleWorkDay, cal))

        cal.set(2026, Calendar.OCTOBER, 19) // 2026-10-19 是周一
        assertTrue(PeriodicTransactionExecutor.isCycleMatching(cycleWorkDay, cal))
    }

    @Test
    fun `test calculatePendingDates range and limits`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val addTime = cal.timeInMillis

        // 每天执行，限制总次数为 3 次
        val cycle = CycleEntity(
            cycleId = 1L,
            cost = 20.0,
            dateMode = CycleEntity.MODE_EVERY_DAY,
            addTime = addTime,
            cycleTimes = 3,
            addTimes = 0,
        )

        // 模拟当前时间为 2026-10-10（跨度 10 天），由于 cycleTimes=3，推算结果应准确截断为 3 笔时间戳
        cal.set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        val now = cal.timeInMillis

        val pendingDates = PeriodicTransactionExecutor.calculatePendingDates(cycle, now)
        assertEquals(3, pendingDates.size)

        // 验证上次已有执行时间（lastAddTime）时，推算起始点正确避开前序已执行日
        cal.set(2026, Calendar.OCTOBER, 2, 0, 0, 0)
        val lastAddTime = cal.timeInMillis
        val updatedCycle = cycle.copy(
            lastAddTime = lastAddTime,
            addTimes = 2,
            cycleTimes = 3,
        )

        val nextPendingDates = PeriodicTransactionExecutor.calculatePendingDates(updatedCycle, now)
        // 之前已执行 2 次，剩余总配额 1 次
        assertEquals(1, nextPendingDates.size)
    }
}
