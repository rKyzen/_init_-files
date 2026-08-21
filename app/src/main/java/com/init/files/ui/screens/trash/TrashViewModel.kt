package com.init.files.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.files.data.local.LocalRepository
import com.init.files.data.storage.FileManager
import com.init.files.domain.model.TrashItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val items: List<TrashItem> = emptyList(),
    val selectedItems: Set<TrashItem> = emptySet(),
    val isLoading: Boolean = false,
    val isMultiSelectMode: Boolean = false,
    val snackbarMessage: String? = null,
    val totalSizeBytes: Long = 0L
)

class TrashViewModel(
    private val fileManager: FileManager,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localRepository.trashItemsFlow.collect { list ->
                _uiState.update {
                    it.copy(
                        items = list,
                        totalSizeBytes = list.sumOf { item -> item.sizeBytes },
                        selectedItems = it.selectedItems.filter { sel -> list.any { item -> item.id == sel.id } }.toSet(),
                        isMultiSelectMode = if (list.isEmpty()) false else it.isMultiSelectMode
                    )
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            localRepository.refreshTrashItems()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun restoreItem(item: TrashItem) {
        viewModelScope.launch {
            val result = fileManager.restoreTrashItem(item)
            if (result.isSuccess) {
                localRepository.removeTrashRecord(item.id)
                _uiState.update { it.copy(snackbarMessage = "RESTORED '${item.name}'") }
            } else {
                _uiState.update { it.copy(snackbarMessage = "FAILED TO RESTORE: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun permanentlyDeleteItem(item: TrashItem) {
        viewModelScope.launch {
            val result = fileManager.permanentlyDeleteTrashItem(item)
            if (result.isSuccess) {
                localRepository.removeTrashRecord(item.id)
                _uiState.update { it.copy(snackbarMessage = "PERMANENTLY DELETED '${item.name}'") }
            } else {
                _uiState.update { it.copy(snackbarMessage = "DELETE FAILED: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun restoreSelected() {
        val selected = _uiState.value.selectedItems.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            var restoredCount = 0
            for (item in selected) {
                val result = fileManager.restoreTrashItem(item)
                if (result.isSuccess) {
                    localRepository.removeTrashRecord(item.id)
                    restoredCount++
                }
            }
            _uiState.update {
                it.copy(
                    selectedItems = emptySet(),
                    isMultiSelectMode = false,
                    snackbarMessage = "RESTORED $restoredCount ITEMS"
                )
            }
        }
    }

    fun deleteSelectedPermanently() {
        val selected = _uiState.value.selectedItems.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            var deletedCount = 0
            for (item in selected) {
                val result = fileManager.permanentlyDeleteTrashItem(item)
                if (result.isSuccess) {
                    localRepository.removeTrashRecord(item.id)
                    deletedCount++
                }
            }
            _uiState.update {
                it.copy(
                    selectedItems = emptySet(),
                    isMultiSelectMode = false,
                    snackbarMessage = "PERMANENTLY DELETED $deletedCount ITEMS"
                )
            }
        }
    }

    fun emptyTrash() {
        val allItems = _uiState.value.items
        viewModelScope.launch {
            val result = fileManager.emptyTrash(allItems)
            localRepository.clearAllTrashRecords()
            _uiState.update {
                it.copy(
                    selectedItems = emptySet(),
                    isMultiSelectMode = false,
                    snackbarMessage = "TRASH EMPTIED"
                )
            }
        }
    }

    fun toggleSelection(item: TrashItem) {
        _uiState.update {
            val current = it.selectedItems.toMutableSet()
            if (current.contains(item)) {
                current.remove(item)
            } else {
                current.add(item)
            }
            it.copy(
                selectedItems = current,
                isMultiSelectMode = current.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update {
            it.copy(
                selectedItems = it.items.toSet(),
                isMultiSelectMode = true
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedItems = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
