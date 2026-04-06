package com.salaryapp.jigong.domain.model

data class PhotoBatch(
    val id: Long,
    val workDate: Long,
    val siteId: Long?,
    val siteNameSnapshot: String,
    val remark: String?,
    val photoCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<PhotoItem>
)
