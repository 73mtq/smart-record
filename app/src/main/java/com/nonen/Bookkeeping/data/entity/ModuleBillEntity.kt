package com.nonen.Bookkeeping.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 账单模板主实体表
 */
@Entity(
    tableName = "module_bills",
    indices = [
        Index("bookId"),
        Index("moduleType"),
        Index("positionWeight"),
    ],
)
data class ModuleBillEntity(
    @PrimaryKey(autoGenerate = true)
    val moduleBillId: Long = 0L,
    val bookId: Long = 1L,
    val moduleType: Int = MODULE_TYPE_BILL,
    val templateName: String = "常用模板",
    val cost: Double,
    val parentCategoryId: Int? = null,
    val childCategoryId: Int? = null,
    val assetId: Long = 1L,
    val fromAssetId: Long = 1L,
    val toAssetId: Long = 1L,
    val serviceCharge: Double = 0.0,
    val remark: String? = null,
    val tags: String? = null,
    val moduleLock: Boolean = false,
    val notIntoBudget: Boolean = false,
    val notIntoTotal: Boolean = false,
    val positionWeight: Int = 0,
    val updateTime: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MODULE_TYPE_BILL = 0
        const val MODULE_TYPE_TRANSFER = 1
    }
}
