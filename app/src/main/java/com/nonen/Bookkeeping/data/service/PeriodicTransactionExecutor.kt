package com.nonen.Bookkeeping.data.service

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.AssetDao
import com.nonen.Bookkeeping.data.db.CycleDao
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.TransferDao
import com.nonen.Bookkeeping.data.db.TransferEntity
import com.nonen.Bookkeeping.data.entity.CycleEntity
import com.nonen.Bookkeeping.data.entity.CycleLogEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class PeriodicTransactionExecutor(
    private val database: AppDatabase,
    private val cycleDao: CycleDao,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val assetDao: AssetDao,
) {
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    /**
     * 扫描并执行全部待入账周期任务，返回本次执行入账的总笔数
     */
    suspend fun executePendingCycles(now: Long = System.currentTimeMillis()): Int {
        val activeCycles = cycleDao.getActiveCycles()
        var executedCount = 0

        for (activeCycle in activeCycles) {
            var currentCycle = activeCycle
            val pendingDates = calculatePendingDates(currentCycle, now)
            for (dateMillis in pendingDates) {
                if (currentCycle.end) {
                    break
                }
                val dateStr = dateFormat.format(Date(dateMillis))
                val dedupKey = "cycle_${currentCycle.cycleId}_$dateStr"

                if (cycleDao.hasExecuted(dedupKey)) {
                    continue
                }

                val executed = database.withTransaction {
                    executeSingleOccurrence(currentCycle, dateMillis, dedupKey)
                }
                if (executed) {
                    executedCount++
                    currentCycle = cycleDao.getCycleById(currentCycle.cycleId) ?: currentCycle
                }
            }
        }
        return executedCount
    }

    /**
     * 执行单次周期入账，保证事务原子性
     */
    private suspend fun executeSingleOccurrence(
        cycle: CycleEntity,
        occurrenceTime: Long,
        dedupKey: String,
    ): Boolean {
        // 双重校验幂等防重
        if (cycleDao.hasExecuted(dedupKey)) {
            return false
        }

        var txId: Long? = null
        var transferId: Long? = null

        if (cycle.cycleType == CycleEntity.CYCLE_TYPE_BILL) {
            // 记账业务
            val amount = -abs(cycle.cost) // 周期账单默认为支出扣费
            val tx = TransactionEntity(
                amount = amount,
                category = "周期记账",
                note = cycle.remark ?: "周期记账自动生成",
                timestamp = occurrenceTime,
                source = "periodic",
                parentCategoryId = cycle.parentCategoryId,
                childCategoryId = cycle.childCategoryId,
                assetId = cycle.assetId,
                bookId = cycle.bookId,
                transactionType = "EXPENSE",
                hash = "periodic_${cycle.cycleId}_${occurrenceTime}_$dedupKey",
            )
            txId = transactionDao.insert(tx)

            // 原子扣减账户余额
            assetDao.updateBalance(cycle.assetId, amount, occurrenceTime)
        } else if (cycle.cycleType == CycleEntity.CYCLE_TYPE_TRANSFER) {
            // 转账业务
            val transfer = TransferEntity(
                fromAssetId = cycle.fromAssetId,
                toAssetId = cycle.toAssetId,
                cost = cycle.cost,
                toCost = cycle.cost,
                serviceCharge = cycle.serviceCharge,
                time = occurrenceTime,
                remark = cycle.remark ?: "周期转账自动生成",
            )
            transferId = transferDao.insert(transfer)

            // 原子联动两账户余额
            assetDao.updateBalance(cycle.fromAssetId, -(cycle.cost + cycle.serviceCharge), occurrenceTime)
            assetDao.updateBalance(cycle.toAssetId, cycle.cost, occurrenceTime)
        }

        // 插入幂等执行日志
        cycleDao.insertLog(
            CycleLogEntity(
                cycleId = cycle.cycleId,
                dedupKey = dedupKey,
                transactionId = txId,
                transferId = transferId,
                executedTime = occurrenceTime,
                amount = cycle.cost,
                status = 1,
            ),
        )

        // 更新周期规则状态
        val newAddTimes = cycle.addTimes + 1
        val shouldEnd = (cycle.cycleTimes > 0 && newAddTimes >= cycle.cycleTimes) ||
            (cycle.cycleEndDate in 1..occurrenceTime)

        val updatedCycle = cycle.copy(
            lastAddTime = occurrenceTime,
            addTimes = newAddTimes,
            end = shouldEnd,
            updateTime = System.currentTimeMillis(),
        )
        cycleDao.updateCycle(updatedCycle)
        return true
    }

    fun calculatePendingDates(cycle: CycleEntity, now: Long): List<Long> =
        Companion.calculatePendingDates(cycle, now)

    fun isCycleMatching(cycle: CycleEntity, cal: Calendar): Boolean =
        Companion.isCycleMatching(cycle, cal)

    companion object {
        /**
         * 推算从上次执行（或首次开始）到当前时间点之间，所有应该触发的日期时间戳列表
         */
        fun calculatePendingDates(cycle: CycleEntity, now: Long): List<Long> {
            val result = mutableListOf<Long>()
            val startCal = Calendar.getInstance()

            val baseTime = if (cycle.lastAddTime > 0L) {
                cycle.lastAddTime
            } else {
                cycle.addTime
            }
            startCal.timeInMillis = baseTime
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            if (cycle.lastAddTime > 0L) {
                startCal.add(Calendar.DAY_OF_MONTH, 1)
            }

            val endCal = Calendar.getInstance()
            endCal.timeInMillis = now
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            if (cycle.cycleEndDate in 1 until endCal.timeInMillis) {
                endCal.timeInMillis = cycle.cycleEndDate
            }

            val cur = startCal.clone() as Calendar
            var loopCount = 0
            while (!cur.after(endCal) && loopCount < 730) {
                loopCount++
                if (isCycleMatching(cycle, cur)) {
                    result.add(cur.timeInMillis)
                    if (cycle.cycleTimes > 0 && (cycle.addTimes + result.size) >= cycle.cycleTimes) {
                        break
                    }
                }
                cur.add(Calendar.DAY_OF_MONTH, 1)
            }
            return result
        }

        /**
         * 判断某特定日是否满足该周期规则的触发条件
         */
        fun isCycleMatching(cycle: CycleEntity, cal: Calendar): Boolean {
            return when (cycle.dateMode) {
                CycleEntity.MODE_EVERY_DAY -> true
                CycleEntity.MODE_EVERY_WEEK -> {
                    val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.SUNDAY -> 7
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        else -> 1
                    }
                    val selectedDays = cycle.weekdays?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
                    selectedDays.contains(dayOfWeek)
                }
                CycleEntity.MODE_EVERY_MONTH -> {
                    if (cycle.monthLast) {
                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.get(Calendar.DAY_OF_MONTH) == maxDay
                    } else {
                        cal.get(Calendar.DAY_OF_MONTH) == cycle.monthDay
                    }
                }
                CycleEntity.MODE_EVERY_YEAR -> {
                    val targetDay = cycle.monthDay
                    val isMonthMatch = (cal.get(Calendar.MONTH) + 1) == 1
                    cal.get(Calendar.DAY_OF_MONTH) == targetDay
                }
                CycleEntity.MODE_EVERY_WORK_DAY -> {
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                    dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
                }
                CycleEntity.MODE_EVERY_DAY_NUM -> {
                    val step = if (cycle.num > 0) cycle.num else 1
                    val diffDays = ((cal.timeInMillis - cycle.addTime) / (1000 * 3600 * 24)).toInt()
                    diffDays >= 0 && diffDays % step == 0
                }
                else -> false
            }
        }
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
