package com.salaryapp.jigong.data.repository

import com.salaryapp.jigong.data.local.dao.SiteDao
import com.salaryapp.jigong.data.local.dao.WorkRecordDao
import com.salaryapp.jigong.data.local.dao.WorkerDao
import com.salaryapp.jigong.data.local.entity.WorkRecordEntity
import com.salaryapp.jigong.domain.model.WorkRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkRecordRepository(
    private val workRecordDao: WorkRecordDao,
    private val workerDao: WorkerDao,
    private val siteDao: SiteDao
) {
    fun observeWorkRecords(): Flow<List<WorkRecord>> = workRecordDao.observeWorkRecords().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun saveWorkRecord(input: WorkRecordInput): SaveWorkRecordResult {
        val amount = input.amount.trim()
        if (amount.isBlank()) {
            return SaveWorkRecordResult.ValidationError("金额不能为空")
        }

        val manualWorkerName = input.workerNameSnapshot.trim()
        if (manualWorkerName.isBlank()) {
            return SaveWorkRecordResult.ValidationError("员工不能为空")
        }

        val worker = input.workerId?.let { workerDao.getWorkerById(it) }
        if (input.workerId != null && worker == null) {
            return SaveWorkRecordResult.ValidationError("所选员工不存在")
        }
        val site = input.siteId?.let { siteDao.getSiteById(it) }
        if (input.siteId != null && site == null) {
            return SaveWorkRecordResult.ValidationError("所选工地不存在")
        }
        val finalWorkerName = worker?.name ?: manualWorkerName
        val finalSiteName = site?.siteName ?: input.siteNameSnapshot.blankToNull()

        val now = System.currentTimeMillis()
        if (input.id == null) {
            workRecordDao.insert(
                WorkRecordEntity(
                    workDate = input.workDate,
                    workerId = worker?.id,
                    workerNameSnapshot = finalWorkerName,
                    phoneNumberSnapshot = input.phoneNumberSnapshot.blankToNull(),
                    siteId = site?.id,
                    siteNameSnapshot = finalSiteName,
                    durationText = input.durationText.blankToNull(),
                    unitPriceText = input.unitPriceText.blankToNull(),
                    amount = amount,
                    remark = input.remark.blankToNull(),
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val current = workRecordDao.getById(input.id)
                ?: return SaveWorkRecordResult.ValidationError("未找到要编辑的记录")
            workRecordDao.update(
                current.copy(
                    workDate = input.workDate,
                    workerId = worker?.id,
                    workerNameSnapshot = finalWorkerName,
                    phoneNumberSnapshot = input.phoneNumberSnapshot.blankToNull(),
                    siteId = site?.id,
                    siteNameSnapshot = finalSiteName,
                    durationText = input.durationText.blankToNull(),
                    unitPriceText = input.unitPriceText.blankToNull(),
                    amount = amount,
                    remark = input.remark.blankToNull(),
                    updatedAt = now
                )
            )
        }
        return SaveWorkRecordResult.Success
    }

    suspend fun getWorkRecord(id: Long): WorkRecord? = workRecordDao.getById(id)?.toModel()

    suspend fun deleteWorkRecords(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        return workRecordDao.deleteByIds(ids)
    }
}

data class WorkRecordInput(
    val id: Long? = null,
    val workDate: Long,
    val workerId: Long?,
    val workerNameSnapshot: String,
    val phoneNumberSnapshot: String,
    val siteId: Long?,
    val siteNameSnapshot: String,
    val durationText: String,
    val unitPriceText: String,
    val amount: String,
    val remark: String
)

sealed interface SaveWorkRecordResult {
    data object Success : SaveWorkRecordResult
    data class ValidationError(val message: String) : SaveWorkRecordResult
}

private fun WorkRecordEntity.toModel(): WorkRecord = WorkRecord(
    id = id,
    workDate = workDate,
    workerId = workerId,
    workerNameSnapshot = workerNameSnapshot,
    phoneNumberSnapshot = phoneNumberSnapshot,
    siteId = siteId,
    siteNameSnapshot = siteNameSnapshot,
    durationText = durationText,
    unitPriceText = unitPriceText,
    amount = amount,
    remark = remark,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun String.blankToNull(): String? = trim().ifBlank { null }
