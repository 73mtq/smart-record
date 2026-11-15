package com.nonen.Bookkeeping

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.core.DefaultCategories
import com.nonen.Bookkeeping.core.RuleEngine
import com.nonen.Bookkeeping.core.nlp.ClassificationModelService
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.CategoryRuleEntity
import com.nonen.Bookkeeping.data.migration.CategoryMigrationService
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import com.nonen.Bookkeeping.data.repo.AssetRepository
import com.nonen.Bookkeeping.data.repo.BudgetRepository
import com.nonen.Bookkeeping.data.repo.CategoryRepository
import com.nonen.Bookkeeping.data.repo.PeriodicRepository
import com.nonen.Bookkeeping.data.repo.ReportRepository
import com.nonen.Bookkeeping.data.repo.RuleRepository
import com.nonen.Bookkeeping.data.repo.TagRepository
import com.nonen.Bookkeeping.data.repo.SavingsGoalRepository
import com.nonen.Bookkeeping.data.repo.TemplateRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.data.service.PeriodicTransactionExecutor
import com.nonen.Bookkeeping.export.FullBackupService
import com.nonen.Bookkeeping.parse.BillImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(val appContext: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settings = SettingsStore(appContext)

    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "bookkeeping.db")
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
        )
        .addCallback(object : RoomDatabase.Callback() {
            /** 早期版本把默认现金账户的分组名写成“现金账户”，原版分组名是“资金账户”，打开时统一更正 */
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("UPDATE `assets` SET `groupName` = '资金账户' WHERE `groupName` = '现金账户'")
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                db.execSQL("INSERT OR IGNORE INTO `books` (`bookId`, `bookName`, `isDefault`, `positionWeight`, `createdAt`) VALUES (1, '默认账本', 1, 1, $now)")
                db.execSQL("INSERT OR IGNORE INTO `assets` (`assetId`, `assetName`, `assetNumber`, `totalQuota`, `assetType`, `intoTotalAsset`, `hide`, `currency`, `groupName`, `positionWeight`, `createdAt`, `updatedAt`) VALUES (1, '现金', 0.0, 0.0, 1, 1, 0, 'CNY', '资金账户', 1, $now, $now)")
                DefaultCategories.ALL.forEach { cat ->
                    val isCustom = if (cat.isCustom) 1 else 0
                    val hide = if (cat.hide) 1 else 0
                    val hideBookVal = if (cat.hideBook != null) "'${cat.hideBook}'" else "NULL"
                    db.execSQL(
                        "INSERT OR IGNORE INTO `categories` (`categoryId`, `parentId`, `categoryName`, `iconUrl`, `positionWeight`, `categoryType`, `isCustom`, `hide`, `hideBook`, `createdAt`) " +
                            "VALUES (${cat.categoryId}, ${cat.parentId}, '${cat.categoryName}', '${cat.iconUrl}', ${cat.positionWeight}, ${cat.categoryType}, $isCustom, $hide, $hideBookVal, $now)",
                    )
                }
            }
        })
        .build()

    val transactionDao get() = database.transactionDao()
    val ruleDao get() = database.categoryRuleDao()
    val merchantDao get() = database.merchantCategoryDao()
    val assetDao get() = database.assetDao()
    val transferDao get() = database.transferDao()
    val refundDao get() = database.refundDao()
    val reimbursementDao get() = database.reimbursementDao()
    val lendDao get() = database.lendDao()
    val instalmentDao get() = database.instalmentDao()
    val categoryDao get() = database.categoryDao()
    val bookDao get() = database.bookDao()
    val tagDao get() = database.tagDao()
    val budgetDao get() = database.budgetDao()
    val cycleDao get() = database.cycleDao()
    val moduleBillDao get() = database.moduleBillDao()
    val dreamDao get() = database.dreamDao()

    val ruleRepository = RuleRepository(ruleDao, merchantDao)
    val tagRepository = TagRepository(tagDao)
    val transactionRepository = TransactionRepository(transactionDao, ruleDao, merchantDao, settings, assetDao, transferDao, database)
    val categoryRepository = CategoryRepository(categoryDao)
    val assetRepository = AssetRepository(assetDao, transferDao, transactionDao, database)
    val budgetRepository = BudgetRepository(budgetDao, transactionDao, categoryDao)
    val periodicRepository = PeriodicRepository(cycleDao)
    val periodicTransactionExecutor = PeriodicTransactionExecutor(database, cycleDao, transactionDao, transferDao, assetDao)
    val templateRepository = TemplateRepository(
        moduleBillDao, transactionRepository, categoryRepository, assetRepository,
    )
    val savingsGoalRepository = SavingsGoalRepository(dreamDao)
    val reportRepository = ReportRepository(transactionDao, assetDao, categoryDao)


    val categoryMigrationService = CategoryMigrationService(database)
    val ruleEngine = RuleEngine(ruleDao, merchantDao)
    val billImporter = BillImporter(transactionRepository, ruleEngine)
    val fullBackupService = FullBackupService(database, settings, appContext)

    init {
        // 1. 执行分类迁移（平滑升级历史账单关联）
        applicationScope.launch {
            categoryMigrationService.migrate()
        }

        // 2. 周期记账自动扫描并补流入账
        applicationScope.launch {
            periodicTransactionExecutor.executePendingCycles()
        }

        // 3. 播种内置分类规则：首次启动全量；升级时移除被重新归类的旧映射（RETIRED_RULES），
        // 再增量补种新增关键词（不动用户删过的其他内置规则与自定义规则）
        applicationScope.launch {
            seedCategoryRules()
        }

        // 4. 检查是否有未完成的备份恢复中断状态并自愈
        applicationScope.launch {
            fullBackupService.checkAndRepairPendingRecovery()
        }

        // 5. 异步初始化离线自然语言分类模型
        applicationScope.launch {
            try {
                ClassificationModelService.initializeFromAssets(appContext)
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun seedCategoryRules() {
        val existing = ruleDao.getAll().map { it.keyword to it.category }.toSet()
        Categories.RETIRED_RULES.forEach { (keyword, category) ->
            if ((keyword to category) in existing) ruleDao.deleteBuiltin(keyword, category)
        }
        val missing = Categories.defaultRules()
            .filter { (keyword, category) -> (keyword to category) !in existing }
            .map { (keyword, category) ->
                CategoryRuleEntity(keyword = keyword, category = category, isCustom = false)
            }
        if (missing.isNotEmpty()) {
            ruleDao.insertAll(missing)
        }
    }
}
