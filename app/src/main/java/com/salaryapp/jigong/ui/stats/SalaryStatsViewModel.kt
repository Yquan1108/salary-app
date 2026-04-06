package com.salaryapp.jigong.ui.stats

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.core.util.formatDate
import com.salaryapp.jigong.core.util.todayMillis
import com.salaryapp.jigong.data.repository.ExportSalaryStatsResult
import com.salaryapp.jigong.data.repository.SalaryStatsExportFilters
import com.salaryapp.jigong.data.repository.SalaryStatsExportRepository
import com.salaryapp.jigong.data.repository.SalaryStatsExportRow
import com.salaryapp.jigong.data.repository.SalaryStatsExportSummary
import com.salaryapp.jigong.data.repository.SiteRepository
import com.salaryapp.jigong.data.repository.WorkRecordRepository
import com.salaryapp.jigong.data.repository.WorkerRepository
import com.salaryapp.jigong.domain.model.Site
import com.salaryapp.jigong.domain.model.WorkRecord
import com.salaryapp.jigong.domain.model.Worker
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SalaryStatsFilterState(
    val workerKeyword: String = "",
    val siteKeyword: String = "",
    val startDate: Long = todayMillis(),
    val endDate: Long = todayMillis()
)

data class SalaryStatsRowUiModel(
    val id: Long,
    val workDate: String,
    val workerName: String,
    val siteName: String,
    val duration: String,
    val phoneNumber: String,
    val unitPrice: String,
    val amount: String
)

data class SalaryStatsUiState(
    val workers: List<Worker> = emptyList(),
    val sites: List<Site> = emptyList(),
    val filterState: SalaryStatsFilterState = SalaryStatsFilterState(),
    val appliedFilterState: SalaryStatsFilterState = SalaryStatsFilterState(),
    val rows: List<SalaryStatsRowUiModel> = emptyList(),
    val recordCount: Int = 0,
    val totalAmount: String = "0",
    val invalidAmountCount: Int = 0,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val message: String? = null,
    val exportedFileUri: Uri? = null,
    val exportedFileName: String? = null
)

class SalaryStatsViewModel(
    private val workRecordRepository: WorkRecordRepository,
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository,
    private val exportRepository: SalaryStatsExportRepository
) : ViewModel() {
    private val draftFilters = MutableStateFlow(SalaryStatsFilterState())
    private val appliedFilters = MutableStateFlow(SalaryStatsFilterState())
    private val _uiState = MutableStateFlow(SalaryStatsUiState())
    val uiState: StateFlow<SalaryStatsUiState> = _uiState.asStateFlow()

    init {
        combine(
            workRecordRepository.observeWorkRecords(),
            workerRepository.observeWorkers(),
            siteRepository.observeSites(),
            draftFilters,
            appliedFilters
        ) { records, workers, sites, draftFilterState, appliedFilterState ->
            buildUiState(records, workers, sites, draftFilterState, appliedFilterState)
        }.onEach { state ->
            _uiState.value = _uiState.value.copy(
                workers = state.workers,
                sites = state.sites,
                filterState = state.filterState,
                appliedFilterState = state.appliedFilterState,
                rows = state.rows,
                recordCount = state.recordCount,
                totalAmount = state.totalAmount,
                invalidAmountCount = state.invalidAmountCount,
                isLoading = false
            )
        }.launchIn(viewModelScope)
    }

    fun updateWorkerKeyword(keyword: String) {
        draftFilters.value = draftFilters.value.copy(workerKeyword = keyword)
    }

    fun updateSiteKeyword(keyword: String) {
        draftFilters.value = draftFilters.value.copy(siteKeyword = keyword)
    }

    fun fillWorkerKeyword(name: String) {
        updateWorkerKeyword(name)
    }

    fun fillSiteKeyword(name: String) {
        updateSiteKeyword(name)
    }

    fun updateStartDate(date: Long) {
        val endDate = draftFilters.value.endDate
        draftFilters.value = draftFilters.value.copy(
            startDate = date,
            endDate = maxOf(date, endDate)
        )
    }

    fun updateEndDate(date: Long) {
        val startDate = draftFilters.value.startDate
        draftFilters.value = draftFilters.value.copy(
            startDate = minOf(startDate, date),
            endDate = date
        )
    }

    fun applyFilters() {
        appliedFilters.value = draftFilters.value.copy(
            workerKeyword = draftFilters.value.workerKeyword.trim(),
            siteKeyword = draftFilters.value.siteKeyword.trim()
        )
    }

    fun resetFilters() {
        val initial = SalaryStatsFilterState()
        draftFilters.value = initial
        appliedFilters.value = initial
    }

    fun export() {
        val state = _uiState.value
        if (state.rows.isEmpty()) {
            _uiState.value = state.copy(message = "没有可导出的数据")
            return
        }
        _uiState.value = state.copy(isExporting = true)
        viewModelScope.launch {
            when (
                val result = exportRepository.exportSalaryStats(
                    rows = state.rows.map {
                        SalaryStatsExportRow(
                            workDate = it.workDate,
                            workerName = it.workerName,
                            siteName = it.siteName,
                            duration = it.duration,
                            phoneNumber = it.phoneNumber,
                            unitPrice = it.unitPrice,
                            amount = it.amount
                        )
                    },
                    summary = SalaryStatsExportSummary(
                        recordCount = state.recordCount,
                        totalAmount = state.totalAmount
                    ),
                    filters = SalaryStatsExportFilters(
                        workerName = state.appliedFilterState.workerKeyword,
                        siteName = state.appliedFilterState.siteKeyword,
                        startDate = formatDate(state.appliedFilterState.startDate),
                        endDate = formatDate(state.appliedFilterState.endDate)
                    )
                )
            ) {
                is ExportSalaryStatsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportedFileUri = result.uri,
                        exportedFileName = result.fileName,
                        message = "导出成功"
                    )
                }

                is ExportSalaryStatsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        message = result.message
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun dismissExportSuccess() {
        _uiState.value = _uiState.value.copy(
            exportedFileUri = null,
            exportedFileName = null
        )
    }

    private fun buildUiState(
        records: List<WorkRecord>,
        workers: List<Worker>,
        sites: List<Site>,
        draftFilterState: SalaryStatsFilterState,
        appliedFilterState: SalaryStatsFilterState
    ): SalaryStatsUiState {
        val workerKeyword = appliedFilterState.workerKeyword.normalizeForSearch()
        val siteKeyword = appliedFilterState.siteKeyword.normalizeForSearch()
        val filtered = records
            .asSequence()
            .filter { it.workDate in appliedFilterState.startDate..appliedFilterState.endDate }
            .filter {
                workerKeyword.isBlank() ||
                    it.workerNameSnapshot.normalizeForSearch().contains(workerKeyword)
            }
            .filter {
                siteKeyword.isBlank() ||
                    it.siteNameSnapshot.orEmpty().normalizeForSearch().contains(siteKeyword)
            }
            .sortedByDescending { it.workDate }
            .toList()

        val parsedAmounts = filtered.mapNotNull { it.amount.toAmountOrNull() }
        return SalaryStatsUiState(
            workers = workers,
            sites = sites,
            filterState = draftFilterState,
            appliedFilterState = appliedFilterState,
            rows = filtered.map { record ->
                SalaryStatsRowUiModel(
                    id = record.id,
                    workDate = formatDate(record.workDate),
                    workerName = record.workerNameSnapshot,
                    siteName = record.siteNameSnapshot.orEmpty(),
                    duration = record.durationText.orEmpty(),
                    phoneNumber = record.phoneNumberSnapshot.orEmpty(),
                    unitPrice = record.unitPriceText.orEmpty(),
                    amount = record.amount
                )
            },
            recordCount = filtered.size,
            totalAmount = parsedAmounts.sumOfOrZero(),
            invalidAmountCount = filtered.size - parsedAmounts.size,
            isLoading = false
        )
    }
}

class SalaryStatsViewModelFactory(
    private val workRecordRepository: WorkRecordRepository,
    private val workerRepository: WorkerRepository,
    private val siteRepository: SiteRepository,
    private val exportRepository: SalaryStatsExportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalaryStatsViewModel(
            workRecordRepository = workRecordRepository,
            workerRepository = workerRepository,
            siteRepository = siteRepository,
            exportRepository = exportRepository
        ) as T
    }
}

private fun String.toAmountOrNull(): BigDecimal? {
    return trim().replace(",", "").toBigDecimalOrNull()
}

private fun List<BigDecimal>.sumOfOrZero(): String {
    return if (isEmpty()) {
        "0"
    } else {
        fold(BigDecimal.ZERO, BigDecimal::add).stripTrailingZeros().toPlainString()
    }
}

private fun String.normalizeForSearch(): String {
    return trim().lowercase(Locale.getDefault())
}
