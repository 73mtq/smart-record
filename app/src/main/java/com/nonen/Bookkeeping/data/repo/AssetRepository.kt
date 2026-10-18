package com.nonen.Bookkeeping.data.repo

import androidx.room.withTransaction
import com.nonen.Bookkeeping.core.HashUtil
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.AssetDao
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.TransactionDao
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.TransferDao
import kotlinx.coroutines.flow.Flow
import java.util.Locale

/**
 * 资产净值聚合统计模型。
 */
data class AssetSummary(
    val netAsset: Double = 0.0,
    val totalAsset: Double = 0.0,
    val totalLiability: Double = 0.0,
)

/**
 * 资产账户数据仓库：
 * 负责资产账户的增删改查、排序、显示隐藏、余额调平及净资产聚合计算。
 */
class AssetRepository(
    private val assetDao: AssetDao,
    private val transferDao: TransferDao? = null,
    private val transactionDao: TransactionDao? = null,
    private val database: AppDatabase? = null,
) {
    companion object {
        const val TYPE_FUND = 1       // 资金账户（现金、微信、支付宝、借记卡等）
        const val TYPE_CREDIT = 2     // 信用卡账户
        const val TYPE_CLAIM = 6      // 借贷借出（债权账户）
        const val TYPE_DEBT = 7       // 借贷借入（债务账户）

        const val DEFAULT_ASSET_ID = 1L
    }

    fun observeAll(): Flow<List<AssetEntity>> = assetDao.getAllFlow()

    fun observeVisible(): Flow<List<AssetEntity>> = assetDao.getAllVisibleFlow()

    suspend fun getAll(): List<AssetEntity> = assetDao.getAll()

    suspend fun getById(id: Long): AssetEntity? = assetDao.findById(id)

    suspend fun getByName(name: String): AssetEntity? = assetDao.findByName(name)

    /**
     * 计算资产净值、总资产与总负债。
     */
    fun calculateSummary(assets: List<AssetEntity>): AssetSummary {
        var totalAsset = 0.0
        var totalLiability = 0.0

        for (asset in assets) {
            if (!asset.intoTotalAsset) continue

            when (asset.assetType) {
                TYPE_CREDIT -> {
                    // 信用卡：若配置了信用总额度，已用额度视为负债，溢缴款视为资产
                    if (asset.totalQuota > 0.0) {
                        if (asset.assetNumber < asset.totalQuota) {
                            totalLiability += (asset.totalQuota - asset.assetNumber)
                        } else {
                            totalAsset += (asset.assetNumber - asset.totalQuota)
                        }
                    } else {
                        if (asset.assetNumber < 0.0) {
                            totalLiability += kotlin.math.abs(asset.assetNumber)
                        } else {
                            totalAsset += asset.assetNumber
                        }
                    }
                }
                TYPE_DEBT -> {
                    // 借入债务账户：金额绝对值记为负债
                    totalLiability += kotlin.math.abs(asset.assetNumber)
                }
                else -> {
                    // 资金账户与债权账户：正余额记为资产，透支负余额记为负债
                    if (asset.assetNumber >= 0.0) {
                        totalAsset += asset.assetNumber
                    } else {
                        totalLiability += kotlin.math.abs(asset.assetNumber)
                    }
                }
            }
        }

        val netAsset = totalAsset - totalLiability
        return AssetSummary(
            netAsset = roundDouble(netAsset),
            totalAsset = roundDouble(totalAsset),
            totalLiability = roundDouble(totalLiability),
        )
    }

    /**
     * 创建新资产账户。
     */
    suspend fun createAsset(
        name: String,
        initialBalance: Double = 0.0,
        quota: Double = 0.0,
        type: Int = TYPE_FUND,
        groupName: String? = null,
        intoTotalAsset: Boolean = true,
        currency: String? = "CNY",
    ): Long {
        val maxId = assetDao.getMaxAssetId() ?: 0L
        val nextId = kotlin.math.max(101L, maxId + 1L)
        val maxWeight = assetDao.getMaxPositionWeight() ?: 0
        val nextWeight = maxWeight + 1
        val now = System.currentTimeMillis()

        val entity = AssetEntity(
            assetId = nextId,
            assetName = name.trim(),
            assetNumber = roundDouble(initialBalance),
            totalQuota = roundDouble(quota),
            assetType = type,
            intoTotalAsset = intoTotalAsset,
            hide = false,
            currency = currency ?: "CNY",
            groupName = groupName?.trim()?.ifEmpty { null },
            positionWeight = nextWeight,
            createdAt = now,
            updatedAt = now,
        )
        assetDao.insert(entity)
        return nextId
    }

    /**
     * 更新资产账户信息。
     */
    suspend fun updateAsset(asset: AssetEntity) {
        val updated = asset.copy(
            assetName = asset.assetName.trim(),
            assetNumber = roundDouble(asset.assetNumber),
            totalQuota = roundDouble(asset.totalQuota),
            groupName = asset.groupName?.trim()?.ifEmpty { null },
            updatedAt = System.currentTimeMillis(),
        )
        assetDao.update(updated)
    }

    /**
     * 调整账户余额（调平），可选择是否生成调平账单。
     */
    suspend fun adjustBalance(
        assetId: Long,
        targetBalance: Double,
        generateBill: Boolean = false,
        note: String? = null,
    ) {
        val asset = assetDao.findById(assetId) ?: return
        val currentBalance = asset.assetNumber
        val delta = roundDouble(targetBalance - currentBalance)

        if (database != null) {
            database.withTransaction {
                executeAdjustBalance(asset, targetBalance, delta, generateBill, note)
            }
        } else {
            executeAdjustBalance(asset, targetBalance, delta, generateBill, note)
        }
    }

    private suspend fun executeAdjustBalance(
        asset: AssetEntity,
        targetBalance: Double,
        delta: Double,
        generateBill: Boolean,
        note: String?,
    ) {
        val now = System.currentTimeMillis()
        val updated = asset.copy(assetNumber = roundDouble(targetBalance), updatedAt = now)
        assetDao.update(updated)

        if (generateBill && kotlin.math.abs(delta) >= 0.01 && transactionDao != null) {
            val isIncome = delta > 0.0
            val absDelta = kotlin.math.abs(delta)
            val signedAmount = if (isIncome) absDelta else -absDelta
            val txNote = note?.trim()?.ifEmpty { null }
                ?: if (isIncome) "余额调平盘盈" else "余额调平盘亏"

            val tx = TransactionEntity(
                amount = signedAmount,
                category = "其他",
                subCategory = "其他",
                parentCategoryId = if (isIncome) 9 else 99,
                childCategoryId = null,
                bookId = 1L,
                assetId = asset.assetId,
                note = txNote,
                merchant = null,
                account = asset.assetName,
                tag = "余额调整",
                transactionType = if (isIncome) "INCOME" else "EXPENSE",
                timestamp = now,
                source = "manual",
                hash = HashUtil.transactionHash(now, signedAmount, null, "manual"),
            )
            transactionDao.insert(tx)
        }
    }

    /**
     * 切换账户隐藏状态。
     */
    suspend fun toggleHide(assetId: Long, hide: Boolean) {
        assetDao.updateAssetHide(assetId, hide)
    }

    /**
     * 资产批量调序。
     */
    suspend fun reorderAssets(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            assetDao.updatePositionWeight(id, index + 1)
        }
    }

    /**
     * 安全删除资产账户：
     * 默认现金账户（assetId = 1）禁止删除；
     * 关联账单将安全重定向至默认现金账户。
     */
    suspend fun deleteAsset(assetId: Long): Boolean {
        if (assetId == DEFAULT_ASSET_ID) {
            return false
        }
        val asset = assetDao.findById(assetId) ?: return false

        if (database != null) {
            database.withTransaction {
                executeDeleteAsset(asset)
            }
        } else {
            executeDeleteAsset(asset)
        }
        return true
    }

    private suspend fun executeDeleteAsset(asset: AssetEntity) {
        // 1. 将关联该账户的所有历史账单重定向至默认现金账户
        val defaultAsset = assetDao.findById(DEFAULT_ASSET_ID)
        val defaultAccountName = defaultAsset?.assetName ?: "现金"
        transactionDao?.reassignAsset(asset.assetId, DEFAULT_ASSET_ID, defaultAccountName)

        // 2. 删除该资产记录
        assetDao.deleteById(asset.assetId)
    }

    private fun roundDouble(v: Double): Double =
        String.format(Locale.US, "%.2f", v).toDouble()
}
