package com.nonen.Bookkeeping.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nonen.Bookkeeping.data.RoomRepositoryTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavingsGoalRepositoryTest : RoomRepositoryTest() {
    @Test
    fun depositsAndWithdrawalsPersistProgressAndLogs() = runBlocking {
        val repo = SavingsGoalRepository(database.dreamDao())
        val goalId = repo.createGoal(
            name = "购买新相机", targetAmount = 5000.0, initialAmount = 1000.0,
            endTime = System.currentTimeMillis() + 10L * 24 * 3600 * 1000, remark = "富士微单",
        )
        var detail = requireNotNull(repo.getGoalById(goalId))
        assertEquals(1000.0, detail.currentSavings, 0.001)
        assertEquals(4000.0, detail.remainingAmount, 0.001)
        assertEquals(0.2f, detail.progressRatio, 0.001f)
        assertFalse(detail.isCompleted)
        assertTrue(detail.dailyRecommended > 0.0)

        repo.deposit(goalId, 3000.0, remark = "工资结余")
        detail = requireNotNull(repo.getGoalById(goalId))
        assertEquals(4000.0, detail.currentSavings, 0.001)
        assertEquals(1000.0, detail.remainingAmount, 0.001)
        assertEquals(0.8f, detail.progressRatio, 0.001f)
        assertFalse(detail.isCompleted)

        repo.withdraw(goalId, 500.0, remark = "应急支出")
        detail = requireNotNull(repo.getGoalById(goalId))
        assertEquals(3500.0, detail.currentSavings, 0.001)
        assertEquals(1500.0, detail.remainingAmount, 0.001)
        assertEquals(0.7f, detail.progressRatio, 0.001f)

        repo.deposit(goalId, 1500.0, remark = "年终奖")
        detail = requireNotNull(repo.getGoalById(goalId))
        assertEquals(5000.0, detail.currentSavings, 0.001)
        assertEquals(0.0, detail.remainingAmount, 0.001)
        assertEquals(1.0f, detail.progressRatio, 0.001f)
        assertTrue(detail.isCompleted)
        val logs = repo.observeLogs(goalId).first()
        assertEquals(3, logs.size)
        assertEquals(listOf(-500.0, 1500.0, 3000.0), logs.map { it.amount }.sorted())
    }
}
