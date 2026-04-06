package com.salaryapp.jigong.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_records")
data class WorkRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workDate: Long,
    val workerId: Long?,
    val workerNameSnapshot: String,
    val phoneNumberSnapshot: String?,
    val siteId: Long?,
    val siteNameSnapshot: String?,
    val durationText: String?,
    val unitPriceText: String?,
    val amount: String,
    val remark: String?,
    val createdAt: Long,
    val updatedAt: Long
)
