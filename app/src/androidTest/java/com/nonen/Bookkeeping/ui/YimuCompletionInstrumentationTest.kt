package com.nonen.Bookkeeping.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nonen.Bookkeeping.BookkeepingApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 复刻收尾阶段的数据链路验证：标签父子关系、资产备注、主题外观持久化。
 * 全部读写真实 Room 数据库与 DataStore，测试结束清理自己创建的数据。
 */
@RunWith(AndroidJUnit4::class)
class YimuCompletionInstrumentationTest {

    @Test
    fun tagsAssetRemarkAndThemeRoundTrip() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.nonen.Bookkeeping", context.packageName)
        val container = (context.applicationContext as BookkeepingApp).container

        // 1. 标签：父子关系与级联删除
        val parentName = "instrumented_parent_${System.currentTimeMillis()}"
        val parent = container.tagRepository.create(parentName)
        val child = container.tagRepository.create("${parentName}_child", parentTagId = parent.tagId)
        assertEquals(parent.tagId, container.tagRepository.getById(child.tagId)?.parentTagId)
        assertEquals(1, container.tagRepository.getChildren(parent.tagId).size)

        container.tagRepository.delete(parent.tagId)
        assertNull(container.tagRepository.getById(parent.tagId))
        assertNull(container.tagRepository.getById(child.tagId))

        // 2. 资产备注与图标字段
        val assetName = "instrumented_asset_${System.currentTimeMillis()}"
        val assetId = container.assetRepository.createAsset(
            name = assetName,
            initialBalance = 12.34,
            remark = "salary card",
            icon = "ic_asset_choice_one",
        )
        val asset = container.assetRepository.getById(assetId)
        assertNotNull(asset)
        assertEquals("salary card", asset?.remark)
        assertEquals("ic_asset_choice_one", asset?.icon)
        container.assetRepository.deleteAsset(assetId)

        // 3. 主题外观持久化
        val originalTheme = container.settings.cachedThemeKey()
        container.settings.setThemeKey("simpleblue")
        assertEquals("simpleblue", container.settings.cachedThemeKey())
        container.settings.setThemeKey(originalTheme)
        assertEquals(originalTheme, container.settings.cachedThemeKey())
    }
}
