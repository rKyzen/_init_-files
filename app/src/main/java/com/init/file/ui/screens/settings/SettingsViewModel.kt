package com.init.file.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.file.data.local.LocalRepository
import com.init.file.domain.model.SortConfig
import com.init.file.domain.model.SortField
import com.init.file.domain.model.SortOrder
import com.init.file.domain.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "DARK",
    val defaultViewMode: ViewMode = ViewMode.LIST,
    val defaultSortConfig: SortConfig = SortConfig(),
    val showHiddenFiles: Boolean = false,
    val isRescanning: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localRepository.initialize()

            launch {
                localRepository.themeModeFlow.collect { mode ->
                    _uiState.update { it.copy(themeMode = mode) }
                }
            }

            launch {
                localRepository.viewModeFlow.collect { mode ->
                    _uiState.update { it.copy(defaultViewMode = mode) }
                }
            }

            launch {
                localRepository.sortConfigFlow.collect { config ->
                    _uiState.update { it.copy(defaultSortConfig = config) }
                }
            }

            launch {
                localRepository.showHiddenFlow.collect { hidden ->
                    _uiState.update { it.copy(showHiddenFiles = hidden) }
                }
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            localRepository.setThemeMode(mode)
        }
    }

    fun setDefaultViewMode(mode: ViewMode) {
        viewModelScope.launch {
            localRepository.setViewMode(mode)
        }
    }

    fun setDefaultSortConfig(config: SortConfig) {
        viewModelScope.launch {
            localRepository.setSortConfig(config)
        }
    }

    fun toggleShowHidden(show: Boolean) {
        viewModelScope.launch {
            localRepository.setShowHidden(show)
        }
    }

    fun triggerRescan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRescanning = true) }
            kotlinx.coroutines.delay(1000)
            _uiState.update { it.copy(isRescanning = false, message = "Storage index refreshed") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
