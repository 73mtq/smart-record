package com.nonen.Bookkeeping.data.repo

import com.nonen.Bookkeeping.data.db.TagDao
import com.nonen.Bookkeeping.data.db.TagEntity
import com.nonen.Bookkeeping.data.db.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

/**
 * 标签仓库：一级标签与二级标签的增删改、账单关联写入。
 * 标签名称在全局唯一，父子关系由 parentTagId 表达，0 表示一级标签。
 */
class TagRepository(
    private val tagDao: TagDao,
) {

    fun observeAll(): Flow<List<TagEntity>> = tagDao.getAllFlow()

    suspend fun getAll(): List<TagEntity> = tagDao.getAll()

    suspend fun getById(tagId: Long): TagEntity? = tagDao.getById(tagId)

    suspend fun getChildren(parentTagId: Long): List<TagEntity> = tagDao.getByParent(parentTagId)

    suspend fun findByName(name: String): TagEntity? = tagDao.findByName(name.trim())

    /** 新建标签；同名标签已存在时直接返回已有标签。 */
    suspend fun create(name: String, color: Int = 0, parentTagId: Long = 0L): TagEntity {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "标签名称不能为空" }
        tagDao.findByName(trimmed)?.let { return it }
        val siblings = tagDao.getByParent(parentTagId)
        val entity = TagEntity(
            tagName = trimmed,
            positionWeight = (siblings.maxOfOrNull { it.positionWeight } ?: 0) + 1,
            color = color,
            parentTagId = parentTagId,
            updateTime = System.currentTimeMillis(),
        )
        val id = tagDao.insert(entity)
        return entity.copy(tagId = id)
    }

    suspend fun rename(tag: TagEntity, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "标签名称不能为空" }
        tagDao.findByName(trimmed)?.let { existing ->
            if (existing.tagId != tag.tagId) error("标签名称已存在")
        }
        tagDao.update(tag.copy(tagName = trimmed, updateTime = System.currentTimeMillis()))
    }

    suspend fun update(tag: TagEntity) {
        tagDao.update(tag.copy(updateTime = System.currentTimeMillis()))
    }

    /** 删除标签：先删除其下二级标签与全部账单关联，再删除自身。 */
    suspend fun delete(tagId: Long) {
        val children = tagDao.getByParent(tagId)
        children.forEach { child ->
            tagDao.clearRefsForTag(child.tagId)
            tagDao.delete(child)
        }
        tagDao.clearRefsForTag(tagId)
        tagDao.getById(tagId)?.let { tagDao.delete(it) }
    }

    suspend fun setTagsForTransaction(transactionId: Long, tagIds: List<Long>) {
        tagDao.clearTagsForTransaction(transactionId)
        if (tagIds.isEmpty()) return
        tagDao.insertAllRefs(tagIds.distinct().map { TransactionTagCrossRef(transactionId, it) })
    }

    suspend fun getTagIdsForTransaction(transactionId: Long): List<Long> =
        tagDao.getTagsForTransaction(transactionId).map { it.tagId }

    suspend fun getTagsForTransaction(transactionId: Long): List<TagEntity> =
        tagDao.getTagsForTransaction(transactionId)

    suspend fun countRefs(tagId: Long): Int = tagDao.countRefsForTag(tagId)

    suspend fun setHidden(tagId: Long, hidden: Boolean) {
        tagDao.getById(tagId)?.let { tagDao.update(it.copy(hide = hidden, updateTime = System.currentTimeMillis())) }
    }

    /** 重排同级标签的顺序。 */
    suspend fun reorder(parentTagId: Long, ordered: List<TagEntity>) {
        ordered.forEachIndexed { index, tag ->
            tagDao.update(tag.copy(positionWeight = index + 1, updateTime = System.currentTimeMillis()))
        }
    }
}
