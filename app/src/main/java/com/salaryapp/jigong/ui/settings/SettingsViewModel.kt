package com.salaryapp.jigong.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.data.repository.PhotoRepository
import com.salaryapp.jigong.data.repository.SiteRepository
import com.salaryapp.jigong.data.repository.WorkRecordRepository
import com.salaryapp.jigong.data.repository.WorkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SettingsUiState(
    val workerCount: Int = 0,
    val siteCount: Int = 0,
    val workRecordCount: Int = 0,
    val photoBatchCount: Int = 0
)

class SettingsViewModel(
    workerRepository: WorkerRepository,
    siteRepository: SiteRepository,
    workRecordRepository: WorkRecordRepository,
    photoRepository: PhotoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            workerRepository.observeWorkers(),
            siteRepository.observeSites(),
            workRecordRepository.observeWorkRecords(),
            photoRepository.observePhotoBatches()
        ) { workers, sites, records, photoBatches ->
            SettingsUiState(
                workerCount = workers.size,
                siteCount = sites.size,
                workRecordCount = records.size,
                photoBatchCount = photoBatches.size
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }
}

class SettingsViewModelFactory(
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository,
    private val workRecordRepository: WorkRecordRepository,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            workerRepository = workerRepository,
            siteRepository = siteRepository,
            workRecordRepository = workRecordRepository,
            photoRepository = photoRepository
        ) as T
    }
}
