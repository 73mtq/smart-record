package com.nonen.Bookkeeping.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 周期规则主实体表
 */
@Entity(
    tableName = "cycles",
    indices = [
        Index("bookId"),
        Index("cycleType"),
        Index("dateMode"),
        Index("pause"),
        Index("end"),
    ],
)
data class CycleEntity(
    @PrimaryKey(autoGenerate = true)
    val cycleId: Long = 0L,
    val bookId: Long = 1L,
    val cycleType: Int = CYCLE_TYPE_BILL,
    val cost: Double,
    val serviceCharge: Double = 0.0,
    val parentCategoryId: Int? = null,
    val childCategoryId: Int? = null,
    val assetId: Long = 1L,
    val fromAssetId: Long = 1L,
    val toAssetId: Long = 1L,
    val dateMode: Int = MODE_EVERY_MONTH,
    val weekdays: String? = null,
    val monthDay: Int = 1,
    val monthLast: Boolean = false,
    val num: Int = 1,
    val remark: String? = null,
    val tags: String? = null,
    val addTime: Long = System.currentTimeMillis(),
    val lastAddTime: Long = 0L,
    val nextTime: Long = 0L,
    val addTimes: Int = 0,
    val cycleTimes: Int = 0,
    val endMode: Int = 0,
    val cycleEndDate: Long = 0L,
    val pause: Boolean = false,
    val end: Boolean = false,
    val notIntoBudget: Boolean = false,
    val notIntoTotal: Boolean = false,
    val updateTime: Long = System.currentTimeMillis(),
) {
    companion object {
        const val CYCLE_TYPE_BILL = 0
        const val CYCLE_TYPE_TRANSFER = 1

        const val MODE_EVERY_DAY = 0
        const val MODE_EVERY_WEEK = 1
        const val MODE_EVERY_MONTH = 2
        const val MODE_EVERY_YEAR = 3
        const val MODE_EVERY_DAY_NUM = 4
        const val MODE_EVERY_WORK_DAY = 5
    }
}

/**
 * 周期执行幂等日志表
 */
@Entity(
    tableName = "cycle_logs",
    indices = [
        Index(value = ["dedupKey"], unique = true),
        Index("cycleId"),
        Index("executedTime"),
    ],
)
data class CycleLogEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0L,
    val cycleId: Long,
    val dedupKey: String,
    val transactionId: Long? = null,
    val transferId: Long? = null,
    val executedTime: Long = System.currentTimeMillis(),
    val amount: Double,
    val status: Int = 1,
)
