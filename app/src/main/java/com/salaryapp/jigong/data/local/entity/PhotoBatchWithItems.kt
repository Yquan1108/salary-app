package com.salaryapp.jigong.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PhotoBatchWithItems(
    @Embedded val batch: PhotoBatchEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "batchId"
    )
    val items: List<PhotoItemEntity>
)
