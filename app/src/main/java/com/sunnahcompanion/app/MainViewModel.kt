package com.sunnahcompanion.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ThemeSettings(val dynamicColor: Boolean = false, val arabicTextScale: Float = 1f)

@HiltViewModel
class MainViewModel @Inject constructor(preferences: AppPreferencesDataSource) : ViewModel() {
    val themeSettings: StateFlow<ThemeSettings> = preferences.userPreferences
        .map { ThemeSettings(it.dynamicColor, it.arabicTextScale) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSettings())
}
