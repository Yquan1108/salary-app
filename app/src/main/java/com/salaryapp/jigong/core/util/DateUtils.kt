package com.salaryapp.jigong.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun todayMillis(): Long = localDateToMillis(LocalDate.now())

fun millisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun localDateToMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun formatDate(millis: Long): String = millisToLocalDate(millis).format(displayFormatter)
