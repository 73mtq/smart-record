package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.DreamDao
import com.nonen.Bookkeeping.data.entity.DreamEntity
import com.nonen.Bookkeeping.data.entity.DreamLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SavingsGoalDetail(
    val dream: DreamEntity,
    val currentSavings: Double,
    val remainingAmount: Double,
    val progressRatio: Float,
    val remainingDays: Int,
    val dailyRecommended: Double,
    val isCompleted: Boolean,
)

class SavingsGoalRepository(
    private val dreamDao: DreamDao,
) {
    fun observeAllGoals(): Flow<List<SavingsGoalDetail>> {
        return dreamDao.observeAllDreams().map { list ->
            list.map { computeGoalDetail(it) }
        }
    }

    suspend fun getGoalById(id: Long): SavingsGoalDetail? {
        val dream = dreamDao.getDreamById(id) ?: return null
        return computeGoalDetail(dream)
    }

    suspend fun createGoal(
        name: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        endTime: Long,
        remark: String? = null,
        iconUrl: String = "",
    ): Long {
        val roundTarget = roundToTwoDecimals(targetAmount)
        val roundInit = roundToTwoDecimals(initialAmount)
        val dream = DreamEntity(
            dreamName = name,
            totalNum = roundTarget,
            startNum = roundInit,
            addNum = 0.0,
            endTime = endTime,
            remark = remark,
            iconUrl = iconUrl,
            updateTime = System.currentTimeMillis(),
        )
        return dreamDao.insertDream(dream)
    }

    suspend fun updateGoal(dream: DreamEntity) {
        dreamDao.updateDream(dream.copy(updateTime = System.currentTimeMillis()))
    }

    suspend fun deleteGoal(dreamId: Long) {
        dreamDao.deleteLogsByDreamId(dreamId)
        dreamDao.deleteDreamById(dreamId)
    }

    /**
     * 存入资金
     */
    suspend fun deposit(
        dreamId: Long,
        amount: Double,
        assetId: Long? = null,
        remark: String? = null,
        time: Long = System.currentTimeMillis(),
    ) {
        val dream = dreamDao.getDreamById(dreamId) ?: return
        val roundAmt = roundToTwoDecimals(amount)
        val newAdd = roundToTwoDecimals(dream.addNum + roundAmt)
        dreamDao.updateDream(dream.copy(addNum = newAdd, updateTime = time))
        dreamDao.insertLog(
            DreamLogEntity(
                dreamId = dreamId,
                amount = roundAmt,
                assetId = assetId,
                time = time,
                remark = remark ?: "存入资金",
            ),
        )
    }

    /**
     * 取出资金
     */
    suspend fun withdraw(
        dreamId: Long,
        amount: Double,
        assetId: Long? = null,
        remark: String? = null,
        time: Long = System.currentTimeMillis(),
    ) {
        val dream = dreamDao.getDreamById(dreamId) ?: return
        val roundAmt = roundToTwoDecimals(amount)
        val newAdd = roundToTwoDecimals(dream.addNum - roundAmt)
        dreamDao.updateDream(dream.copy(addNum = newAdd, updateTime = time))
        dreamDao.insertLog(
            DreamLogEntity(
                dreamId = dreamId,
                amount = -roundAmt,
                assetId = assetId,
                time = time,
                remark = remark ?: "取出资金",
            ),
        )
    }

    fun observeLogs(dreamId: Long): Flow<List<DreamLogEntity>> {
        return dreamDao.observeLogsForDream(dreamId)
    }

    private fun computeGoalDetail(dream: DreamEntity): SavingsGoalDetail {
        val current = roundToTwoDecimals(dream.startNum + dream.addNum)
        val remaining = roundToTwoDecimals(max(0.0, dream.totalNum - current))
        val ratio = if (dream.totalNum > 0) min(1f, (current / dream.totalNum).toFloat()) else 0f
        val isDone = current >= dream.totalNum && dream.totalNum > 0

        val now = System.currentTimeMillis()
        val diffMillis = dream.endTime - now
        val remainingDays = if (diffMillis > 0) {
            (diffMillis / (1000 * 3600 * 24)).toInt() + 1
        } else {
            0
        }

        val dailyRec = if (remainingDays > 0 && remaining > 0) {
            roundToTwoDecimals(remaining / remainingDays)
        } else {
            0.0
        }

        return SavingsGoalDetail(
            dream = dream,
            currentSavings = current,
            remainingAmount = remaining,
            progressRatio = ratio,
            remainingDays = remainingDays,
            dailyRecommended = dailyRec,
            isCompleted = isDone,
        )
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
