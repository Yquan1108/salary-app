package com.salaryapp.jigong.domain.model

data class Worker(
    val id: Long,
    val name: String,
    val defaultWage: String?,
    val phone: String?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)
