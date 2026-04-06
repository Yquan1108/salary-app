package com.salaryapp.jigong.ui.workrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.core.util.todayMillis
import com.salaryapp.jigong.data.repository.SaveWorkRecordResult
import com.salaryapp.jigong.data.repository.SiteRepository
import com.salaryapp.jigong.data.repository.WorkRecordInput
import com.salaryapp.jigong.data.repository.WorkRecordRepository
import com.salaryapp.jigong.data.repository.WorkerRepository
import com.salaryapp.jigong.domain.model.Site
import com.salaryapp.jigong.domain.model.Worker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class WorkRecordEditorState(
    val id: Long? = null,
    val workDate: Long = todayMillis(),
    val workerId: Long? = null,
    val workerName: String = "",
    val phoneNumber: String = "",
    val siteId: Long? = null,
    val siteName: String = "",
    val durationText: String = "",
    val unitPriceText: String = "",
    val amount: String = "",
    val remark: String = ""
)

data class WorkRecordEditorUiState(
    val editorState: WorkRecordEditorState = WorkRecordEditorState(),
    val workers: List<Worker> = emptyList(),
    val sites: List<Site> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val saveSucceeded: Boolean = false
)

class WorkRecordEditorViewModel(
    private val recordId: Long?,
    private val workRecordRepository: WorkRecordRepository,
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkRecordEditorUiState())
    val uiState: StateFlow<WorkRecordEditorUiState> = _uiState.asStateFlow()

    init {
        combine(
            workerRepository.observeWorkers(),
            siteRepository.observeSites()
        ) { workers, sites -> workers to sites }
            .onEach { (workers, sites) ->
                _uiState.value = _uiState.value.copy(
                    workers = workers,
                    sites = sites,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)

        if (recordId != null) {
            viewModelScope.launch {
                val record = workRecordRepository.getWorkRecord(recordId)
                if (record != null) {
                    _uiState.value = _uiState.value.copy(
                        editorState = WorkRecordEditorState(
                            id = record.id,
                            workDate = record.workDate,
                            workerId = record.workerId,
                            workerName = record.workerNameSnapshot,
                            phoneNumber = record.phoneNumberSnapshot.orEmpty(),
                            siteId = record.siteId,
                            siteName = record.siteNameSnapshot.orEmpty(),
                            durationText = record.durationText.orEmpty(),
                            unitPriceText = record.unitPriceText.orEmpty(),
                            amount = record.amount,
                            remark = record.remark.orEmpty()
                        ),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "未找到要编辑的记录"
                    )
                }
            }
        }
    }

    fun updateEditor(transform: (WorkRecordEditorState) -> WorkRecordEditorState) {
        _uiState.value = _uiState.value.copy(editorState = transform(_uiState.value.editorState))
    }

    fun save(continueAdd: Boolean = false) {
        val editor = _uiState.value.editorState
        viewModelScope.launch {
            when (val result = workRecordRepository.saveWorkRecord(
                WorkRecordInput(
                    id = editor.id,
                    workDate = editor.workDate,
                    workerId = editor.workerId,
                    workerNameSnapshot = editor.workerName,
                    phoneNumberSnapshot = editor.phoneNumber,
                    siteId = editor.siteId,
                    siteNameSnapshot = editor.siteName,
                    durationText = editor.durationText,
                    unitPriceText = editor.unitPriceText,
                    amount = editor.amount,
                    remark = editor.remark
                )
            )) {
                is SaveWorkRecordResult.Success -> {
                    _uiState.value = if (continueAdd) {
                        _uiState.value.copy(
                            editorState = WorkRecordEditorState(
                                workDate = editor.workDate,
                                workerId = editor.workerId,
                                workerName = editor.workerName,
                                phoneNumber = editor.phoneNumber,
                                siteId = editor.siteId,
                                siteName = editor.siteName,
                                unitPriceText = editor.unitPriceText
                            ),
                            message = "已保存，可继续新增",
                            saveSucceeded = false
                        )
                    } else {
                        _uiState.value.copy(saveSucceeded = true, message = "已保存")
                    }
                }

                is SaveWorkRecordResult.ValidationError -> {
                    _uiState.value = _uiState.value.copy(message = result.message)
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

class WorkRecordEditorViewModelFactory(
    private val recordId: Long?,
    private val workRecordRepository: WorkRecordRepository,
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkRecordEditorViewModel(recordId, workRecordRepository, workerRepository, siteRepository) as T
    }
}
