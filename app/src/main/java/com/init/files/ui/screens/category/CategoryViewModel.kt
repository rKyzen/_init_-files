package com.init.files.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.files.data.local.LocalRepository
import com.init.files.data.storage.FileManager
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.SortConfig
import com.init.files.domain.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryUiState(
    val category: FileCategory = FileCategory.ALL,
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val sortConfig: SortConfig = SortConfig(),
    val viewMode: ViewMode = ViewMode.LIST,
    val selectedItems: Set<FileItem> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val snackbarMessage: String? = null
)

class CategoryViewModel(
    private val fileManager: FileManager,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localRepository.initialize()
            localRepository.viewModeFlow.collect { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
    }

    fun setCategory(category: FileCategory) {
        _uiState.update { it.copy(category = category, selectedItems = emptySet(), isMultiSelectMode = false) }
        loadCategoryItems()
    }

    fun refresh() {
        loadCategoryItems()
    }

    private fun loadCategoryItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val cat = _uiState.value.category
            val sort = _uiState.value.sortConfig
            val files = fileManager.getCategoryFiles(cat, sort)
            _uiState.update {
                it.copy(
                    items = files,
                    isLoading = false
                )
            }
        }
    }

    fun toggleSelection(item: FileItem) {
        _uiState.update { state ->
            val current = state.selectedItems.toMutableSet()
            if (current.contains(item)) current.remove(item) else current.add(item)
            state.copy(selectedItems = current, isMultiSelectMode = current.isNotEmpty())
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedItems = it.items.toSet(), isMultiSelectMode = true) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptySet(), isMultiSelectMode = false) }
    }

    fun toggleViewMode() {
        val next = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        viewModelScope.launch {
            localRepository.setViewMode(next)
        }
    }

    fun setSortConfig(config: SortConfig) {
        _uiState.update { it.copy(sortConfig = config) }
        loadCategoryItems()
    }

    fun deleteSelected(permanently: Boolean = false) {
        val items = _uiState.value.selectedItems.toList()
        if (items.isEmpty()) return
        viewModelScope.launch {
            if (!permanently) {
                var count = 0
                for (item in items) {
                    val res = fileManager.moveToTrash(item)
                    if (res.isSuccess) {
                        localRepository.recordTrashItem(res.getOrThrow())
                        count++
                    }
                }
                clearSelection()
                loadCategoryItems()
                _uiState.update { it.copy(snackbarMessage = "Moved $count item(s) to Trash") }
            } else {
                val result = fileManager.deleteFiles(items)
                clearSelection()
                if (result.isSuccess) {
                    loadCategoryItems()
                    _uiState.update { it.copy(snackbarMessage = "${result.getOrNull()} item(s) permanently deleted") }
                }
            }
        }
    }

    fun recordFileAccess(file: FileItem) {
        viewModelScope.launch {
            localRepository.recordFileAccess(file)
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
