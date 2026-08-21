package com.init.files.ui.screens.browse

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.files.data.local.LocalRepository
import com.init.files.data.storage.FileManager
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.FileOperation
import com.init.files.domain.model.OperationProgress
import com.init.files.domain.model.SortConfig
import com.init.files.domain.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface BrowseDialogState {
    data object None : BrowseDialogState
    data object CreateFolder : BrowseDialogState
    data object CreateFile : BrowseDialogState
    data class Rename(val item: FileItem) : BrowseDialogState
    data class DeleteConfirm(val items: List<FileItem>) : BrowseDialogState
    data object Sort : BrowseDialogState
    data class ZipPrompt(val items: List<FileItem>) : BrowseDialogState
}

data class BrowseUiState(
    val currentPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val sortConfig: SortConfig = SortConfig(),
    val viewMode: ViewMode = ViewMode.LIST,
    val showHidden: Boolean = false,
    val selectedItems: Set<FileItem> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val clipboard: Pair<List<FileItem>, FileOperation>? = null,
    val operationProgress: OperationProgress? = null,
    val dialogState: BrowseDialogState = BrowseDialogState.None,
    val snackbarMessage: String? = null,
    val isCurrentFolderPinned: Boolean = false
)

class BrowseViewModel(
    private val fileManager: FileManager,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localRepository.initialize()

            // Observe preferences
            launch {
                localRepository.viewModeFlow.collect { mode ->
                    _uiState.update { it.copy(viewMode = mode) }
                }
            }
            launch {
                localRepository.sortConfigFlow.collect { config ->
                    _uiState.update { it.copy(sortConfig = config) }
                    loadCurrentDirectory()
                }
            }
            launch {
                localRepository.showHiddenFlow.collect { hidden ->
                    _uiState.update { it.copy(showHidden = hidden) }
                    loadCurrentDirectory()
                }
            }

            loadCurrentDirectory()
        }
    }

    fun navigateTo(path: String) {
        _uiState.update { it.copy(currentPath = path, selectedItems = emptySet(), isMultiSelectMode = false) }
        loadCurrentDirectory()
    }

    fun navigateUp(): Boolean {
        val current = File(_uiState.value.currentPath)
        val parent = current.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            navigateTo(parent.absolutePath)
            return true
        }
        return false
    }

    fun refresh() {
        loadCurrentDirectory()
    }

    private fun loadCurrentDirectory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val path = _uiState.value.currentPath
            val sort = _uiState.value.sortConfig
            val showHidden = _uiState.value.showHidden

            val files = fileManager.listFiles(path, sort, showHidden)
            val isPinned = localRepository.isPinned(path)

            _uiState.update {
                it.copy(
                    items = files,
                    isLoading = false,
                    isCurrentFolderPinned = isPinned
                )
            }
        }
    }

    // --- Selection ---

    fun toggleSelection(item: FileItem) {
        _uiState.update { state ->
            val current = state.selectedItems.toMutableSet()
            if (current.contains(item)) {
                current.remove(item)
            } else {
                current.add(item)
            }
            state.copy(
                selectedItems = current,
                isMultiSelectMode = current.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                selectedItems = state.items.toSet(),
                isMultiSelectMode = true
            )
        }
    }

    fun invertSelection() {
        _uiState.update { state ->
            val inverted = state.items.filterNot { state.selectedItems.contains(it) }.toSet()
            state.copy(
                selectedItems = inverted,
                isMultiSelectMode = inverted.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptySet(), isMultiSelectMode = false) }
    }

    // --- View Mode & Sort ---

    fun toggleViewMode() {
        val next = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        viewModelScope.launch {
            localRepository.setViewMode(next)
        }
    }

    fun setSortConfig(config: SortConfig) {
        viewModelScope.launch {
            localRepository.setSortConfig(config)
        }
    }

    // --- Dialogs ---

    fun showDialog(dialog: BrowseDialogState) {
        _uiState.update { it.copy(dialogState = dialog) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = BrowseDialogState.None) }
    }

    // --- File Operations ---

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = fileManager.createFolder(_uiState.value.currentPath, name)
            dismissDialog()
            if (result.isSuccess) {
                loadCurrentDirectory()
                showSnackbar("Directory '$name' created")
            } else {
                showSnackbar("Error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val result = fileManager.createFile(_uiState.value.currentPath, name)
            dismissDialog()
            if (result.isSuccess) {
                loadCurrentDirectory()
                showSnackbar("File '$name' created")
            } else {
                showSnackbar("Error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun renameItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            val result = fileManager.rename(item, newName)
            dismissDialog()
            if (result.isSuccess) {
                loadCurrentDirectory()
                showSnackbar("Renamed to '$newName'")
            } else {
                showSnackbar("Rename failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun moveToTrashSelected() {
        val itemsToDelete = _uiState.value.selectedItems.toList()
        if (itemsToDelete.isEmpty()) return

        viewModelScope.launch {
            dismissDialog()
            var count = 0
            for (item in itemsToDelete) {
                val result = fileManager.moveToTrash(item)
                if (result.isSuccess) {
                    localRepository.recordTrashItem(result.getOrThrow())
                    count++
                }
            }
            clearSelection()
            loadCurrentDirectory()
            showSnackbar("Moved $count item(s) to Trash")
        }
    }

    fun deleteSelected(permanently: Boolean = false) {
        if (!permanently) {
            moveToTrashSelected()
            return
        }
        val itemsToDelete = _uiState.value.selectedItems.toList()
        if (itemsToDelete.isEmpty()) return

        viewModelScope.launch {
            dismissDialog()
            val result = fileManager.deleteFiles(itemsToDelete)
            clearSelection()
            if (result.isSuccess) {
                loadCurrentDirectory()
                showSnackbar("${result.getOrNull()} item(s) permanently deleted")
            } else {
                showSnackbar("Delete error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun copySelected() {
        val items = _uiState.value.selectedItems.toList()
        if (items.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = Pair(items, FileOperation.COPY),
                selectedItems = emptySet(),
                isMultiSelectMode = false
            )
        }
        showSnackbar("${items.size} item(s) copied to clipboard")
    }

    fun moveSelected() {
        val items = _uiState.value.selectedItems.toList()
        if (items.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = Pair(items, FileOperation.MOVE),
                selectedItems = emptySet(),
                isMultiSelectMode = false
            )
        }
        showSnackbar("${items.size} item(s) ready to move")
    }

    fun pasteClipboard() {
        val clipboard = _uiState.value.clipboard ?: return
        val items = clipboard.first
        val op = clipboard.second
        val target = _uiState.value.currentPath

        viewModelScope.launch {
            if (op == FileOperation.COPY) {
                val result = fileManager.copyFiles(items, target) { progress ->
                    _uiState.update { it.copy(operationProgress = progress) }
                }
                _uiState.update { it.copy(operationProgress = null) }
                if (result.isSuccess) {
                    loadCurrentDirectory()
                    showSnackbar("Pasted ${result.getOrNull()} item(s)")
                } else {
                    showSnackbar("Copy failed: ${result.exceptionOrNull()?.message}")
                }
            } else if (op == FileOperation.MOVE) {
                val result = fileManager.moveFiles(items, target) { progress ->
                    _uiState.update { it.copy(operationProgress = progress) }
                }
                _uiState.update { it.copy(operationProgress = null, clipboard = null) }
                if (result.isSuccess) {
                    loadCurrentDirectory()
                    showSnackbar("Moved ${result.getOrNull()} item(s)")
                } else {
                    showSnackbar("Move failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun cancelClipboard() {
        _uiState.update { it.copy(clipboard = null) }
    }

    fun zipSelected(zipName: String) {
        val items = _uiState.value.selectedItems.toList()
        if (items.isEmpty()) return

        val cleanName = if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip"
        val targetZipPath = File(_uiState.value.currentPath, cleanName).absolutePath

        viewModelScope.launch {
            dismissDialog()
            val result = fileManager.zipFiles(items, targetZipPath) { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
            }
            _uiState.update { it.copy(operationProgress = null) }
            clearSelection()
            if (result.isSuccess) {
                loadCurrentDirectory()
                showSnackbar("Archive '$cleanName' created")
            } else {
                showSnackbar("Zip failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun extractZip(item: FileItem) {
        viewModelScope.launch {
            val targetDir = File(item.path).parentFile?.absolutePath ?: _uiState.value.currentPath
            val result = fileManager.extractZip(item, targetDir) { progress ->
                _uiState.update { it.copy(operationProgress = progress) }
            }
            _uiState.update { it.copy(operationProgress = null) }
            if (result.isSuccess) {
                loadCurrentDirectory()
                showSnackbar("Extracted ${result.getOrNull()} files")
            } else {
                showSnackbar("Extraction failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun togglePinCurrentFolder() {
        viewModelScope.launch {
            val path = _uiState.value.currentPath
            val name = File(path).name.ifEmpty { "Root" }
            if (_uiState.value.isCurrentFolderPinned) {
                localRepository.unpinFolder(path)
                showSnackbar("Unpinned folder")
            } else {
                localRepository.pinFolder(path, name)
                showSnackbar("Pinned folder to Quick Access")
            }
            _uiState.update { it.copy(isCurrentFolderPinned = !it.isCurrentFolderPinned) }
        }
    }

    fun recordFileAccess(file: FileItem) {
        viewModelScope.launch {
            localRepository.recordFileAccess(file)
        }
    }

    private fun showSnackbar(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
