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
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

/**
 * JVM 纯函数全量备份序列化与反序列化测试。
 * 遵循无 Fake DAO、无 Mock 规则，验证数据结构往返一致性与摘要算法。
 */
class FullBackupServiceTest {

    @Test
    fun testSerializeAndDeserializeDataRoundtrip() {
        val now = 1771234567890L

        val originalTransactions = listOf(
            TransactionEntity(
                id = 101L,
                amount = -65.50,
                category = "餐饮美食",
                subCategory = "晚餐",
                note = "聚餐AA",
                merchant = "望湘园",
                timestamp = now,
                source = "manual",
                account = "微信零钱",
                tag = "聚餐",
                transactionType = "EXPENSE",
                refundOriginalId = null,
                targetAccount = null,
                assetId = 2L,
                bookId = 1L,
                parentCategoryId = 1,
                childCategoryId = 11,
                rawData = "raw",
                hash = "hash_101",
                createdAt = now,
                updatedAt = now,
            ),
        )

        val originalAssets = listOf(
            AssetEntity(
                assetId = 2L,
                assetName = "微信钱包",
                assetNumber = 1250.0,
                totalQuota = 0.0,
                assetType = 1,
                intoTotalAsset = true,
                hide = false,
                currency = "CNY",
                groupName = "电子钱包",
                positionWeight = 2,
                createdAt = now,
                updatedAt = now,
            ),
        )

        val originalBooks = listOf(
            BookEntity(bookId = 1L, bookName = "日常账本", isDefault = true, positionWeight = 1, createdAt = now),
        )

        val originalCategories = listOf(
            CategoryEntity(
                id = 1L,
                categoryId = 1,
                parentId = 0,
                categoryName = "餐饮美食",
                iconUrl = "icon_food",
                positionWeight = 1,
                categoryType = 0,
                isCustom = false,
                hide = false,
                hideBook = null,
                createdAt = now,
            ),
        )

        val originalTransfers = listOf(
            TransferEntity(
                transferId = 1L,
                fromAssetId = 1L,
                toAssetId = 2L,
                cost = 200.0,
                toCost = 200.0,
                serviceCharge = 0.0,
                billId = 101L,
                time = now,
                remark = "充值零钱",
                createdAt = now,
            ),
        )

        val originalRefunds = listOf(
            RefundEntity(
                refundId = 1L,
                billId = 101L,
                refundNum = 15.0,
                refundInfos = "[]",
                createdAt = now,
            ),
        )

        val originalReimbursements = listOf(
            ReimbursementEntity(
                reimbursementId = 1L,
                billId = 101L,
                assetId = 2L,
                reimbursementAssetId = 2L,
                reimbursementNum = 50.0,
                reimbursementNumbers = "[]",
                end = false,
                createdAt = now,
            ),
        )

        val originalLends = listOf(
            LendEntity(
                lendId = 1L,
                type = 1,
                assetId = 2L,
                repaymentAssetId = 1L,
                number = 500.0,
                interest = 0.0,
                billId = 101L,
                fromCost = 500.0,
                time = now,
                remark = "借款",
                createdAt = now,
            ),
        )

        val originalInstalments = listOf(
            InstalmentEntity(
                instalmentId = 1L,
                billId = 101L,
                assetId = 2L,
                instalmentType = 1,
                totalNumber = 1200.0,
                periods = 12,
                serviceNumber = 10.0,
                serviceType = 0,
                remainderType = 0,
                accountMonth = "2026-10",
                createdAt = now,
            ),
        )

        val originalTags = listOf(
            TagEntity(tagId = 1L, tagName = "工作", positionWeight = 1, createdAt = now),
        )

        val originalTagRefs = listOf(
            TransactionTagCrossRef(transactionId = 101L, tagId = 1L),
        )

        val originalBudgets = listOf(
            BudgetEntity(
                budgetId = 1L,
                bookId = 1L,
                budgetName = "月度总预算",
                year = 2026,
                month = 10,
                type = BudgetEntity.TYPE_MONTH,
                startTime = now,
                endTime = now + 86400000L * 30,
                num = 5000.0,
                addNum = 0.0,
                positionWeight = 1,
                updateTime = now,
            ),
        )

        val originalCategoryBudgets = listOf(
            CategoryBudgetEntity(
                categoryBudgetId = 1L,
                budgetId = 1L,
                bookId = 1L,
                year = 2026,
                month = 10,
                parentCategory = 1,
                childCategory = 11,
                num = 1500.0,
                addNum = 0.0,
                positionWeight = 1,
                updateTime = now,
            ),
        )

        val originalCycles = listOf(
            CycleEntity(
                cycleId = 1L,
                bookId = 1L,
                cycleType = CycleEntity.CYCLE_TYPE_BILL,
                cost = 30.0,
                serviceCharge = 0.0,
                parentCategoryId = 1,
                childCategoryId = 11,
                assetId = 2L,
                dateMode = CycleEntity.MODE_EVERY_MONTH,
                monthDay = 10,
                remark = "会员费",
                addTime = now,
                nextTime = now + 86400000L * 30,
                updateTime = now,
            ),
        )

        val originalCycleLogs = listOf(
            CycleLogEntity(
                logId = 1L,
                cycleId = 1L,
                dedupKey = "cycle_1_20261010",
                transactionId = 101L,
                transferId = null,
                executedTime = now,
                amount = 30.0,
                status = 1,
            ),
        )

        val originalModuleBills = listOf(
            ModuleBillEntity(
                moduleBillId = 1L,
                bookId = 1L,
                moduleType = 0,
                templateName = "常点午餐",
                cost = 25.0,
                parentCategoryId = 1,
                childCategoryId = 11,
                assetId = 2L,
                remark = "外卖",
                updateTime = now,
            ),
        )

        val originalDreams = listOf(
            DreamEntity(
                dreamId = 1L,
                dreamName = "买新电脑",
                iconUrl = "icon_laptop",
                totalNum = 15000.0,
                startNum = 2000.0,
                addNum = 1000.0,
                startTime = now,
                endTime = now + 86400000L * 180,
                updateTime = now,
            ),
        )

        val originalDreamLogs = listOf(
            DreamLogEntity(
                logId = 1L,
                dreamId = 1L,
                amount = 1000.0,
                time = now,
                remark = "月度存入",
            ),
        )

        val originalCategoryRules = listOf(
            CategoryRuleEntity(id = 1L, keyword = "海底捞", category = "餐饮美食", isCustom = true, createdAt = now),
        )

        val originalMerchantCategories = listOf(
            MerchantCategoryEntity(id = 1L, merchantKey = "美团", category = "餐饮美食", isIncome = false, hits = 5, updatedAt = now),
        )

        val originalSettings = mapOf(
            "autoRecordEnabled" to "true",
            "listenScope" to "ALL",
            "notifyOnRecord" to "true",
            "learnOnEdit" to "true",
            "themeMode" to "DARK",
            "dynamicColor" to "true",
            "hideAmount" to "false",
            "appLockEnabled" to "true",
            "fontOption" to "DEFAULT",
        )

        // 直接调用 internal serializeData 与 deserializeData 进行纯数据层核验
        val jsonString = FullBackupService.serializeData(
            originalTransactions, originalAssets, originalBooks, originalCategories,
            originalTransfers, originalRefunds, originalReimbursements, originalLends,
            originalInstalments, originalTags, originalTagRefs, originalBudgets,
            originalCategoryBudgets, originalCycles, originalCycleLogs, originalModuleBills,
            originalDreams, originalDreamLogs, originalCategoryRules, originalMerchantCategories,
            originalSettings,
        )

        assertTrue("序列化 JSON 必须非空且有效", jsonString.isNotEmpty())
        val jsonObj = JSONObject(jsonString)
        val restoredData = FullBackupService.deserializeData(jsonObj)

        // 验证 20 表领域实体无损往返
        assertEquals(1, restoredData.transactions.size)
        assertEquals(originalTransactions[0].id, restoredData.transactions[0].id)
        assertEquals(originalTransactions[0].amount, restoredData.transactions[0].amount, 0.0001)
        assertEquals(originalTransactions[0].category, restoredData.transactions[0].category)
        assertEquals(originalTransactions[0].merchant, restoredData.transactions[0].merchant)

        assertEquals(1, restoredData.assets.size)
        assertEquals("微信钱包", restoredData.assets[0].assetName)
        assertEquals(1250.0, restoredData.assets[0].assetNumber, 0.0001)

        assertEquals(1, restoredData.books.size)
        assertEquals("日常账本", restoredData.books[0].bookName)

        assertEquals(1, restoredData.categories.size)
        assertEquals("餐饮美食", restoredData.categories[0].categoryName)

        assertEquals(1, restoredData.transfers.size)
        assertEquals(200.0, restoredData.transfers[0].cost, 0.0001)

        assertEquals(1, restoredData.refunds.size)
        assertEquals(15.0, restoredData.refunds[0].refundNum, 0.0001)

        assertEquals(1, restoredData.reimbursements.size)
        assertEquals(50.0, restoredData.reimbursements[0].reimbursementNum, 0.0001)

        assertEquals(1, restoredData.lends.size)
        assertEquals(500.0, restoredData.lends[0].number, 0.0001)

        assertEquals(1, restoredData.instalments.size)
        assertEquals(1200.0, restoredData.instalments[0].totalNumber, 0.0001)

        assertEquals(1, restoredData.tags.size)
        assertEquals("工作", restoredData.tags[0].tagName)

        assertEquals(1, restoredData.transactionTags.size)
        assertEquals(101L, restoredData.transactionTags[0].transactionId)

        assertEquals(1, restoredData.budgets.size)
        assertEquals(5000.0, restoredData.budgets[0].num, 0.0001)

        assertEquals(1, restoredData.categoryBudgets.size)
        assertEquals(1500.0, restoredData.categoryBudgets[0].num, 0.0001)

        assertEquals(1, restoredData.cycles.size)
        assertEquals(30.0, restoredData.cycles[0].cost, 0.0001)

        assertEquals(1, restoredData.cycleLogs.size)
        assertEquals("cycle_1_20261010", restoredData.cycleLogs[0].dedupKey)

        assertEquals(1, restoredData.moduleBills.size)
        assertEquals("常点午餐", restoredData.moduleBills[0].templateName)

        assertEquals(1, restoredData.dreams.size)
        assertEquals("买新电脑", restoredData.dreams[0].dreamName)

        assertEquals(1, restoredData.dreamLogs.size)
        assertEquals(1000.0, restoredData.dreamLogs[0].amount, 0.0001)

        assertEquals(1, restoredData.categoryRules.size)
        assertEquals("海底捞", restoredData.categoryRules[0].keyword)

        assertEquals(1, restoredData.merchantCategories.size)
        assertEquals("美团", restoredData.merchantCategories[0].merchantKey)

        // 验证全部 9 项设置无损往返
        assertEquals(9, restoredData.settings.size)
        assertEquals("DARK", restoredData.settings["themeMode"])
        assertEquals("true", restoredData.settings["dynamicColor"])
        assertEquals("false", restoredData.settings["hideAmount"])
        assertEquals("true", restoredData.settings["appLockEnabled"])
        assertEquals("DEFAULT", restoredData.settings["fontOption"])
    }

    @Test
    fun testChecksumCalculationAccuracy() {
        val testBytes = "SmartRecordBackupIntegrityVerification".toByteArray(StandardCharsets.UTF_8)
        val checksum = FullBackupService.calculateSha256(testBytes)

        assertNotNull(checksum)
        assertEquals(64, checksum.length)
        // 验证相同输入计算结果确定性
        val checksumRepeat = FullBackupService.calculateSha256(testBytes)
        assertEquals(checksum, checksumRepeat)
    }
}
