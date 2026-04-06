package com.salaryapp.jigong.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.salaryapp.jigong.data.local.entity.WorkRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkRecordDao {
    @Query("SELECT * FROM work_records ORDER BY workDate DESC, updatedAt DESC, id DESC")
    fun observeWorkRecords(): Flow<List<WorkRecordEntity>>

    @Query("SELECT * FROM work_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkRecordEntity): Long

    @Update
    suspend fun update(entity: WorkRecordEntity)

    @Delete
    suspend fun delete(entity: WorkRecordEntity)

    @Query("DELETE FROM work_records WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int
}
