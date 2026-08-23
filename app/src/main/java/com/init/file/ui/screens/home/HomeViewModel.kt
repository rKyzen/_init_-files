package com.init.file.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.file.data.local.LocalRepository
import com.init.file.data.storage.FileManager
import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.FileItem
import com.init.file.domain.model.StorageVolumeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategorySummary(
    val category: FileCategory,
    val count: Int = 0,
    val totalSizeBytes: Long = 0L
)

data class HomeUiState(
    val volumes: List<StorageVolumeInfo> = emptyList(),
    val categories: List<CategorySummary> = emptyList(),
    val pinnedFolders: List<FileItem> = emptyList(),
    val recentFiles: List<FileItem> = emptyList(),
    val trashCount: Int = 0,
    val trashSizeBytes: Long = 0L,
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val fileManager: FileManager,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localRepository.initialize()

            launch {
                localRepository.pinnedFoldersFlow.collect { list ->
                    _uiState.update { it.copy(pinnedFolders = list) }
                }
            }

            launch {
                localRepository.recentFilesFlow.collect { list ->
                    _uiState.update { it.copy(recentFiles = list) }
                }
            }

            launch {
                localRepository.trashItemsFlow.collect { list ->
                    _uiState.update {
                        it.copy(
                            trashCount = list.size,
                            trashSizeBytes = list.sumOf { item -> item.sizeBytes }
                        )
                    }
                }
            }

            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val vols = fileManager.getStorageVolumes()

            // Summarize categories
            val summaries = mutableListOf<CategorySummary>()
            for (cat in listOf(
                FileCategory.IMAGES,
                FileCategory.VIDEOS,
                FileCategory.AUDIO,
                FileCategory.DOCUMENTS,
                FileCategory.APKS,
                FileCategory.ARCHIVES,
                FileCategory.DOWNLOADS
            )) {
                val files = fileManager.getCategoryFiles(cat)
                summaries.add(
                    CategorySummary(
                        category = cat,
                        count = files.size,
                        totalSizeBytes = files.sumOf { it.sizeBytes }
                    )
                )
            }

            _uiState.update {
                it.copy(
                    volumes = vols,
                    categories = summaries,
                    isLoading = false
                )
            }
        }
    }
}
