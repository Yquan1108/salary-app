package com.salaryapp.jigong.data.repository

import com.salaryapp.jigong.data.local.dao.SiteDao
import com.salaryapp.jigong.data.local.entity.SiteEntity
import com.salaryapp.jigong.domain.model.Site
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SiteRepository(
    private val siteDao: SiteDao
) {
    fun observeSites(): Flow<List<Site>> = siteDao.observeSites().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun saveSite(
        input: SiteInput,
        allowDuplicate: Boolean = false
    ): SaveSiteResult {
        val trimmedName = input.siteName.trim()
        if (trimmedName.isBlank()) {
            return SaveSiteResult.ValidationError("工地名称不能为空")
        }

        val duplicate = siteDao.findByName(trimmedName)
        if (!allowDuplicate && duplicate != null && duplicate.id != input.id) {
            return SaveSiteResult.DuplicateName
        }

        val now = System.currentTimeMillis()
        if (input.id == null) {
            siteDao.insert(
                SiteEntity(
                    siteName = trimmedName,
                    addressOrAlias = input.addressOrAlias.blankToNull(),
                    note = input.note.blankToNull(),
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val current = siteDao.getSiteById(input.id)
                ?: return SaveSiteResult.ValidationError("未找到要编辑的工地")
            siteDao.update(
                current.copy(
                    siteName = trimmedName,
                    addressOrAlias = input.addressOrAlias.blankToNull(),
                    note = input.note.blankToNull(),
                    updatedAt = now
                )
            )
        }
        return SaveSiteResult.Success
    }

    suspend fun deleteSite(id: Long): Boolean {
        val current = siteDao.getSiteById(id) ?: return false
        siteDao.delete(current)
        return true
    }
}

data class SiteInput(
    val id: Long? = null,
    val siteName: String,
    val addressOrAlias: String,
    val note: String
)

sealed interface SaveSiteResult {
    data object Success : SaveSiteResult
    data object DuplicateName : SaveSiteResult
    data class ValidationError(val message: String) : SaveSiteResult
}

private fun SiteEntity.toModel(): Site = Site(
    id = id,
    siteName = siteName,
    addressOrAlias = addressOrAlias,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun String.blankToNull(): String? = trim().ifBlank { null }
