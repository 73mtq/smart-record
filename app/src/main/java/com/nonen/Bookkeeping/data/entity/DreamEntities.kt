package com.nonen.Bookkeeping.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 储蓄目标实体表
 */
@Entity(
    tableName = "dreams",
    indices = [
        Index("positionWeight"),
    ],
)
data class DreamEntity(
    @PrimaryKey(autoGenerate = true)
    val dreamId: Long = 0L,
    val dreamName: String,
    val iconUrl: String = "",
    val totalNum: Double,
    val startNum: Double = 0.0,
    val addNum: Double = 0.0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long,
    val remark: String? = null,
    val addType: Int = 0,
    val assetMode: Int = 0,
    val assetList: String? = null,
    val positionWeight: Int = 0,
    val updateTime: Long = System.currentTimeMillis(),
)

/**
 * 储蓄目标明细流水表
 */
@Entity(
    tableName = "dream_logs",
    indices = [
        Index("dreamId"),
        Index("time"),
    ],
)
data class DreamLogEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0L,
    val dreamId: Long,
    val amount: Double,
    val assetId: Long? = null,
    val time: Long = System.currentTimeMillis(),
    val remark: String? = null,
)
