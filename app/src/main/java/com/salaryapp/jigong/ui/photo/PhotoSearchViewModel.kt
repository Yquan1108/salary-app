package com.salaryapp.jigong.ui.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.data.repository.PhotoRepository
import com.salaryapp.jigong.data.repository.SaveToAlbumResult
import com.salaryapp.jigong.data.repository.SiteRepository
import com.salaryapp.jigong.domain.model.PhotoBatch
import com.salaryapp.jigong.domain.model.Site
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PhotoSearchUiState(
    val isLoading: Boolean = true,
    val allBatches: List<PhotoBatch> = emptyList(),
    val filteredBatches: List<PhotoBatch> = emptyList(),
    val sites: List<Site> = emptyList(),
    val keyword: String = "",
    val selectedSiteId: Long? = null,
    val selectedSiteName: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val previewBatch: PhotoBatch? = null,
    val previewIndex: Int = 0,
    val isExporting: Boolean = false,
    val message: String? = null
)

class PhotoSearchViewModel(
    private val photoRepository: PhotoRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoSearchUiState())
    val uiState: StateFlow<PhotoSearchUiState> = _uiState.asStateFlow()

    init {
        combine(
            photoRepository.observePhotoBatches(),
            siteRepository.observeSites()
        ) { batches, sites -> batches to sites }
            .onEach { (batches, sites) ->
                val current = _uiState.value
                val nextState = current.copy(
                    isLoading = false,
                    allBatches = batches,
                    sites = sites
                )
                _uiState.value = nextState.copy(
                    filteredBatches = nextState.applyFilters()
                )
            }
            .launchIn(viewModelScope)
    }

    fun updateKeyword(value: String) {
        updateState { copy(keyword = value) }
    }

    fun updateSite(site: Site?) {
        updateState {
            copy(
                selectedSiteId = site?.id,
                selectedSiteName = site?.siteName.orEmpty()
            )
        }
    }

    fun updateStartDate(value: Long?) {
        updateState { copy(startDate = value) }
    }

    fun updateEndDate(value: Long?) {
        updateState { copy(endDate = value) }
    }

    fun clearFilters() {
        updateState {
            copy(
                keyword = "",
                selectedSiteId = null,
                selectedSiteName = "",
                startDate = null,
                endDate = null
            )
        }
    }

    fun openPreview(batch: PhotoBatch, index: Int) {
        _uiState.value = _uiState.value.copy(
            previewBatch = batch,
            previewIndex = index.coerceIn(0, batch.items.lastIndex)
        )
    }

    fun updatePreviewIndex(index: Int) {
        val batch = _uiState.value.previewBatch ?: return
        _uiState.value = _uiState.value.copy(
            previewIndex = index.coerceIn(0, batch.items.lastIndex)
        )
    }

    fun closePreview() {
        _uiState.value = _uiState.value.copy(previewBatch = null, previewIndex = 0)
    }

    fun exportCurrentPhoto() {
        val batch = _uiState.value.previewBatch ?: return
        val item = batch.items.getOrNull(_uiState.value.previewIndex) ?: return
        _uiState.value = _uiState.value.copy(isExporting = true)
        viewModelScope.launch {
            when (val result = photoRepository.exportPhotoToAlbum(item)) {
                is SaveToAlbumResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        message = "已保存到相册"
                    )
                }

                is SaveToAlbumResult.Error -> {
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

    private fun updateState(transform: PhotoSearchUiState.() -> PhotoSearchUiState) {
        val next = _uiState.value.transform()
        _uiState.value = next.copy(filteredBatches = next.applyFilters())
    }

    private fun PhotoSearchUiState.applyFilters(): List<PhotoBatch> {
        val normalizedKeyword = keyword.trim()
        return allBatches.filter { batch ->
            val matchesSite = selectedSiteId == null || batch.siteId == selectedSiteId
            val matchesStart = startDate == null || batch.workDate >= startDate
            val matchesEnd = endDate == null || batch.workDate <= endDate
            val matchesKeyword = normalizedKeyword.isBlank() || batch.matchesKeyword(normalizedKeyword)
            matchesSite && matchesStart && matchesEnd && matchesKeyword
        }
    }

    private fun PhotoBatch.matchesKeyword(keyword: String): Boolean {
        return siteNameSnapshot.contains(keyword, ignoreCase = true) ||
            remark.orEmpty().contains(keyword, ignoreCase = true) ||
            items.any { item -> item.originalFileName.orEmpty().contains(keyword, ignoreCase = true) }
    }
}

class PhotoSearchViewModelFactory(
    private val photoRepository: PhotoRepository,
    private val siteRepository: SiteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PhotoSearchViewModel(
            photoRepository = photoRepository,
            siteRepository = siteRepository
        ) as T
    }
}
