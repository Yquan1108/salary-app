package com.salaryapp.jigong.ui.workrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.data.repository.WorkRecordRepository
import com.salaryapp.jigong.data.repository.WorkerRepository
import com.salaryapp.jigong.data.repository.SiteRepository
import com.salaryapp.jigong.domain.model.Site
import com.salaryapp.jigong.domain.model.WorkRecord
import com.salaryapp.jigong.domain.model.Worker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class WorkRecordFilterState(
    val date: Long? = null,
    val workerId: Long? = null,
    val siteId: Long? = null
)

data class WorkRecordUiState(
    val allRecords: List<WorkRecord> = emptyList(),
    val visibleRecords: List<WorkRecord> = emptyList(),
    val workers: List<Worker> = emptyList(),
    val sites: List<Site> = emptyList(),
    val filterState: WorkRecordFilterState = WorkRecordFilterState(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val pendingDeleteCount: Int? = null,
    val message: String? = null
)

class WorkRecordViewModel(
    private val workRecordRepository: WorkRecordRepository,
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {
    private val filters = MutableStateFlow(WorkRecordFilterState())
    private val _uiState = MutableStateFlow(WorkRecordUiState())
    val uiState: StateFlow<WorkRecordUiState> = _uiState.asStateFlow()

    init {
        combine(
            workRecordRepository.observeWorkRecords(),
            workerRepository.observeWorkers(),
            siteRepository.observeSites(),
            filters
        ) { records, workers, sites, filterState ->
            val visible = records.filter { record ->
                (filterState.date == null || record.workDate == filterState.date) &&
                    (filterState.workerId == null || record.workerId == filterState.workerId) &&
                    (filterState.siteId == null || record.siteId == filterState.siteId)
            }
            val selectedIds = _uiState.value.selectedIds.intersect(visible.map { it.id }.toSet())
            _uiState.value.copy(
                allRecords = records,
                visibleRecords = visible,
                workers = workers,
                sites = sites,
                filterState = filterState,
                selectedIds = selectedIds,
                isSelectionMode = selectedIds.isNotEmpty() || _uiState.value.isSelectionMode
            )
        }.onEach { state ->
            _uiState.value = state.copy(
                isSelectionMode = if (state.selectedIds.isEmpty()) false else state.isSelectionMode
            )
        }.launchIn(viewModelScope)
    }

    fun updateDateFilter(date: Long?) {
        filters.value = filters.value.copy(date = date)
    }

    fun updateWorkerFilter(workerId: Long?) {
        filters.value = filters.value.copy(workerId = workerId)
    }

    fun updateSiteFilter(siteId: Long?) {
        filters.value = filters.value.copy(siteId = siteId)
    }

    fun clearFilters() {
        filters.value = WorkRecordFilterState()
    }

    fun enterSelectionMode(recordId: Long) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedIds = _uiState.value.selectedIds + recordId
        )
    }

    fun toggleSelection(recordId: Long) {
        val selected = _uiState.value.selectedIds.toMutableSet()
        if (!selected.add(recordId)) {
            selected.remove(recordId)
        }
        _uiState.value = _uiState.value.copy(
            selectedIds = selected,
            isSelectionMode = selected.isNotEmpty()
        )
    }

    fun selectAllVisible() {
        _uiState.value = _uiState.value.copy(
            selectedIds = _uiState.value.visibleRecords.map { it.id }.toSet(),
            isSelectionMode = _uiState.value.visibleRecords.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun showDeleteConfirm() {
        if (_uiState.value.selectedIds.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(pendingDeleteCount = _uiState.value.selectedIds.size)
        }
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(pendingDeleteCount = null)
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        viewModelScope.launch {
            val count = workRecordRepository.deleteWorkRecords(ids)
            _uiState.value = _uiState.value.copy(
                selectedIds = emptySet(),
                isSelectionMode = false,
                pendingDeleteCount = null,
                message = "已删除 $count 条记录"
            )
        }
    }

    fun notifySaved() {
        _uiState.value = _uiState.value.copy(message = "已保存")
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

class WorkRecordViewModelFactory(
    private val workRecordRepository: WorkRecordRepository,
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkRecordViewModel(workRecordRepository, workerRepository, siteRepository) as T
    }
}
