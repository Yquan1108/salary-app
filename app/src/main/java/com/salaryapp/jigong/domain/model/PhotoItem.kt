package com.salaryapp.jigong.domain.model

data class PhotoItem(
    val id: Long,
    val batchId: Long,
    val localUri: String,
    val localPath: String,
    val originalFileName: String?,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?,
    val createdAt: Long
)
