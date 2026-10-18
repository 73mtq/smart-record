package com.nonen.Bookkeeping.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 资产账户数据访问接口。
 */
@Dao
interface AssetDao {
    @Query("SELECT * FROM assets WHERE hide = 0 ORDER BY positionWeight ASC, assetId ASC")
    fun getAllVisibleFlow(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY positionWeight ASC, assetId ASC")
    suspend fun getAll(): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE assetId = :assetId LIMIT 1")
    suspend fun findById(assetId: Long): AssetEntity?

    @Query("SELECT * FROM assets WHERE assetName = :name LIMIT 1")
    suspend fun findByName(name: String): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<AssetEntity>)

    @Update
    suspend fun update(asset: AssetEntity)

    @Query("UPDATE assets SET assetNumber = assetNumber + :delta, updatedAt = :timestamp WHERE assetId = :assetId")
    suspend fun updateBalance(assetId: Long, delta: Double, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM assets WHERE assetId = :assetId")
    suspend fun deleteById(assetId: Long)
}

/**
 * 资金转账数据访问接口。
 */
@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY time DESC")
    fun getAllFlow(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE transferId = :id LIMIT 1")
    suspend fun findById(id: Long): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: TransferEntity): Long

    @Update
    suspend fun update(transfer: TransferEntity)

    @Delete
    suspend fun delete(transfer: TransferEntity)
}

/**
 * 退款记录数据访问接口。
 */
@Dao
interface RefundDao {
    @Query("SELECT * FROM refunds WHERE billId = :billId LIMIT 1")
    suspend fun findByBillId(billId: Long): RefundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(refund: RefundEntity): Long

    @Update
    suspend fun update(refund: RefundEntity)

    @Delete
    suspend fun delete(refund: RefundEntity)
}

/**
 * 报销业务数据访问接口。
 */
@Dao
interface ReimbursementDao {
    @Query("SELECT * FROM reimbursements WHERE billId = :billId LIMIT 1")
    suspend fun findByBillId(billId: Long): ReimbursementEntity?

    @Query("SELECT * FROM reimbursements WHERE end = 0 ORDER BY createdAt DESC")
    fun getPendingFlow(): Flow<List<ReimbursementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reimbursement: ReimbursementEntity): Long

    @Update
    suspend fun update(reimbursement: ReimbursementEntity)

    @Delete
    suspend fun delete(reimbursement: ReimbursementEntity)
}

/**
 * 借贷流转数据访问接口。
 */
@Dao
interface LendDao {
    @Query("SELECT * FROM lends ORDER BY time DESC")
    fun getAllFlow(): Flow<List<LendEntity>>

    @Query("SELECT * FROM lends WHERE lendId = :id LIMIT 1")
    suspend fun findById(id: Long): LendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lend: LendEntity): Long

    @Update
    suspend fun update(lend: LendEntity)

    @Delete
    suspend fun delete(lend: LendEntity)
}

/**
 * 分期管理数据访问接口。
 */
@Dao
interface InstalmentDao {
    @Query("SELECT * FROM instalments WHERE billId = :billId LIMIT 1")
    suspend fun findByBillId(billId: Long): InstalmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instalment: InstalmentEntity): Long

    @Update
    suspend fun update(instalment: InstalmentEntity)

    @Delete
    suspend fun delete(instalment: InstalmentEntity)
}

/**
 * 账本数据访问接口。
 */
@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY positionWeight ASC, bookId ASC")
    fun getAllFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE bookId = :id LIMIT 1")
    suspend fun findById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultBook(): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)
}

/**
 * 标签数据访问接口。
 */
@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY positionWeight ASC, tagId ASC")
    fun getAllFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE tagName = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRef(ref: TransactionTagCrossRef)

    @Query("SELECT t.* FROM tags t INNER JOIN transaction_tag_refs r ON t.tagId = r.tagId WHERE r.transactionId = :transactionId")
    suspend fun getTagsForTransaction(transactionId: Long): List<TagEntity>

    @Query("DELETE FROM transaction_tag_refs WHERE transactionId = :transactionId")
    suspend fun clearTagsForTransaction(transactionId: Long)
}
