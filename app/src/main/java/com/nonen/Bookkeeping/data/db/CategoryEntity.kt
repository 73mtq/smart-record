package com.nonen.Bookkeeping.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分类目录持久化实体（对标一木记账 76 项标准分类及自定义扩展）。
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["categoryId", "parentId", "categoryType"], unique = true),
        Index("parentId"),
        Index("categoryType"),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 业务分类 ID（一级分类为 1~10，二级分类为三位数如 101、801） */
    val categoryId: Int,
    /** 父级分类 ID（0 代表一级分类，大于 0 为所属一级分类的 categoryId） */
    val parentId: Int,
    /** 分类名称 */
    val categoryName: String,
    /** 图标资源名称 */
    val iconUrl: String,
    /** 排序权重 */
    val positionWeight: Int,
    /** 收支方向：0 为支出，1 为收入 */
    val categoryType: Int,
    /** 是否为用户自定义分类 */
    val isCustom: Boolean = false,
    /** 是否全局隐藏 */
    val hide: Boolean = false,
    /** 在特定账本中隐藏的账本 ID 列表（以英文逗号分隔，如 "1,2"），对标原应用 List<Long> hideBook */
    val hideBook: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun isHiddenInBook(bookId: Long): Boolean {
        if (hide) return true
        val list = hideBook?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
        return bookId in list
    }

    fun withBookHidden(bookId: Long, hidden: Boolean): CategoryEntity {
        val currentList = hideBook?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.toMutableSet() ?: mutableSetOf()
        if (hidden) {
            currentList.add(bookId)
        } else {
            currentList.remove(bookId)
        }
        val newHideBook = if (currentList.isEmpty()) null else currentList.joinToString(",")
        return copy(hideBook = newHideBook)
    }
}

/**
 * 统一分类选择值对象。
 */
data class CategorySelection(
    val parentId: Int,
    val childId: Int,
)
