package com.nonen.Bookkeeping.core.nlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime

class ClassificationModelServiceTest {

    private lateinit var service: ClassificationModelService

    @Before
    fun setUp() {
        val modelFile = File("src/main/assets/classification-model.ser")
        val patternFile = File("src/main/assets/TimeExp.m")

        assertTrue("classification-model.ser 必须存在", modelFile.exists())
        assertTrue("TimeExp.m 必须存在", patternFile.exists())

        val model = FileInputStream(modelFile).use {
            ClassificationModelService.loadModelFromStream(it)
        }
        val pattern = FileInputStream(patternFile).use {
            ClassificationModelService.loadPatternFromStream(it)
        }

        assertNotNull("Model 必须反序列化成功", model)
        assertNotNull("TimePattern 必须反序列化成功", pattern)

        service = ClassificationModelService(model, pattern)
    }

    @Test
    fun `model structure and catalogs are correctly loaded`() {
        val model = service.svmModel
        assertNotNull(model.catalog)
        assertEquals(70, model.catalog.size)

        assertNotNull(model.wordIdTrie)
        assertTrue(model.wordIdTrie.size() > 0)

        assertNotNull(model.featureWeighter)
        assertNotNull(model.svmModel)
        assertEquals(70, model.svmModel.nrClass)

        println("Loaded 70 categories, wordIdTrie size: ${model.wordIdTrie.size()}, nrClass: ${model.svmModel.nrClass}")
    }

    @Test
    fun `predict fast food text classifies to food category`() {
        val text = "肯德基汉堡"
        val predictions = service.predict(text)
        assertTrue("预测结果不应为空", predictions.isNotEmpty())

        val top = predictions.maxByOrNull { it.value }
        assertNotNull(top)
        println("Predictions for '$text': top=${top?.key} (${top?.value})")
        assertTrue("应当归类为食品餐饮相关分类", top?.key?.startsWith("食品餐饮") == true)
    }

    @Test
    fun `predict taxi ride text classifies to traffic taxi`() {
        val text = "滴滴打车出行"
        val predictions = service.predict(text)
        assertTrue("预测结果不应为空", predictions.isNotEmpty())

        val top = predictions.maxByOrNull { it.value }
        assertNotNull(top)
        println("Predictions for '$text': top=${top?.key} (${top?.value})")
        assertTrue("应当归类为出行交通相关分类", top?.key?.startsWith("出行交通") == true)
    }

    @Test
    fun `unrecognized text retains zero confidence and other category`() {
        val text = "🧩🛰️🧬"
        assertTrue(service.predict(text).isEmpty())

        val result = service.parseQuickRecord("${text}18元", LocalDateTime.of(2026, 10, 19, 8, 0))
        assertEquals(18.0, result.amount ?: 0.0, 0.001)
        assertEquals("其他", result.category)
        assertNull(result.subCategory)
        assertEquals(0.0, result.confidence, 0.0)
        assertEquals(text, result.remark)
    }

    @Test
    fun `accepted category retains real model maximum probability`() {
        val text = "滴滴打车出行"
        val maximum = service.predict(text).values.maxOrNull()!!
        assertTrue("真实出行语句必须满足分类门槛", maximum >= 0.015)

        val result = service.parseQuickRecord("${text}花了28元", LocalDateTime.of(2026, 10, 19, 8, 0))
        assertEquals("出行交通", result.category)
        assertEquals("打车", result.subCategory)
        assertEquals(maximum, result.confidence, 0.0)
    }

    @Test
    fun `parseQuickRecord extracts money, time and category correctly`() {
        val baseTime = LocalDateTime.of(2026, 10, 18, 12, 0, 0)
        val result = service.parseQuickRecord("今天中午吃肯德基汉堡花了35元", baseTime)

        println("QuickRecord result: amount=${result.amount}, category=${result.category}, subCategory=${result.subCategory}, time=${result.dateTime}, confidence=${result.confidence}")

        assertEquals(35.0, result.amount ?: 0.0, 0.001)
        assertEquals("食品餐饮", result.category)
        assertTrue(result.subCategory == "午餐" || result.subCategory == "快餐" || result.subCategory == "休闲零食")
        assertEquals(2026, result.dateTime.year)
        assertEquals(10, result.dateTime.monthValue)
        assertEquals(18, result.dateTime.dayOfMonth)
        assertEquals(12, result.dateTime.hour)
    }

    @Test
    fun `parseQuickRecord extracts salary income correctly`() {
        val baseTime = LocalDateTime.of(2026, 10, 18, 10, 0, 0)
        val result = service.parseQuickRecord("发工资8000元", baseTime)

        println("QuickRecord result: amount=${result.amount}, isIncome=${result.isIncome}, category=${result.category}, subCategory=${result.subCategory}")

        assertEquals(8000.0, result.amount ?: 0.0, 0.001)
        assertTrue("应判定为收入", result.isIncome)
        assertEquals("收入", result.category)
        assertEquals("工资", result.subCategory)
    }

    @Test
    fun `resolveTimeExp accurately parses relative dates and specific hours and minutes`() {
        val base = LocalDateTime.of(2026, 10, 18, 12, 0, 0)

        // 1. 相对日期（大前天、前天、大后天、后天）
        val tBigPrev = ClassificationModelService.resolveTimeExp("大前天买了本书", base)
        assertEquals(2026, tBigPrev.year)
        assertEquals(10, tBigPrev.monthValue)
        assertEquals(15, tBigPrev.dayOfMonth)

        val tPrev = ClassificationModelService.resolveTimeExp("前天超市买菜", base)
        assertEquals(16, tPrev.dayOfMonth)

        val tBigNext = ClassificationModelService.resolveTimeExp("大后天要买机票", base)
        assertEquals(21, tBigNext.dayOfMonth)

        val tNext = ClassificationModelService.resolveTimeExp("后天发工资", base)
        assertEquals(20, tNext.dayOfMonth)

        // 2. 具体时刻：昨天下午3点20分
        val tSpecific1 = ClassificationModelService.resolveTimeExp("昨天下午3点20分喝奶茶", base)
        assertEquals(17, tSpecific1.dayOfMonth)
        assertEquals(15, tSpecific1.hour)
        assertEquals(20, tSpecific1.minute)

        // 3. 具体时刻：上午9点半
        val tSpecific2 = ClassificationModelService.resolveTimeExp("上午9点半吃早餐", base)
        assertEquals(18, tSpecific2.dayOfMonth)
        assertEquals(9, tSpecific2.hour)
        assertEquals(30, tSpecific2.minute)

        // 4. 泛时段：昨天下午
        val tPeriod = ClassificationModelService.resolveTimeExp("昨天下午喝咖啡", base)
        assertEquals(17, tPeriod.dayOfMonth)
        assertEquals(15, tPeriod.hour)
        assertEquals(30, tPeriod.minute)
    }

    @Test
    fun `parseQuickRecord handles full statements with combined time expressions and amounts`() {
        val baseDateTime = LocalDateTime.of(2026, 10, 18, 17, 0, 0)

        // 1. 昨天下午3点20分买药18元 -> 10月17日 15:20 / 金额 18.0
        val r1 = service.parseQuickRecord("昨天下午3点20分买药18元", baseDateTime)
        assertEquals(18.0, r1.amount ?: 0.0, 0.001)
        assertEquals(2026, r1.dateTime.year)
        assertEquals(10, r1.dateTime.monthValue)
        assertEquals(17, r1.dateTime.dayOfMonth)
        assertEquals(15, r1.dateTime.hour)
        assertEquals(20, r1.dateTime.minute)
        assertEquals("健康医疗", r1.category)

        // 2. 今天上午9点半吃早餐12元 -> 10月18日 09:30 / 金额 12.0
        val r2 = service.parseQuickRecord("今天上午9点半吃早餐12元", baseDateTime)
        assertEquals(12.0, r2.amount ?: 0.0, 0.001)
        assertEquals(2026, r2.dateTime.year)
        assertEquals(10, r2.dateTime.monthValue)
        assertEquals(18, r2.dateTime.dayOfMonth)
        assertEquals(9, r2.dateTime.hour)
        assertEquals(30, r2.dateTime.minute)
        assertEquals("吃早餐", r2.remark)
        assertEquals("食品餐饮", r2.category)
        assertEquals("早餐", r2.subCategory)

        // 3. 大前天晚上8点喝咖啡35元 -> 10月15日 20:00 / 金额 35.0
        val r3 = service.parseQuickRecord("大前天晚上8点喝咖啡35元", baseDateTime)
        assertEquals(35.0, r3.amount ?: 0.0, 0.001)
        assertEquals(2026, r3.dateTime.year)
        assertEquals(10, r3.dateTime.monthValue)
        assertEquals(15, r3.dateTime.dayOfMonth)
        assertEquals(20, r3.dateTime.hour)
        assertEquals(0, r3.dateTime.minute)
        assertTrue("大前天喝咖啡应归为食品餐饮或休闲娱乐", r3.category == "食品餐饮" || r3.category == "休闲娱乐")
    }
}
