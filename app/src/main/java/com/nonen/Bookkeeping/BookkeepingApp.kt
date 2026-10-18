package com.nonen.Bookkeeping

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nonen.Bookkeeping.core.Categories
import com.nonen.Bookkeeping.core.DefaultCategories
import com.nonen.Bookkeeping.core.RuleEngine
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.CategoryRuleEntity
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import com.nonen.Bookkeeping.data.repo.RuleRepository
import com.nonen.Bookkeeping.data.repo.TransactionRepository
import com.nonen.Bookkeeping.debug.CaptureDebug
import com.nonen.Bookkeeping.parse.BillImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BookkeepingApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 恢复「抓取调试」开关（src/debug 真实实现；主源集为空操作）
        CaptureDebug.init(this)
    }
}

/**
 * 手动依赖注入容器：项目小，不引入 Hilt。
 */
class AppContainer(val appContext: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settings = SettingsStore(appContext)

    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "bookkeeping.db")
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                db.execSQL("INSERT OR IGNORE INTO `books` (`bookId`, `bookName`, `isDefault`, `positionWeight`, `createdAt`) VALUES (1, '默认账本', 1, 1, $now)")
                db.execSQL("INSERT OR IGNORE INTO `assets` (`assetId`, `assetName`, `assetNumber`, `totalQuota`, `assetType`, `intoTotalAsset`, `hide`, `currency`, `groupName`, `positionWeight`, `createdAt`, `updatedAt`) VALUES (1, '现金', 0.0, 0.0, 1, 1, 0, 'CNY', '现金账户', 1, $now, $now)")
                DefaultCategories.ALL.forEach { cat ->
                    val isCustom = if (cat.isCustom) 1 else 0
                    val hide = if (cat.hide) 1 else 0
                    db.execSQL(
                        "INSERT OR IGNORE INTO `categories` (`categoryId`, `parentId`, `categoryName`, `iconUrl`, `positionWeight`, `categoryType`, `isCustom`, `hide`, `createdAt`) " +
                            "VALUES (${cat.categoryId}, ${cat.parentId}, '${cat.categoryName}', '${cat.iconUrl}', ${cat.positionWeight}, ${cat.categoryType}, $isCustom, $hide, $now)",
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

    val ruleRepository = RuleRepository(ruleDao, merchantDao)
    val transactionRepository = TransactionRepository(transactionDao, ruleDao, merchantDao, settings)
    val categoryRepository = com.nonen.Bookkeeping.data.repo.CategoryRepository(categoryDao)
    val categoryMigrationService = com.nonen.Bookkeeping.data.migration.CategoryMigrationService(database)
    val ruleEngine = RuleEngine(ruleDao, merchantDao)
    val billImporter = BillImporter(transactionRepository, ruleEngine)

    init {
        // 1. 执行分类迁移（平滑升级历史账单关联）
        applicationScope.launch {
            categoryMigrationService.migrate()
        }

        // 2. 播种内置分类规则：首次启动全量；升级时移除被重新归类的旧映射（RETIRED_RULES），
        // 再增量补种新增关键词（不动用户删过的其他内置规则与自定义规则）
        applicationScope.launch {
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
}
