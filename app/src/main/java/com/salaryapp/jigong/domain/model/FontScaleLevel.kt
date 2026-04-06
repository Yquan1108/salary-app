package com.salaryapp.jigong.domain.model

enum class FontScaleLevel(
    val storageValue: String,
    val title: String,
    val multiplier: Float
) {
    STANDARD("standard", "标准", 1.0f),
    LARGE("large", "偏大", 1.12f),
    EXTRA_LARGE("extra_large", "超大", 1.24f);

    companion object {
        fun fromStorage(value: String?): FontScaleLevel {
            return entries.firstOrNull { it.storageValue == value } ?: STANDARD
        }
    }
}
