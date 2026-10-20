package com.nonen.Bookkeeping.export

import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.BookEntity
import com.nonen.Bookkeeping.data.db.CategoryEntity
import com.nonen.Bookkeeping.data.db.CategoryRuleEntity
import com.nonen.Bookkeeping.data.db.InstalmentEntity
import com.nonen.Bookkeeping.data.db.LendEntity
import com.nonen.Bookkeeping.data.db.MerchantCategoryEntity
import com.nonen.Bookkeeping.data.db.RefundEntity
import com.nonen.Bookkeeping.data.db.ReimbursementEntity
import com.nonen.Bookkeeping.data.db.TagEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.db.TransactionTagCrossRef
import com.nonen.Bookkeeping.data.db.TransferEntity
import com.nonen.Bookkeeping.data.entity.BudgetEntity
import com.nonen.Bookkeeping.data.entity.CategoryBudgetEntity
import com.nonen.Bookkeeping.data.entity.CycleEntity
import com.nonen.Bookkeeping.data.entity.CycleLogEntity
import com.nonen.Bookkeeping.data.entity.DreamEntity
import com.nonen.Bookkeeping.data.entity.DreamLogEntity
import com.nonen.Bookkeeping.data.entity.ModuleBillEntity

/**
 * 通用账单交换模型：代表跨格式（一木 23 列、微信、支付宝、CSV）的标准行数据结构。
 */
data class UnifiedBillRecord(
    val timeMillis: Long,
    val isIncome: Boolean,
    val amount: Double,
    val category: String,
    val subCategory: String? = null,
    val account: String? = null,
    val book: String? = null,
    val refund: String? = null,
    val discount: String? = null,
    val note: String? = null,
    val tag: String? = null,
    val reimbursementAccount: String? = null,
    val reimbursementAmount: String? = null,
    val reimbursementDetail: String? = null,
    val multiCurrency: String? = null,
    val address: String? = null,
    val createdUser: String? = null,
    val other: String? = null,
    val merchant: String? = null,
    val source: String? = null,
    val hash: String? = null,
)

/**
 * 本地完整备份清单元数据。
 */
data class BackupManifest(
    val formatVersion: String = "1.0",
    val appVersion: String,
    val createdAt: Long,
    val tableCounts: Map<String, Int>,
    val dataChecksum: String,
)

/**
 * 本地全量数据载体集合：覆盖全部领域实体与配置数据。
 */
data class FullBackupData(
    val transactions: List<TransactionEntity> = emptyList(),
    val assets: List<AssetEntity> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList(),
    val refunds: List<RefundEntity> = emptyList(),
    val reimbursements: List<ReimbursementEntity> = emptyList(),
    val lends: List<LendEntity> = emptyList(),
    val instalments: List<InstalmentEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val transactionTags: List<TransactionTagCrossRef> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val categoryBudgets: List<CategoryBudgetEntity> = emptyList(),
    val cycles: List<CycleEntity> = emptyList(),
    val cycleLogs: List<CycleLogEntity> = emptyList(),
    val moduleBills: List<ModuleBillEntity> = emptyList(),
    val dreams: List<DreamEntity> = emptyList(),
    val dreamLogs: List<DreamLogEntity> = emptyList(),
    val categoryRules: List<CategoryRuleEntity> = emptyList(),
    val merchantCategories: List<MerchantCategoryEntity> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
)

/**
 * 备份与恢复执行结果。
 */
data class FullBackupResult(
    val success: Boolean,
    val totalCount: Int = 0,
    val message: String = "",
    val error: String? = null,
)
