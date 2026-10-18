package com.nonen.Bookkeeping.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryRuleEntity::class,
        MerchantCategoryEntity::class,
        AssetEntity::class,
        TransferEntity::class,
        RefundEntity::class,
        ReimbursementEntity::class,
        LendEntity::class,
        InstalmentEntity::class,
        CategoryEntity::class,
        BookEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
    abstract fun assetDao(): AssetDao
    abstract fun transferDao(): TransferDao
    abstract fun refundDao(): RefundDao
    abstract fun reimbursementDao(): ReimbursementDao
    abstract fun lendDao(): LendDao
    abstract fun instalmentDao(): InstalmentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bookDao(): BookDao
    abstract fun tagDao(): TagDao

    companion object {

        /**
         * v1 → v2：
         * 1. 新建 merchant_categories（商户记忆）；
         * 2. category_rules 的唯一约束由 keyword 放宽到 (keyword, category)——
         *    原先同一关键词在收/支两个方向各有一条内置规则，唯一索引只留下先插入的那条，
         *    收入侧的「红包/压岁/利息/赔付」等被静默丢弃。改为整表重建（SQLite 不支持改索引）。
         * 只新增表与重建索引，不改动 transactions，用户流水零风险。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `merchant_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `merchantKey` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isIncome` INTEGER NOT NULL,
                        `hits` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_merchant_categories_merchantKey_isIncome` " +
                        "ON `merchant_categories` (`merchantKey`, `isIncome`)",
                )

                // category_rules：搬数据前先按 (keyword, category) 去重，
                // 避免重建唯一索引时因历史重复行失败
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_rules_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `keyword` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isCustom` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `category_rules_new` (`keyword`, `category`, `isCustom`, `createdAt`)
                    SELECT `keyword`, `category`, `isCustom`, MIN(`createdAt`)
                    FROM `category_rules`
                    GROUP BY `keyword`, `category`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `category_rules`")
                db.execSQL("ALTER TABLE `category_rules_new` RENAME TO `category_rules`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_category_rules_keyword_category` " +
                        "ON `category_rules` (`keyword`, `category`)",
                )
            }
        }

        /**
         * v2 → v3：
         * 为 transactions 表扩充字段：二级分类、账户、标签、交易类型、退款原单 ID、转账目标账户。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `subCategory` TEXT")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `account` TEXT")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `tag` TEXT")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `transactionType` TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `refundOriginalId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `targetAccount` TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_account` ON `transactions` (`account`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_tag` ON `transactions` (`tag`)")
                db.execSQL("UPDATE `transactions` SET `transactionType` = 'INCOME' WHERE `amount` > 0")
            }
        }

        /**
         * v3 → v4：
         * 1. 新建 9 张独立领域实体表：assets, transfers, refunds, reimbursements, lends, instalments, categories, books, tags, transaction_tag_refs；
         * 2. transactions 表增加 assetId, bookId, parentCategoryId, childCategoryId 并建索引；
         * 3. 插入默认账本（bookId=1）与默认资产账户（assetId=1）；
         * 4. 历史 transactions 回填默认 assetId 与 bookId；
         * 5. 播种一木记账 76 项默认分类目录。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 创建 assets 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `assets` (
                        `assetId` INTEGER NOT NULL,
                        `assetName` TEXT NOT NULL,
                        `assetNumber` REAL NOT NULL,
                        `totalQuota` REAL NOT NULL,
                        `assetType` INTEGER NOT NULL,
                        `intoTotalAsset` INTEGER NOT NULL,
                        `hide` INTEGER NOT NULL,
                        `currency` TEXT,
                        `groupName` TEXT,
                        `positionWeight` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`assetId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_assetType` ON `assets` (`assetType`)")

                // 2. 创建 transfers 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transfers` (
                        `transferId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fromAssetId` INTEGER NOT NULL,
                        `toAssetId` INTEGER NOT NULL,
                        `cost` REAL NOT NULL,
                        `toCost` REAL NOT NULL,
                        `serviceCharge` REAL NOT NULL,
                        `billId` INTEGER,
                        `time` INTEGER NOT NULL,
                        `remark` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_fromAssetId` ON `transfers` (`fromAssetId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_toAssetId` ON `transfers` (`toAssetId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_time` ON `transfers` (`time`)")

                // 3. 创建 refunds 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `refunds` (
                        `refundId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `billId` INTEGER NOT NULL,
                        `refundNum` REAL NOT NULL,
                        `refundInfos` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_refunds_billId` ON `refunds` (`billId`)")

                // 4. 创建 reimbursements 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reimbursements` (
                        `reimbursementId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `billId` INTEGER NOT NULL,
                        `assetId` INTEGER NOT NULL,
                        `reimbursementAssetId` INTEGER NOT NULL,
                        `reimbursementNum` REAL NOT NULL,
                        `reimbursementNumbers` TEXT NOT NULL,
                        `end` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reimbursements_billId` ON `reimbursements` (`billId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reimbursements_assetId` ON `reimbursements` (`assetId`)")

                // 5. 创建 lends 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lends` (
                        `lendId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` INTEGER NOT NULL,
                        `assetId` INTEGER NOT NULL,
                        `repaymentAssetId` INTEGER NOT NULL,
                        `number` REAL NOT NULL,
                        `interest` REAL NOT NULL,
                        `billId` INTEGER,
                        `fromCost` REAL NOT NULL,
                        `time` INTEGER NOT NULL,
                        `remark` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_lends_assetId` ON `lends` (`assetId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_lends_repaymentAssetId` ON `lends` (`repaymentAssetId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_lends_time` ON `lends` (`time`)")

                // 6. 创建 instalments 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `instalments` (
                        `instalmentId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `billId` INTEGER NOT NULL,
                        `assetId` INTEGER NOT NULL,
                        `instalmentType` INTEGER NOT NULL,
                        `totalNumber` REAL NOT NULL,
                        `periods` INTEGER NOT NULL,
                        `serviceNumber` REAL NOT NULL,
                        `serviceType` INTEGER NOT NULL,
                        `remainderType` INTEGER NOT NULL,
                        `accountMonth` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_instalments_billId` ON `instalments` (`billId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_instalments_assetId` ON `instalments` (`assetId`)")

                // 7. 创建 categories 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `parentId` INTEGER NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `iconUrl` TEXT NOT NULL,
                        `positionWeight` INTEGER NOT NULL,
                        `categoryType` INTEGER NOT NULL,
                        `isCustom` INTEGER NOT NULL,
                        `hide` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_categoryId_parentId_categoryType` ON `categories` (`categoryId`, `parentId`, `categoryType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentId` ON `categories` (`parentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_categoryType` ON `categories` (`categoryType`)")

                // 8. 创建 books 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `books` (
                        `bookId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookName` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `positionWeight` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // 9. 创建 tags 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `tagId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tagName` TEXT NOT NULL,
                        `positionWeight` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_tagName` ON `tags` (`tagName`)")

                // 10. 创建 transaction_tag_refs 表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_tag_refs` (
                        `transactionId` INTEGER NOT NULL,
                        `tagId` INTEGER NOT NULL,
                        PRIMARY KEY(`transactionId`, `tagId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tag_refs_tagId` ON `transaction_tag_refs` (`tagId`)")

                // 11. 扩充 transactions 表字段
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `assetId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `bookId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `parentCategoryId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `childCategoryId` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_assetId` ON `transactions` (`assetId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_bookId` ON `transactions` (`bookId`)")

                // 12. 播种默认账本与默认资产
                val now = System.currentTimeMillis()
                db.execSQL("INSERT OR IGNORE INTO `books` (`bookId`, `bookName`, `isDefault`, `positionWeight`, `createdAt`) VALUES (1, '默认账本', 1, 1, $now)")
                db.execSQL("INSERT OR IGNORE INTO `assets` (`assetId`, `assetName`, `assetNumber`, `totalQuota`, `assetType`, `intoTotalAsset`, `hide`, `currency`, `groupName`, `positionWeight`, `createdAt`, `updatedAt`) VALUES (1, '现金', 0.0, 0.0, 1, 1, 0, 'CNY', '现金账户', 1, $now, $now)")

                // 13. 回填历史数据
                db.execSQL("UPDATE `transactions` SET `assetId` = 1 WHERE `assetId` IS NULL OR `assetId` = 0")
                db.execSQL("UPDATE `transactions` SET `bookId` = 1 WHERE `bookId` IS NULL OR `bookId` = 0")

                // 14. 播种一木 76 项默认分类
                com.nonen.Bookkeeping.core.DefaultCategories.ALL.forEach { cat ->
                    val isCustom = if (cat.isCustom) 1 else 0
                    val hide = if (cat.hide) 1 else 0
                    db.execSQL(
                        "INSERT OR IGNORE INTO `categories` (`categoryId`, `parentId`, `categoryName`, `iconUrl`, `positionWeight`, `categoryType`, `isCustom`, `hide`, `createdAt`) " +
                            "VALUES (${cat.categoryId}, ${cat.parentId}, '${cat.categoryName}', '${cat.iconUrl}', ${cat.positionWeight}, ${cat.categoryType}, $isCustom, $hide, $now)",
                    )
                }
            }
        }
    }
}
