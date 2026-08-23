package com.init.file.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.file.data.local.LocalRepository
import com.init.file.data.storage.FileManager
import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.FileItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SizeFilter(val label: String, val minBytes: Long?, val maxBytes: Long?) {
    ALL("ANY SIZE", null, null),
    SMALL("< 1 MB", null, 1024 * 1024L),
    MEDIUM("1 - 10 MB", 1024 * 1024L, 10 * 1024 * 1024L),
    LARGE("10 - 100 MB", 10 * 1024 * 1024L, 100 * 1024 * 1024L),
    HUGE("> 100 MB", 100 * 1024 * 1024L, null)
}

enum class DateFilter(val label: String, val daysAgo: Int?) {
    ALL("ANY TIME", null),
    TODAY("TODAY", 1),
    WEEK("7 DAYS", 7),
    MONTH("30 DAYS", 30)
}

data class SearchUiState(
    val query: String = "",
    val results: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val selectedCategory: FileCategory? = null,
    val selectedSizeFilter: SizeFilter = SizeFilter.ALL,
    val selectedDateFilter: DateFilter = DateFilter.ALL,
    val searchHistory: List<String> = emptyList(),
    val scopePath: String? = null
)

class SearchViewModel(
    private val fileManager: FileManager,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val history = localRepository.getSearchHistory()
            _uiState.update { it.copy(searchHistory = history) }
        }
    }

    fun setQuery(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        triggerSearch()
    }

    fun setScopePath(path: String?) {
        _uiState.update { it.copy(scopePath = path) }
        triggerSearch()
    }

    fun selectCategory(category: FileCategory?) {
        _uiState.update { it.copy(selectedCategory = if (it.selectedCategory == category) null else category) }
        triggerSearch()
    }

    fun selectSizeFilter(filter: SizeFilter) {
        _uiState.update { it.copy(selectedSizeFilter = filter) }
        triggerSearch()
    }

    fun selectDateFilter(filter: DateFilter) {
        _uiState.update { it.copy(selectedDateFilter = filter) }
        triggerSearch()
    }

    private fun triggerSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val query = _uiState.value.query
            val category = _uiState.value.selectedCategory
            val sizeFilter = _uiState.value.selectedSizeFilter
            val dateFilter = _uiState.value.selectedDateFilter
            val scope = _uiState.value.scopePath

            if (query.isBlank() && category == null && sizeFilter == SizeFilter.ALL && dateFilter == DateFilter.ALL) {
                _uiState.update { it.copy(results = emptyList(), isSearching = false) }
                return@launch
            }

            _uiState.update { it.copy(isSearching = true) }
            delay(200) // Debounce

            val minDate = dateFilter.daysAgo?.let {
                System.currentTimeMillis() - (it * 24L * 60L * 60L * 1000L)
            }

            val files = fileManager.searchFiles(
                query = query,
                rootPath = scope,
                category = category,
                minSize = sizeFilter.minBytes,
                maxSize = sizeFilter.maxBytes,
                minDate = minDate
            )

            if (query.isNotBlank()) {
                localRepository.saveSearchQuery(query)
            }

            _uiState.update {
                it.copy(
                    results = files,
                    isSearching = false
                )
            }
            loadHistory()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            localRepository.clearSearchHistory()
            _uiState.update { it.copy(searchHistory = emptyList()) }
        }
    }
}
