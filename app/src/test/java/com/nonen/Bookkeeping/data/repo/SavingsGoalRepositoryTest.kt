package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.DreamDao
import com.nonen.Bookkeeping.data.entity.DreamEntity
import com.nonen.Bookkeeping.data.entity.DreamLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsGoalRepositoryTest {

    private class FakeDreamDao : DreamDao {
        val dreams = mutableListOf<DreamEntity>()
        val logs = mutableListOf<DreamLogEntity>()
        private var idSeq = 1L
        private var logSeq = 1L

        override suspend fun insertDream(dream: DreamEntity): Long {
            val id = if (dream.dreamId > 0L) dream.dreamId else idSeq++
            val entity = dream.copy(dreamId = id)
            dreams.removeAll { it.dreamId == id }
            dreams.add(entity)
            return id
        }

        override suspend fun updateDream(dream: DreamEntity) {
            val idx = dreams.indexOfFirst { it.dreamId == dream.dreamId }
            if (idx >= 0) dreams[idx] = dream
        }

        override suspend fun deleteDream(dream: DreamEntity) {
            dreams.removeAll { it.dreamId == dream.dreamId }
        }

        override suspend fun deleteDreamById(id: Long) {
            dreams.removeAll { it.dreamId == id }
        }

        override fun observeAllDreams(): Flow<List<DreamEntity>> = flowOf(dreams)

        override suspend fun getDreamById(id: Long): DreamEntity? = dreams.find { it.dreamId == id }

        override suspend fun insertLog(log: DreamLogEntity): Long {
            val id = if (log.logId > 0L) log.logId else logSeq++
            val entity = log.copy(logId = id)
            logs.add(entity)
            return id
        }

        override suspend fun deleteLog(log: DreamLogEntity) {
            logs.removeAll { it.logId == log.logId }
        }

        override fun observeLogsForDream(dreamId: Long): Flow<List<DreamLogEntity>> =
            flowOf(logs.filter { it.dreamId == dreamId })

        override suspend fun getLogsForDream(dreamId: Long): List<DreamLogEntity> =
            logs.filter { it.dreamId == dreamId }

        override suspend fun deleteLogsByDreamId(dreamId: Long) {
            logs.removeAll { it.dreamId == dreamId }
        }
    }

    @Test
    fun `test savings goal creation and progress computation`() = runBlocking {
        val dao = FakeDreamDao()
        val repo = SavingsGoalRepository(dao)

        val now = System.currentTimeMillis()
        val targetDays = 10
        val endTime = now + targetDays.toLong() * 24 * 3600 * 1000

        val goalId = repo.createGoal(
            name = "购买新相机",
            targetAmount = 5000.0,
            initialAmount = 1000.0,
            endTime = endTime,
            remark = "富士微单",
        )

        var detail = repo.getGoalById(goalId)
        assertNotNull(detail)
        assertEquals(1000.0, detail!!.currentSavings, 0.001)
        assertEquals(4000.0, detail.remainingAmount, 0.001)
        assertEquals(0.2f, detail.progressRatio, 0.01f)
        assertFalse(detail.isCompleted)
        assertTrue(detail.dailyRecommended > 0.0)

        // 存入 3000 元
        repo.deposit(goalId, 3000.0, remark = "工资结余")
        detail = repo.getGoalById(goalId)
        assertEquals(4000.0, detail!!.currentSavings, 0.001)
        assertEquals(1000.0, detail.remainingAmount, 0.001)
        assertEquals(0.8f, detail.progressRatio, 0.01f)
        assertFalse(detail.isCompleted)

        // 取出 500 元
        repo.withdraw(goalId, 500.0, remark = "应急支出")
        detail = repo.getGoalById(goalId)
        assertEquals(3500.0, detail!!.currentSavings, 0.001)
        assertEquals(1500.0, detail.remainingAmount, 0.001)
        assertEquals(0.7f, detail.progressRatio, 0.01f)

        // 再存入 1500 元，达成目标
        repo.deposit(goalId, 1500.0, remark = "年终奖")
        detail = repo.getGoalById(goalId)
        assertEquals(5000.0, detail!!.currentSavings, 0.001)
        assertEquals(0.0, detail.remainingAmount, 0.001)
        assertEquals(1.0f, detail.progressRatio, 0.01f)
        assertTrue(detail.isCompleted)

        // 验证流水日志
        val logs = repo.observeLogs(goalId).first()
        assertEquals(3, logs.size)
        assertEquals(3000.0, logs[0].amount, 0.001)
        assertEquals(-500.0, logs[1].amount, 0.001)
        assertEquals(1500.0, logs[2].amount, 0.001)
    }
}
