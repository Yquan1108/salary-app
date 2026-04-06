package com.salaryapp.jigong.core.preference

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferences {
    val HasCompletedOnboarding = booleanPreferencesKey("has_completed_onboarding")
    val FontScaleLevel = stringPreferencesKey("font_scale_level")
}
