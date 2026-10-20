package com.nonen.Bookkeeping.core.nlp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * 手机真实 Android 运行环境下的本地离线模型加载与速记推断验证。
 * 纯只读验证 assets 中的模型资源反序列化与真实推断，严格不触碰任何数据库与应用设置。
 */
@RunWith(AndroidJUnit4::class)
class AndroidClassificationModelTest {

    @Test
    fun testThresholdAndBreakfastNounInAndroidRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = ClassificationModelService.initializeFromAssets(context)
        val base = LocalDateTime.of(2026, 10, 19, 17, 0)

        val unknownText = "🧩🛰️🧬"
        assertTrue(service.predict(unknownText).isEmpty())
        val unknown = service.parseQuickRecord("${unknownText}18元", base)
        assertEquals("其他", unknown.category)
        assertNull(unknown.subCategory)
        assertEquals(0.0, unknown.confidence, 0.0)
        assertEquals(18.0, unknown.amount ?: 0.0, 0.001)

        val breakfast = service.parseQuickRecord("今天上午9点半吃早餐12元", base)
        assertEquals("食品餐饮", breakfast.category)
        assertEquals("早餐", breakfast.subCategory)
        assertEquals("吃早餐", breakfast.remark)
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 30), breakfast.dateTime)
        assertEquals(12.0, breakfast.amount ?: 0.0, 0.001)
        assertTrue(breakfast.confidence >= 0.015)
        assertEquals(service.predict(breakfast.remark).values.maxOrNull()!!, breakfast.confidence, 0.0)
    }

    @Test
    fun testModelLoadsAndPredictsInAndroidRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // 验证从 Android 真实 assets 目录初始化加载
        val service = ClassificationModelService.initializeFromAssets(context)
        assertNotNull("ClassificationModelService should initialize successfully on Android", service)
        assertNotNull("SVM Model must be present", service.svmModel)
        assertEquals(70, service.svmModel.catalog.size)

        // 真实中文消费语句推断 1：餐饮
        val resultFood = service.parseQuickRecord("中午吃肯德基汉堡花了35元")
        assertEquals(35.0, resultFood.amount ?: 0.0, 0.001)
        assertEquals("食品餐饮", resultFood.category)
        assertTrue(resultFood.confidence > 0.5)

        // 真实中文消费语句推断 2：打车出行
        val resultTraffic = service.parseQuickRecord("滴滴打车出行花了28元")
        assertEquals(28.0, resultTraffic.amount ?: 0.0, 0.001)
        assertEquals("出行交通", resultTraffic.category)
        assertEquals("打车", resultTraffic.subCategory)
        assertTrue(resultTraffic.confidence > 0.8)

        // 真实中文收入语句推断：工资
        val resultIncome = service.parseQuickRecord("公司发工资8000元")
        assertEquals(8000.0, resultIncome.amount ?: 0.0, 0.001)
        assertTrue(resultIncome.isIncome)
        assertEquals("收入", resultIncome.category)
    }
}
