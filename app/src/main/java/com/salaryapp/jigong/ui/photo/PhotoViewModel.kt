package com.salaryapp.jigong.ui.photo

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.core.util.todayMillis
import com.salaryapp.jigong.data.repository.PhotoBatchInput
import com.salaryapp.jigong.data.repository.PhotoRepository
import com.salaryapp.jigong.data.repository.SavePhotoBatchResult
import com.salaryapp.jigong.data.repository.SaveToAlbumResult
import com.salaryapp.jigong.data.repository.SiteRepository
import com.salaryapp.jigong.domain.model.PhotoBatch
import com.salaryapp.jigong.domain.model.PhotoItem
import com.salaryapp.jigong.domain.model.Site
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PhotoUploadFormState(
    val workDate: Long = todayMillis(),
    val siteId: Long? = null,
    val siteName: String = "",
    val remark: String = ""
)

data class PhotoUiState(
    val batches: List<PhotoBatch> = emptyList(),
    val sites: List<Site> = emptyList(),
    val uploadForm: PhotoUploadFormState = PhotoUploadFormState(),
    val pendingUris: List<Uri> = emptyList(),
    val pendingCameraBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedBatchIds: Set<Long> = emptySet(),
    val deleteConfirmCount: Int? = null,
    val previewBatch: PhotoBatch? = null,
    val previewIndex: Int = 0,
    val message: String? = null
)

class PhotoViewModel(
    private val photoRepository: PhotoRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoUiState())
    val uiState: StateFlow<PhotoUiState> = _uiState.asStateFlow()

    init {
        combine(
            photoRepository.observePhotoBatches(),
            siteRepository.observeSites()
        ) { batches, sites -> batches to sites }
            .onEach { (batches, sites) ->
                _uiState.value = _uiState.value.copy(
                    batches = batches,
                    sites = sites,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun updateUploadForm(transform: (PhotoUploadFormState) -> PhotoUploadFormState) {
        _uiState.value = _uiState.value.copy(uploadForm = transform(_uiState.value.uploadForm))
    }

    fun setPickedUris(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            pendingUris = uris,
            pendingCameraBitmap = null
        )
    }

    fun setCapturedBitmap(bitmap: Bitmap?) {
        _uiState.value = _uiState.value.copy(
            pendingCameraBitmap = bitmap,
            pendingUris = emptyList()
        )
    }

    fun clearPendingPhotos() {
        _uiState.value = _uiState.value.copy(
            pendingUris = emptyList(),
            pendingCameraBitmap = null
        )
    }

    fun savePendingBatch() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            when (
                val result = photoRepository.savePhotoBatch(
                    input = PhotoBatchInput(
                        workDate = state.uploadForm.workDate,
                        siteId = state.uploadForm.siteId,
                        siteName = state.uploadForm.siteName,
                        remark = state.uploadForm.remark
                    ),
                    pickedUris = state.pendingUris,
                    cameraBitmap = state.pendingCameraBitmap
                )
            ) {
                is SavePhotoBatchResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        pendingUris = emptyList(),
                        pendingCameraBitmap = null,
                        uploadForm = state.uploadForm.copy(remark = ""),
                        message = "已保存 ${result.savedCount} 张照片"
                    )
                }

                is SavePhotoBatchResult.ValidationError -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = result.message
                    )
                }
            }
        }
    }

    fun enterSelectionMode(batchId: Long) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedBatchIds = _uiState.value.selectedBatchIds + batchId
        )
    }

    fun toggleSelection(batchId: Long) {
        val selected = _uiState.value.selectedBatchIds.toMutableSet()
        if (!selected.add(batchId)) selected.remove(batchId)
        _uiState.value = _uiState.value.copy(
            selectedBatchIds = selected,
            isSelectionMode = selected.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedBatchIds = emptySet()
        )
    }

    fun selectAllVisible() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedBatchIds = _uiState.value.batches.map { it.id }.toSet()
        )
    }

    fun showDeleteConfirm() {
        val count = _uiState.value.selectedBatchIds.size
        if (count > 0) {
            _uiState.value = _uiState.value.copy(deleteConfirmCount = count)
        }
    }

    fun requestDeleteBatch(batchId: Long) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedBatchIds = setOf(batchId),
            deleteConfirmCount = 1
        )
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(deleteConfirmCount = null)
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedBatchIds.toList()
        dismissDeleteConfirm()
        viewModelScope.launch {
            val count = photoRepository.deletePhotoBatches(ids)
            _uiState.value = _uiState.value.copy(
                isSelectionMode = false,
                selectedBatchIds = emptySet(),
                message = "已删除 $count 个批次"
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
        _uiState.value = _uiState.value.copy(previewIndex = index.coerceIn(0, batch.items.lastIndex))
    }

    fun closePreview() {
        _uiState.value = _uiState.value.copy(previewBatch = null, previewIndex = 0)
    }

    fun exportCurrentPhoto() {
        val batch = _uiState.value.previewBatch ?: return
        val item = batch.items.getOrNull(_uiState.value.previewIndex) ?: return
        _uiState.value = _uiState.value.copy(isExporting = true, message = "正在保存到系统相册...")
        exportPhoto(item)
    }

    private fun exportPhoto(item: PhotoItem) {
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
}

class PhotoViewModelFactory(
    private val photoRepository: PhotoRepository,
    private val siteRepository: SiteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PhotoViewModel(photoRepository, siteRepository) as T
    }
}
