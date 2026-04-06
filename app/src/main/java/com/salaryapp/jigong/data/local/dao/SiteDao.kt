package com.salaryapp.jigong.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.salaryapp.jigong.data.local.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY updatedAt DESC, id DESC")
    fun observeSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun getSiteById(id: Long): SiteEntity?

    @Query("SELECT * FROM sites WHERE TRIM(siteName) = TRIM(:siteName) LIMIT 1")
    suspend fun findByName(siteName: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(siteEntity: SiteEntity): Long

    @Update
    suspend fun update(siteEntity: SiteEntity)

    @Delete
    suspend fun delete(siteEntity: SiteEntity)
}
