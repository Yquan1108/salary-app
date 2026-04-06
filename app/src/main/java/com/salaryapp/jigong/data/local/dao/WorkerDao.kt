package com.salaryapp.jigong.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.salaryapp.jigong.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY updatedAt DESC, id DESC")
    fun observeWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    suspend fun getWorkerById(id: Long): WorkerEntity?

    @Query("SELECT * FROM workers WHERE TRIM(name) = TRIM(:name) LIMIT 1")
    suspend fun findByName(name: String): WorkerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workerEntity: WorkerEntity): Long

    @Update
    suspend fun update(workerEntity: WorkerEntity)

    @Delete
    suspend fun delete(workerEntity: WorkerEntity)
}
