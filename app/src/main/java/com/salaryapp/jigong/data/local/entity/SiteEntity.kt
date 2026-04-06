package com.salaryapp.jigong.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val siteName: String,
    val addressOrAlias: String?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)
