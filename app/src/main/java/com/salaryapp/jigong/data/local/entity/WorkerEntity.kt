package com.salaryapp.jigong.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultWage: String?,
    val phone: String?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)
