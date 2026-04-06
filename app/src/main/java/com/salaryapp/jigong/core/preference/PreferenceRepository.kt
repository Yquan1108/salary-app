package com.salaryapp.jigong.core.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.salaryapp.jigong.domain.model.FontScaleLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

data class PreferenceState(
    val hasCompletedOnboarding: Boolean = false,
    val fontScaleLevel: FontScaleLevel = FontScaleLevel.STANDARD
)

class PreferenceRepository(
    private val context: Context
) {
    val preferenceState: Flow<PreferenceState> = context.dataStore.data.map { preferences ->
        PreferenceState(
            hasCompletedOnboarding = preferences[AppPreferences.HasCompletedOnboarding] ?: false,
            fontScaleLevel = FontScaleLevel.fromStorage(
                preferences[AppPreferences.FontScaleLevel]
            )
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AppPreferences.HasCompletedOnboarding] = completed
        }
    }

    suspend fun setFontScaleLevel(fontScaleLevel: FontScaleLevel) {
        context.dataStore.edit { preferences ->
            preferences[AppPreferences.FontScaleLevel] = fontScaleLevel.storageValue
        }
    }
}
