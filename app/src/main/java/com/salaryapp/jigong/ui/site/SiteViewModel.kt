package com.salaryapp.jigong.ui.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.data.repository.SaveSiteResult
import com.salaryapp.jigong.data.repository.SiteInput
import com.salaryapp.jigong.data.repository.SiteRepository
import com.salaryapp.jigong.domain.model.Site
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SiteEditorState(
    val id: Long? = null,
    val siteName: String = "",
    val addressOrAlias: String = "",
    val note: String = ""
)

data class SiteUiState(
    val sites: List<Site> = emptyList(),
    val editorState: SiteEditorState? = null,
    val deleteTarget: Site? = null,
    val duplicatePending: Boolean = false,
    val message: String? = null
)

class SiteViewModel(
    private val siteRepository: SiteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SiteUiState())
    val uiState: StateFlow<SiteUiState> = _uiState.asStateFlow()

    init {
        siteRepository.observeSites()
            .onEach { sites ->
                _uiState.value = _uiState.value.copy(sites = sites)
            }
            .launchIn(viewModelScope)
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            editorState = SiteEditorState(),
            duplicatePending = false,
            message = null
        )
    }

    fun showEditDialog(site: Site) {
        _uiState.value = _uiState.value.copy(
            editorState = SiteEditorState(
                id = site.id,
                siteName = site.siteName,
                addressOrAlias = site.addressOrAlias.orEmpty(),
                note = site.note.orEmpty()
            ),
            duplicatePending = false,
            message = null
        )
    }

    fun updateEditor(transform: (SiteEditorState) -> SiteEditorState) {
        val current = _uiState.value.editorState ?: return
        _uiState.value = _uiState.value.copy(editorState = transform(current))
    }

    fun dismissEditor() {
        _uiState.value = _uiState.value.copy(editorState = null, duplicatePending = false)
    }

    fun saveSite(allowDuplicate: Boolean = false) {
        val editor = _uiState.value.editorState ?: return
        viewModelScope.launch {
            when (
                val result = siteRepository.saveSite(
                    input = SiteInput(
                        id = editor.id,
                        siteName = editor.siteName,
                        addressOrAlias = editor.addressOrAlias,
                        note = editor.note
                    ),
                    allowDuplicate = allowDuplicate
                )
            ) {
                is SaveSiteResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        editorState = null,
                        duplicatePending = false,
                        message = if (editor.id == null) "工地已保存" else "工地已更新"
                    )
                }
                is SaveSiteResult.DuplicateName -> {
                    _uiState.value = _uiState.value.copy(duplicatePending = true)
                }
                is SaveSiteResult.ValidationError -> {
                    _uiState.value = _uiState.value.copy(message = result.message)
                }
            }
        }
    }

    fun requestDelete(site: Site) {
        _uiState.value = _uiState.value.copy(deleteTarget = site)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(deleteTarget = null)
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            siteRepository.deleteSite(target.id)
            _uiState.value = _uiState.value.copy(deleteTarget = null, message = "工地已删除")
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

class SiteViewModelFactory(
    private val siteRepository: SiteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SiteViewModel(siteRepository) as T
    }
}
