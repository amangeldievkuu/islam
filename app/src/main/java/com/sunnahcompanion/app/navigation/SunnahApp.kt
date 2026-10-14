package com.sunnahcompanion.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.feature.category.CategoryScreen
import com.sunnahcompanion.app.feature.category.CategoryViewModel
import com.sunnahcompanion.app.feature.home.HomeScreen
import com.sunnahcompanion.app.feature.reader.ReaderScreen
import com.sunnahcompanion.app.feature.reader.ReaderViewModel
import com.sunnahcompanion.app.feature.settings.SettingsScreen
import com.sunnahcompanion.app.feature.tasbih.TasbihScreen

/**
 * App shell: adaptive navigation (bottom bar / rail) around a Navigation 3 [NavDisplay].
 * The back stack always starts at [HomeKey]. Lists and the reader form a list-detail
 * scene, so on wide windows the reader opens beside the list.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SunnahApp(
    deepLink: NavKey?,
    onDeepLinkHandled: () -> Unit,
) {
    val backStack = rememberNavBackStack(HomeKey)

    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            backStack.openOnTop(deepLink)
            onDeepLinkHandled()
        }
    }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isCompact = !adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val currentTopLevel = backStack.currentTopLevel()
    // On phones the reader is full screen with its own action bar.
    val hideNavigation = isCompact && backStack.lastOrNull() is ReaderKey

    NavigationSuiteScaffold(
        navigationSuiteType = if (hideNavigation) {
            NavigationSuiteType.None
        } else {
            NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
        },
        navigationItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination == currentTopLevel
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { backStack.selectTopLevel(destination) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        },
    ) {
        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            sceneStrategies = listOf(listDetailStrategy),
            entryProvider = entryProvider {
                entry<HomeKey>(
                    metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder() }),
                ) {
                    HomeScreen(
                        onOpenItem = { backStack.openDetail(ReaderKey(it)) },
                        onOpenCategory = { backStack.add(CategoryKey(it)) },
                    )
                }
                entry<CategoryKey>(
                    metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder() }),
                ) { key ->
                    CategoryScreen(
                        viewModel = hiltViewModel<CategoryViewModel, CategoryViewModel.Factory>(
                            creationCallback = { it.create(key.category) },
                        ),
                        onBack = { backStack.removeLastOrNull() },
                        onOpenItem = { backStack.openDetail(ReaderKey(it)) },
                    )
                }
                entry<ReaderKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    ReaderScreen(
                        viewModel = hiltViewModel<ReaderViewModel, ReaderViewModel.Factory>(
                            creationCallback = { it.create(key.itemId) },
                        ),
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<TasbihKey> { TasbihScreen() }
                entry<SettingsKey> { SettingsScreen() }
            },
        )
    }
}

@Composable
private fun DetailPlaceholder() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.detail_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun NavBackStack<NavKey>.currentTopLevel(): TopLevelDestination =
    asReversed().firstNotNullOfOrNull { key -> TopLevelDestination.entries.firstOrNull { it.key == key } }
        ?: TopLevelDestination.HOME

/** Home is always the root, so Back from another tab returns to Today. */
private fun NavBackStack<NavKey>.selectTopLevel(destination: TopLevelDestination) {
    if (currentTopLevel() == destination && size > 1 && last() == destination.key) return
    removeAll { it != HomeKey }
    if (destination.key != HomeKey) add(destination.key)
}

/** Replaces an open reader instead of stacking readers (keeps two-pane layouts tidy). */
private fun NavBackStack<NavKey>.openDetail(key: ReaderKey) {
    if (lastOrNull() is ReaderKey) set(lastIndex, key) else add(key)
}

private fun NavBackStack<NavKey>.openOnTop(key: NavKey) {
    when (key) {
        is ReaderKey -> openDetail(key)
        else -> if (lastOrNull() != key) add(key)
    }
}
