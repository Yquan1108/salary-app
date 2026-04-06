package com.salaryapp.jigong.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salaryapp.jigong.core.preference.PreferenceRepository
import com.salaryapp.jigong.domain.model.FontScaleLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class RootUiState(
    val isLoading: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val fontScaleLevel: FontScaleLevel = FontScaleLevel.STANDARD
)

class RootViewModel(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RootUiState())
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        preferenceRepository.preferenceState
            .onEach { preferenceState ->
                _uiState.value = RootUiState(
                    isLoading = false,
                    hasCompletedOnboarding = preferenceState.hasCompletedOnboarding,
                    fontScaleLevel = preferenceState.fontScaleLevel
                )
            }
            .launchIn(viewModelScope)
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            preferenceRepository.setOnboardingCompleted(true)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferenceRepository.setOnboardingCompleted(false)
        }
    }

    fun updateFontScale(fontScaleLevel: FontScaleLevel) {
        viewModelScope.launch {
            preferenceRepository.setFontScaleLevel(fontScaleLevel)
        }
    }
}

class RootViewModelFactory(
    private val preferenceRepository: PreferenceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RootViewModel(preferenceRepository) as T
    }
}
