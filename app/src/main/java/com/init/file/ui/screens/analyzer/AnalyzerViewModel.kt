package com.init.file.ui.screens.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.file.data.local.LocalRepository
import com.init.file.data.storage.FileManager
import com.init.file.domain.model.FileItem
import com.init.file.domain.model.StorageBreakdown
import com.init.file.domain.model.StorageVolumeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyzerUiState(
    val volumes: List<StorageVolumeInfo> = emptyList(),
    val selectedVolume: StorageVolumeInfo? = null,
    val breakdown: StorageBreakdown? = null,
    val isAnalyzing: Boolean = false,
    val showCleanConfirmDialog: Boolean = false,
    val isCleaning: Boolean = false,
    val statusMessage: String? = null
)

class AnalyzerViewModel(
    private val fileManager: FileManager,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyzerUiState())
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    init {
        loadVolumesAndAnalyze()
    }

    fun loadVolumesAndAnalyze() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val vols = fileManager.getStorageVolumes()
            val primary = vols.firstOrNull()

            _uiState.update {
                it.copy(
                    volumes = vols,
                    selectedVolume = primary
                )
            }

            if (primary != null) {
                analyzeVolume(primary)
            } else {
                _uiState.update { it.copy(isAnalyzing = false) }
            }
        }
    }

    fun selectVolume(volume: StorageVolumeInfo) {
        _uiState.update { it.copy(selectedVolume = volume) }
        analyzeVolume(volume)
    }

    private fun analyzeVolume(volume: StorageVolumeInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val result = fileManager.analyzeStorage(volume)
            _uiState.update {
                it.copy(
                    breakdown = result,
                    isAnalyzing = false
                )
            }
        }
    }

    fun promptCleanJunk() {
        _uiState.update { it.copy(showCleanConfirmDialog = true) }
    }

    fun dismissCleanDialog() {
        _uiState.update { it.copy(showCleanConfirmDialog = false) }
    }

    fun performCleanJunk() {
        val junkFiles = _uiState.value.breakdown?.junkFiles ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showCleanConfirmDialog = false, isCleaning = true) }
            var movedCount = 0
            for (file in junkFiles) {
                val res = fileManager.moveToTrash(file)
                if (res.isSuccess) {
                    localRepository.recordTrashItem(res.getOrThrow())
                    movedCount++
                }
            }
            _uiState.update { it.copy(isCleaning = false) }
            _uiState.value.selectedVolume?.let { analyzeVolume(it) }
            _uiState.update { it.copy(statusMessage = "Moved $movedCount junk item(s) to Trash") }
        }
    }

    fun moveToTrash(file: FileItem) {
        viewModelScope.launch {
            val res = fileManager.moveToTrash(file)
            if (res.isSuccess) {
                localRepository.recordTrashItem(res.getOrThrow())
                _uiState.update { state ->
                    val currentBreakdown = state.breakdown
                    if (currentBreakdown != null) {
                        val updatedFiles = currentBreakdown.largestFiles.filter { it.path != file.path }
                        val updatedFolders = currentBreakdown.largestFolders.filter { it.path != file.path }
                        state.copy(
                            breakdown = currentBreakdown.copy(
                                largestFiles = updatedFiles,
                                largestFolders = updatedFolders
                            ),
                            statusMessage = "Moved '${file.name}' to Trash"
                        )
                    } else {
                        state.copy(statusMessage = "Moved '${file.name}' to Trash")
                    }
                }
            } else {
                _uiState.update { it.copy(statusMessage = "Failed: ${res.exceptionOrNull()?.message ?: "Could not move to Trash"}") }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
