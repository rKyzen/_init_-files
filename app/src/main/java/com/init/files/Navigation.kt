package com.init.files

import android.content.Context
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.init.files.data.local.InitDatabaseHelper
import com.init.files.data.local.LocalRepository
import com.init.files.data.storage.FileManager
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.categorizeFile
import com.init.files.ui.screens.analyzer.AnalyzerViewModel
import com.init.files.ui.screens.analyzer.StorageAnalyzerScreen
import com.init.files.ui.screens.browse.BrowseScreen
import com.init.files.ui.screens.browse.BrowseViewModel
import com.init.files.ui.screens.category.CategoryScreen
import com.init.files.ui.screens.category.CategoryViewModel
import com.init.files.ui.screens.home.HomeScreen
import com.init.files.ui.screens.home.HomeViewModel
import com.init.files.ui.screens.preview.FilePreviewSheet
import com.init.files.ui.screens.preview.PreviewViewModel
import com.init.files.ui.screens.search.SearchScreen
import com.init.files.ui.screens.search.SearchViewModel
import com.init.files.ui.screens.settings.SettingsScreen
import com.init.files.ui.screens.settings.SettingsViewModel
import com.init.files.ui.screens.splash.SplashScreen
import com.init.files.ui.screens.trash.TrashScreen
import com.init.files.ui.screens.trash.TrashViewModel
import java.io.File

private val FluidDecelEasing = CubicBezierEasing(0.08f, 0.95f, 0.12f, 1.0f)
private val FluidAccelEasing = CubicBezierEasing(0.35f, 0.0f, 0.75f, 0.15f)

@Composable
fun MainNavigation(localRepository: LocalRepository? = null) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack(SplashNavKey)

    val dbHelper = remember { InitDatabaseHelper(context) }
    val repo = remember(localRepository) { localRepository ?: LocalRepository(dbHelper) }
    val fileManager = remember { FileManager(context) }

    val homeViewModel = remember { HomeViewModel(fileManager, repo) }
    val browseViewModel = remember { BrowseViewModel(fileManager, repo) }
    val searchViewModel = remember { SearchViewModel(fileManager, repo) }
    val analyzerViewModel = remember { AnalyzerViewModel(fileManager, repo) }
    val previewViewModel = remember { PreviewViewModel(fileManager) }
    val settingsViewModel = remember { SettingsViewModel(repo) }
    val categoryViewModel = remember { CategoryViewModel(fileManager, repo) }
    val trashViewModel = remember { TrashViewModel(fileManager, repo) }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        transitionSpec = {
            val fromKey = initialState.key
            val toKey = targetState.key
            if (fromKey is SplashNavKey || toKey is SplashNavKey) {
                fadeIn(animationSpec = tween(350, easing = LinearOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing))
            } else if (toKey is PreviewNavKey) {
                // Smooth bottom slide in for preview sheets
                (slideInVertically(
                    initialOffsetY = { (it * 0.18f).toInt() },
                    animationSpec = tween(320, easing = FluidDecelEasing)
                ) + fadeIn(
                    animationSpec = tween(240, easing = LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.97f,
                    animationSpec = tween(320, easing = FluidDecelEasing)
                )) togetherWith (
                    fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing))
                )
            } else if (fromKey is PreviewNavKey) {
                // Smooth bottom slide out when dismissing preview
                (fadeIn(
                    animationSpec = tween(240, easing = LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.98f,
                    animationSpec = tween(280, easing = FluidDecelEasing)
                )) togetherWith (
                    slideOutVertically(
                        targetOffsetY = { (it * 0.18f).toInt() },
                        animationSpec = tween(280, easing = FluidAccelEasing)
                    ) + fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing))
                )
            } else if (toKey is HomeNavKey) {
                // Returning back to Home: Reverse of intro (Opposite direction)
                // Home slides in from Left, current page slides out to Right
                (slideInHorizontally(
                    initialOffsetX = { (-it * 0.20f).toInt() },
                    animationSpec = tween(320, easing = FluidDecelEasing)
                ) + fadeIn(
                    animationSpec = tween(240, easing = LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.98f,
                    animationSpec = tween(320, easing = FluidDecelEasing)
                )) togetherWith (
                    slideOutHorizontally(
                        targetOffsetX = { (it * 0.25f).toInt() },
                        animationSpec = tween(300, easing = FluidDecelEasing)
                    ) + fadeOut(
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    )
                )
            } else {
                // Forward navigation into page: Slide in from Right, previous screen pushes to Left
                (slideInHorizontally(
                    initialOffsetX = { (it * 0.25f).toInt() },
                    animationSpec = tween(320, easing = FluidDecelEasing)
                ) + fadeIn(
                    animationSpec = tween(240, easing = LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.98f,
                    animationSpec = tween(320, easing = FluidDecelEasing)
                )) togetherWith (
                    slideOutHorizontally(
                        targetOffsetX = { (-it * 0.20f).toInt() },
                        animationSpec = tween(300, easing = FluidDecelEasing)
                    ) + fadeOut(
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    )
                )
            }
        },
        entryProvider = entryProvider {
            entry<SplashNavKey> {
                SplashScreen(
                    onSplashFinished = {
                        backStack.clear()
                        backStack.add(HomeNavKey)
                    }
                )
            }

            entry<HomeNavKey> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToBrowse = { path ->
                        browseViewModel.navigateTo(path)
                        backStack.add(BrowseNavKey(path))
                    },
                    onNavigateToCategory = { cat ->
                        backStack.add(CategoryNavKey(cat.name))
                    },
                    onNavigateToTrash = {
                        trashViewModel.refresh()
                        backStack.add(TrashNavKey)
                    },
                    onNavigateToSearch = {
                        backStack.add(SearchNavKey())
                    },
                    onNavigateToAnalyzer = {
                        analyzerViewModel.loadVolumesAndAnalyze()
                        backStack.add(AnalyzerNavKey)
                    },
                    onNavigateToSettings = {
                        backStack.add(SettingsNavKey)
                    },
                    onOpenFilePreview = { fileItem ->
                        backStack.add(PreviewNavKey(fileItem.path))
                    }
                )
            }

            entry<BrowseNavKey> { key ->
                BrowseScreen(
                    initialPath = key.path,
                    viewModel = browseViewModel,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    },
                    onOpenFilePreview = { fileItem ->
                        backStack.add(PreviewNavKey(fileItem.path))
                    },
                    onNavigateToSearch = { path ->
                        searchViewModel.setScopePath(path)
                        backStack.add(SearchNavKey(path))
                    }
                )
            }

            entry<CategoryNavKey> { key ->
                val cat = try {
                    FileCategory.valueOf(key.categoryName)
                } catch (_: Exception) {
                    FileCategory.ALL
                }
                CategoryScreen(
                    category = cat,
                    viewModel = categoryViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onOpenFilePreview = { fileItem ->
                        backStack.add(PreviewNavKey(fileItem.path))
                    }
                )
            }

            entry<TrashNavKey> {
                TrashScreen(
                    viewModel = trashViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<SearchNavKey> {
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onOpenFilePreview = { fileItem ->
                        backStack.add(PreviewNavKey(fileItem.path))
                    },
                    onNavigateToDirectory = { path ->
                        browseViewModel.navigateTo(path)
                        backStack.add(BrowseNavKey(path))
                    }
                )
            }

            entry<AnalyzerNavKey> {
                StorageAnalyzerScreen(
                    viewModel = analyzerViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onOpenFilePreview = { fileItem ->
                        backStack.add(PreviewNavKey(fileItem.path))
                    },
                    onNavigateToFolder = { path ->
                        browseViewModel.navigateTo(path)
                        backStack.add(BrowseNavKey(path))
                    }
                )
            }

            entry<PreviewNavKey> { key ->
                val file = File(key.path)
                val item = FileItem(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    sizeBytes = if (file.exists()) file.length() else 0L,
                    lastModified = if (file.exists()) file.lastModified() else 0L,
                    isDirectory = file.isDirectory,
                    extension = file.name.substringAfterLast('.', "").lowercase(),
                    category = categorizeFile(file.name, file.isDirectory)
                )
                FilePreviewSheet(
                    fileItem = item,
                    viewModel = previewViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<SettingsNavKey> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
