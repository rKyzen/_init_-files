package com.init.files.ui.screens.duplicate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.files.data.storage.DuplicateScanProgress
import com.init.files.data.storage.DuplicateScanner
import com.init.files.domain.model.DuplicateGroup
import com.init.files.domain.model.DuplicateScanState
import com.init.files.domain.model.DuplicateSelectFilter
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuplicateViewModel(
    private val duplicateScanner: DuplicateScanner
) : ViewModel() {

    private val _scanState = MutableStateFlow<DuplicateScanState>(DuplicateScanState.Idle)
    val scanState: StateFlow<DuplicateScanState> = _scanState.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FileCategory.ALL)
    val selectedCategory: StateFlow<FileCategory> = _selectedCategory.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    private var currentGroups: List<DuplicateGroup> = emptyList()
    private var scanJob: Job? = null

    init {
        startScan()
    }

    fun setCategoryFilter(category: FileCategory) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            startScan(category)
        }
    }

    fun startScan(category: FileCategory = _selectedCategory.value) {
        scanJob?.cancel()
        _selectedPaths.value = emptySet()
        _scanState.value = DuplicateScanState.Scanning(0, 0, "Initializing scan...")

        scanJob = viewModelScope.launch {
            duplicateScanner.scanForDuplicates(category).collect { progress ->
                when (progress) {
                    is DuplicateScanProgress.Scanning -> {
                        _scanState.value = DuplicateScanState.Scanning(
                            filesScanned = progress.filesIndexed,
                            duplicatesFound = 0,
                            currentFileName = progress.currentPath
                        )
                    }
                    is DuplicateScanProgress.Analyzing -> {
                        _scanState.value = DuplicateScanState.Scanning(
                            filesScanned = progress.processedCollisions,
                            duplicatesFound = progress.collisionsCount,
                            currentFileName = "Computing cryptographic SHA-256 hashes..."
                        )
                    }
                    is DuplicateScanProgress.Completed -> {
                        currentGroups = progress.groups
                        // Default smart selection: select all duplicates (keep 1 original intact)
                        val initialSelected = progress.groups.flatMap { group ->
                            group.duplicateFiles.map { it.path }
                        }.toSet()

                        _selectedPaths.value = initialSelected
                        _scanState.value = DuplicateScanState.Completed(
                            groups = progress.groups,
                            totalWastedBytes = progress.totalWastedBytes,
                            selectedPaths = initialSelected
                        )
                    }
                    is DuplicateScanProgress.Error -> {
                        _scanState.value = DuplicateScanState.Completed(
                            groups = emptyList(),
                            totalWastedBytes = 0L,
                            selectedPaths = emptySet()
                        )
                    }
                }
            }
        }
    }

    fun togglePathSelection(path: String) {
        val current = _selectedPaths.value
        val next = if (current.contains(path)) {
            current - path
        } else {
            current + path
        }
        _selectedPaths.value = next
        updateCompletedStateSelection(next)
    }

    fun applySelectFilter(filter: DuplicateSelectFilter) {
        val newSelection = when (filter) {
            DuplicateSelectFilter.ALL_DUPLICATES -> {
                currentGroups.flatMap { it.duplicateFiles.map { f -> f.path } }.toSet()
            }
            DuplicateSelectFilter.KEEP_OLDEST -> {
                // Keep the oldest file in each group, select all newer copies
                currentGroups.flatMap { group ->
                    val oldest = group.allFiles.minByOrNull { it.lastModified }
                    group.allFiles.filter { it.path != oldest?.path }.map { it.path }
                }.toSet()
            }
            DuplicateSelectFilter.KEEP_NEWEST -> {
                // Keep the newest file in each group, select all older copies
                currentGroups.flatMap { group ->
                    val newest = group.allFiles.maxByOrNull { it.lastModified }
                    group.allFiles.filter { it.path != newest?.path }.map { it.path }
                }.toSet()
            }
            DuplicateSelectFilter.DESELECT_ALL -> emptySet()
        }

        _selectedPaths.value = newSelection
        updateCompletedStateSelection(newSelection)
    }

    fun deleteSelectedDuplicates(useTrash: Boolean = false, onFinished: (deletedCount: Int, reclaimedBytes: Long) -> Unit) {
        val toDelete = _selectedPaths.value
        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            _scanState.value = DuplicateScanState.Deleting(0, toDelete.size)

            // Compute bytes to be reclaimed
            val reclaimedBytes = currentGroups.flatMap { it.allFiles }
                .filter { toDelete.contains(it.path) }
                .sumOf { it.sizeBytes }

            val count = duplicateScanner.deleteDuplicateFiles(toDelete, useTrash)
            _selectedPaths.value = emptySet()

            onFinished(count, reclaimedBytes)
            startScan(_selectedCategory.value)
        }
    }

    private fun updateCompletedStateSelection(selection: Set<String>) {
        val state = _scanState.value as? DuplicateScanState.Completed ?: return
        _scanState.value = state.copy(selectedPaths = selection)
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
