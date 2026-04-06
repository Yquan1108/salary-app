package com.salaryapp.jigong.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_batches")
data class PhotoBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workDate: Long,
    val siteId: Long?,
    val siteNameSnapshot: String,
    val remark: String?,
    val photoCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
