package com.salaryapp.jigong.ui.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.data.repository.SaveWorkerResult
import com.salaryapp.jigong.data.repository.WorkerInput
import com.salaryapp.jigong.data.repository.WorkerRepository
import com.salaryapp.jigong.domain.model.Worker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class WorkerEditorState(
    val id: Long? = null,
    val name: String = "",
    val defaultWage: String = "",
    val phone: String = "",
    val note: String = ""
)

data class WorkerUiState(
    val workers: List<Worker> = emptyList(),
    val editorState: WorkerEditorState? = null,
    val deleteTarget: Worker? = null,
    val duplicatePending: Boolean = false,
    val message: String? = null
)

class WorkerViewModel(
    private val workerRepository: WorkerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkerUiState())
    val uiState: StateFlow<WorkerUiState> = _uiState.asStateFlow()

    init {
        workerRepository.observeWorkers()
            .onEach { workers ->
                _uiState.value = _uiState.value.copy(workers = workers)
            }
            .launchIn(viewModelScope)
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            editorState = WorkerEditorState(),
            duplicatePending = false,
            message = null
        )
    }

    fun showEditDialog(worker: Worker) {
        _uiState.value = _uiState.value.copy(
            editorState = WorkerEditorState(
                id = worker.id,
                name = worker.name,
                defaultWage = worker.defaultWage.orEmpty(),
                phone = worker.phone.orEmpty(),
                note = worker.note.orEmpty()
            ),
            duplicatePending = false,
            message = null
        )
    }

    fun updateEditor(transform: (WorkerEditorState) -> WorkerEditorState) {
        val current = _uiState.value.editorState ?: return
        _uiState.value = _uiState.value.copy(editorState = transform(current))
    }

    fun dismissEditor() {
        _uiState.value = _uiState.value.copy(editorState = null, duplicatePending = false)
    }

    fun saveWorker(allowDuplicate: Boolean = false) {
        val editor = _uiState.value.editorState ?: return
        viewModelScope.launch {
            when (
                val result = workerRepository.saveWorker(
                    input = WorkerInput(
                        id = editor.id,
                        name = editor.name,
                        defaultWage = editor.defaultWage,
                        phone = editor.phone,
                        note = editor.note
                    ),
                    allowDuplicate = allowDuplicate
                )
            ) {
                is SaveWorkerResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        editorState = null,
                        duplicatePending = false,
                        message = if (editor.id == null) "员工已保存" else "员工已更新"
                    )
                }
                is SaveWorkerResult.DuplicateName -> {
                    _uiState.value = _uiState.value.copy(duplicatePending = true)
                }
                is SaveWorkerResult.ValidationError -> {
                    _uiState.value = _uiState.value.copy(message = result.message)
                }
            }
        }
    }

    fun requestDelete(worker: Worker) {
        _uiState.value = _uiState.value.copy(deleteTarget = worker)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(deleteTarget = null)
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            workerRepository.deleteWorker(target.id)
            _uiState.value = _uiState.value.copy(deleteTarget = null, message = "员工已删除")
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

class WorkerViewModelFactory(
    private val workerRepository: WorkerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkerViewModel(workerRepository) as T
    }
}
