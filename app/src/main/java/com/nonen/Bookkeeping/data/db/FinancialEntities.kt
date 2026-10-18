package com.nonen.Bookkeeping.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 资产账户实体（对应原应用 Asset 结构）。
 * 覆盖现金、银行卡、信用卡、借贷虚拟账户等。
 */
@Entity(
    tableName = "assets",
    indices = [
        Index("assetType"),
    ],
)
data class AssetEntity(
    @PrimaryKey val assetId: Long,
    val assetName: String,
    val assetNumber: Double = 0.0,
    val totalQuota: Double = 0.0,
    /** 账户类型：1 资金账户、2 信用卡账户、6 借贷借出(债权)、7 借贷借入(债务) */
    val assetType: Int = 1,
    val intoTotalAsset: Boolean = true,
    val hide: Boolean = false,
    val currency: String? = "CNY",
    val groupName: String? = null,
    val positionWeight: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * 资金转账记录实体（对应原应用 Transfer 结构）。
 */
@Entity(
    tableName = "transfers",
    indices = [
        Index("fromAssetId"),
        Index("toAssetId"),
        Index("time"),
    ],
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val transferId: Long = 0L,
    val fromAssetId: Long,
    val toAssetId: Long,
    val cost: Double,
    val toCost: Double = cost,
    val serviceCharge: Double = 0.0,
    val billId: Long? = null,
    val time: Long,
    val remark: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 退款冲正记录实体（对应原应用 Refund 结构）。
 */
@Entity(
    tableName = "refunds",
    indices = [
        Index("billId"),
    ],
)
data class RefundEntity(
    @PrimaryKey(autoGenerate = true) val refundId: Long = 0L,
    val billId: Long,
    val refundNum: Double,
    /** JSON 序列化的退款明细列表 */
    val refundInfos: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 报销业务记录实体（对应原应用 Reimbursement 结构）。
 */
@Entity(
    tableName = "reimbursements",
    indices = [
        Index("billId"),
        Index("assetId"),
    ],
)
data class ReimbursementEntity(
    @PrimaryKey(autoGenerate = true) val reimbursementId: Long = 0L,
    val billId: Long,
    val assetId: Long,
    val reimbursementAssetId: Long = assetId,
    val reimbursementNum: Double = 0.0,
    /** JSON 序列化回款批次明细列表 */
    val reimbursementNumbers: String = "[]",
    val end: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 借贷流转实体（对应原应用 Lend 结构）。
 */
@Entity(
    tableName = "lends",
    indices = [
        Index("assetId"),
        Index("repaymentAssetId"),
        Index("time"),
    ],
)
data class LendEntity(
    @PrimaryKey(autoGenerate = true) val lendId: Long = 0L,
    /** 借贷类型：1 借出、2 收取还款、3 借入、4 归还借款 */
    val type: Int,
    val assetId: Long,
    val repaymentAssetId: Long,
    val number: Double,
    val interest: Double = 0.0,
    val billId: Long? = null,
    val fromCost: Double = number,
    val time: Long,
    val remark: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 分期管理实体（对应原应用 Instalment 结构）。
 */
@Entity(
    tableName = "instalments",
    indices = [
        Index("billId"),
        Index("assetId"),
    ],
)
data class InstalmentEntity(
    @PrimaryKey(autoGenerate = true) val instalmentId: Long = 0L,
    val billId: Long,
    val assetId: Long,
    /** 分期类型：1 账单分期、2 信用卡还款分期 */
    val instalmentType: Int = 1,
    val totalNumber: Double,
    val periods: Int,
    val serviceNumber: Double = 0.0,
    /** 手续费收取方式：0 一次性收取、1 每期均摊 */
    val serviceType: Int = 0,
    /** 零头处理规则：0 计入首期、1 计入末期、2 首期调平、3 末期调平 */
    val remainderType: Int = 0,
    val accountMonth: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 账本实体。
 */
@Entity(
    tableName = "books",
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val bookId: Long = 0L,
    val bookName: String,
    val isDefault: Boolean = false,
    val positionWeight: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 标签实体。
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["tagName"], unique = true),
    ],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0L,
    val tagName: String,
    val positionWeight: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 账单与标签多对多关联实体。
 */
@Entity(
    tableName = "transaction_tag_refs",
    primaryKeys = ["transactionId", "tagId"],
    indices = [
        Index("tagId"),
    ],
)
data class TransactionTagCrossRef(
    val transactionId: Long,
    val tagId: Long,
)
