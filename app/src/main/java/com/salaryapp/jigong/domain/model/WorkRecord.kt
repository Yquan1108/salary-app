package com.salaryapp.jigong.domain.model

data class WorkRecord(
    val id: Long,
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
