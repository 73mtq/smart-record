package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.CycleDao
import com.nonen.Bookkeeping.data.entity.CycleEntity
import com.nonen.Bookkeeping.data.entity.CycleLogEntity
import kotlinx.coroutines.flow.Flow

class PeriodicRepository(
    private val cycleDao: CycleDao,
) {
    fun observeAllCycles(bookId: Long = 1L): Flow<List<CycleEntity>> {
        return cycleDao.observeAllCycles(bookId)
    }

    suspend fun getCycleById(cycleId: Long): CycleEntity? {
        return cycleDao.getCycleById(cycleId)
    }

    suspend fun createCycle(cycle: CycleEntity): Long {
        return cycleDao.insertCycle(cycle)
    }

    suspend fun updateCycle(cycle: CycleEntity) {
        cycleDao.updateCycle(cycle)
    }

    suspend fun togglePause(cycleId: Long) {
        val cycle = cycleDao.getCycleById(cycleId) ?: return
        cycleDao.updateCycle(cycle.copy(pause = !cycle.pause, updateTime = System.currentTimeMillis()))
    }

    suspend fun deleteCycle(cycleId: Long) {
        cycleDao.deleteCycleById(cycleId)
    }

    fun observeLogsForCycle(cycleId: Long): Flow<List<CycleLogEntity>> {
        return cycleDao.observeLogsForCycle(cycleId)
    }
}
