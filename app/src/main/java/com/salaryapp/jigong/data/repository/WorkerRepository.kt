package com.salaryapp.jigong.data.repository

import com.salaryapp.jigong.data.local.dao.WorkerDao
import com.salaryapp.jigong.data.local.entity.WorkerEntity
import com.salaryapp.jigong.domain.model.Worker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkerRepository(
    private val workerDao: WorkerDao
) {
    fun observeWorkers(): Flow<List<Worker>> = workerDao.observeWorkers().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun saveWorker(
        input: WorkerInput,
        allowDuplicate: Boolean = false
    ): SaveWorkerResult {
        val trimmedName = input.name.trim()
        if (trimmedName.isBlank()) {
            return SaveWorkerResult.ValidationError("姓名不能为空")
        }

        val duplicate = workerDao.findByName(trimmedName)
        if (!allowDuplicate && duplicate != null && duplicate.id != input.id) {
            return SaveWorkerResult.DuplicateName
        }

        val now = System.currentTimeMillis()
        if (input.id == null) {
            workerDao.insert(
                WorkerEntity(
                    name = trimmedName,
                    defaultWage = input.defaultWage.blankToNull(),
                    phone = input.phone.blankToNull(),
                    note = input.note.blankToNull(),
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val current = workerDao.getWorkerById(input.id)
                ?: return SaveWorkerResult.ValidationError("未找到要编辑的员工")
            workerDao.update(
                current.copy(
                    name = trimmedName,
                    defaultWage = input.defaultWage.blankToNull(),
                    phone = input.phone.blankToNull(),
                    note = input.note.blankToNull(),
                    updatedAt = now
                )
            )
        }
        return SaveWorkerResult.Success
    }

    suspend fun deleteWorker(id: Long): Boolean {
        val current = workerDao.getWorkerById(id) ?: return false
        workerDao.delete(current)
        return true
    }
}

data class WorkerInput(
    val id: Long? = null,
    val name: String,
    val defaultWage: String,
    val phone: String,
    val note: String
)

sealed interface SaveWorkerResult {
    data object Success : SaveWorkerResult
    data object DuplicateName : SaveWorkerResult
    data class ValidationError(val message: String) : SaveWorkerResult
}

private fun WorkerEntity.toModel(): Worker = Worker(
    id = id,
    name = name,
    defaultWage = defaultWage,
    phone = phone,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun String.blankToNull(): String? = trim().ifBlank { null }
