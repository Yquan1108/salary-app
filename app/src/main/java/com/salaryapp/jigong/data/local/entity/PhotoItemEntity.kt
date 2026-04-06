package com.salaryapp.jigong.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_items",
    foreignKeys = [
        ForeignKey(
            entity = PhotoBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("batchId")]
)
data class PhotoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val localUri: String,
    val localPath: String,
    val originalFileName: String?,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?,
    val createdAt: Long
)
