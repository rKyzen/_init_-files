package com.init.files

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SplashNavKey : NavKey

@Serializable
data object HomeNavKey : NavKey

@Serializable
data class BrowseNavKey(val path: String) : NavKey

@Serializable
data class CategoryNavKey(val categoryName: String) : NavKey

@Serializable
data class SearchNavKey(val scopePath: String? = null) : NavKey

@Serializable
data object AnalyzerNavKey : NavKey

@Serializable
data class PreviewNavKey(val path: String) : NavKey

@Serializable
data object SettingsNavKey : NavKey

@Serializable
data object TrashNavKey : NavKey
