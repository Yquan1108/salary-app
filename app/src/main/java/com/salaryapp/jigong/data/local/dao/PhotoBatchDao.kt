package com.salaryapp.jigong.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.salaryapp.jigong.data.local.entity.PhotoBatchEntity
import com.salaryapp.jigong.data.local.entity.PhotoBatchWithItems
import com.salaryapp.jigong.data.local.entity.PhotoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoBatchDao {
    @Transaction
    @Query("SELECT * FROM photo_batches ORDER BY workDate DESC, createdAt DESC")
    fun observePhotoBatches(): Flow<List<PhotoBatchWithItems>>

    @Insert
    suspend fun insertBatch(entity: PhotoBatchEntity): Long

    @Insert
    suspend fun insertItems(entities: List<PhotoItemEntity>)

    @Transaction
    @Query("SELECT * FROM photo_batches WHERE id = :batchId")
    suspend fun getBatchWithItems(batchId: Long): PhotoBatchWithItems?

    @Query("DELETE FROM photo_batches WHERE id IN (:ids)")
    suspend fun deleteBatches(ids: List<Long>): Int
}
