package com.sunnahcompanion.app.feature.tasbih

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.data.model.ContentItem
import com.sunnahcompanion.app.ui.components.ArabicText
import com.sunnahcompanion.app.ui.components.ProgressRing
import com.sunnahcompanion.app.ui.theme.SunnahTheme

private val TARGETS = listOf(33, 100)

@Composable
fun TasbihScreen(viewModel: TasbihViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TasbihScreen(
        uiState = uiState,
        onTap = viewModel::onTap,
        onSelectDhikr = viewModel::onSelectDhikr,
        onSelectTarget = viewModel::onSelectTarget,
        onReset = viewModel::onReset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TasbihScreen(
    uiState: TasbihUiState,
    onTap: () -> Unit,
    onSelectDhikr: (ContentItem) -> Unit,
    onSelectTarget: (Int) -> Unit,
    onReset: () -> Unit,
) {
    // Counting can take minutes; don't let the screen sleep mid-dhikr.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_tasbih)) },
                actions = {
                    IconButton(onClick = onReset, enabled = uiState is TasbihUiState.Ready) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.tasbih_reset))
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            TasbihUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }

            is TasbihUiState.Ready -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val chipsState = rememberLazyListState()
                val selectedIndex = uiState.options.indexOfFirst { it.id == uiState.selected.id }
                LaunchedEffect(selectedIndex) {
                    if (selectedIndex >= 0) chipsState.animateScrollToItem(selectedIndex)
                }
                LazyRow(
                    state = chipsState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.options, key = { it.id }) { option ->
                        FilterChip(
                            selected = option.id == uiState.selected.id,
                            onClick = { onSelectDhikr(option) },
                            label = { Text(option.title) },
                        )
                    }
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    TARGETS.forEachIndexed { index, target ->
                        SegmentedButton(
                            selected = uiState.target == target,
                            onClick = { onSelectTarget(target) },
                            shape = SegmentedButtonDefaults.itemShape(index, TARGETS.size),
                        ) { Text("×$target") }
                    }
                }
                TapZone(
                    state = uiState,
                    onTap = onTap,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            }
        }
    }
}

/** The whole area is one big button, so counting never needs a look at the screen. */
@Composable
private fun TapZone(state: TasbihUiState.Ready, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val countLabel = stringResource(R.string.tasbih_count_action)
    val description = stringResource(R.string.tasbih_count_description, state.selected.title, state.count, state.target)
    Surface(
        onClick = onTap,
        shape = MaterialTheme.shapes.extraLarge,
        color = if (state.isRoundComplete) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (state.isRoundComplete) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
            stateDescription = description
            onClick(label = countLabel) { onTap(); true }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArabicText(
                    text = state.selected.arabicText,
                    style = SunnahTheme.arabic.display,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                Text(
                    text = state.selected.transliteration,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            ProgressRing(
                progress = state.count.toFloat() / state.target,
                size = 220.dp,
                strokeWidth = 14.dp,
                color = if (state.isRoundComplete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = state.count,
                        transitionSpec = {
                            (slideInVertically(tween(120)) { it / 3 } + fadeIn(tween(120))) togetherWith
                                (slideOutVertically(tween(120)) { -it / 3 } + fadeOut(tween(120)))
                        },
                        label = "count",
                    ) { count ->
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text("/ ${state.target}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (state.isRoundComplete) {
                        stringResource(R.string.tasbih_round_complete)
                    } else {
                        stringResource(R.string.tasbih_tap_hint)
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.tasbih_stats, state.rounds, state.todayTotal),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
