package com.salaryapp.jigong.domain.model

data class Site(
    val id: Long,
    val siteName: String,
    val addressOrAlias: String?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)
