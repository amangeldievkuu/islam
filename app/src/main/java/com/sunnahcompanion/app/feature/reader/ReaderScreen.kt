package com.sunnahcompanion.app.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.data.model.Ayah
import com.sunnahcompanion.app.data.model.ContentItem
import com.sunnahcompanion.app.ui.components.ArabicText
import com.sunnahcompanion.app.ui.components.RevealableText
import com.sunnahcompanion.app.ui.theme.SunnahTheme

/** Tanzil Uthmani basmala (from 1:1), shown above each surah that begins in this item. */
private const val BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

@Composable
fun ReaderScreen(viewModel: ReaderViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReaderScreen(
        uiState = uiState,
        onBack = onBack,
        onLearningModeChange = viewModel::onLearningModeChange,
        onActiveChange = viewModel::onActiveChange,
        onCountOne = viewModel::onCountOne,
        onMarkAsRead = viewModel::onMarkAsRead,
        onPlayPause = viewModel::onPlayPause,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScreen(
    uiState: ReaderUiState,
    onBack: () -> Unit,
    onLearningModeChange: (Boolean) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onCountOne: () -> Unit,
    onMarkAsRead: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val state = uiState as? ReaderUiState.Success
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(state?.item?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    if (state != null) {
                        TooltipIconToggle(
                            checked = state.learningMode,
                            onCheckedChange = onLearningModeChange,
                            icon = Icons.Rounded.School,
                            label = stringResource(R.string.reader_learning_mode),
                        )
                        TooltipIconToggle(
                            checked = state.isActive,
                            onCheckedChange = onActiveChange,
                            enabled = state.canActivate,
                            icon = if (state.isActive) Icons.Rounded.PlaylistAddCheck else Icons.Rounded.PlaylistAdd,
                            label = stringResource(
                                when {
                                    state.isActive -> R.string.reader_deactivate
                                    state.canActivate -> R.string.reader_activate
                                    else -> R.string.reader_slots_full
                                },
                            ),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (state != null) {
                ReaderActionBar(state = state, onCountOne = onCountOne, onMarkAsRead = onMarkAsRead, onPlayPause = onPlayPause)
            }
        },
    ) { innerPadding ->
        when (uiState) {
            ReaderUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }

            ReaderUiState.NotFound -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                Text(stringResource(R.string.reader_not_found))
            }

            is ReaderUiState.Success -> ReaderContent(uiState, innerPadding)
        }
    }
}

@Composable
private fun ReaderContent(state: ReaderUiState.Success, contentPadding: PaddingValues) {
    // Revealed transliterations in Learning Mode; re-hidden whenever the mode is toggled.
    var revealed by rememberSaveable(state.learningMode) { mutableStateOf(emptyList<Int>()) }
    val isHidden: (Int) -> Boolean = { state.learningMode && it !in revealed }
    val reveal: (Int) -> Unit = { revealed = revealed + it }
    val item = state.item

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "virtue") { VirtueCard(item.hadithVirtue, item.virtueReference) }
        if (item.sababAndAdabTip.isNotBlank()) {
            item(key = "sabab") { SababCard(item.sababAndAdabTip) }
        }
        if (item.kind == ContentKind.SURAH) {
            itemsIndexed(item.ayahs, key = { _, a -> "${a.surahNumber}:${a.ayahNumber}" }) { index, ayah ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (ayah.ayahNumber == 1 && ayah.surahNumber != 1 && ayah.surahNumber != 9) {
                        ArabicText(
                            text = BASMALA,
                            style = SunnahTheme.arabic.compact,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    AyahCard(
                        ayah = ayah,
                        highlighted = state.playingIndex == index && item.audioUrl == null,
                        transliterationHidden = isHidden(index),
                        onReveal = { reveal(index) },
                    )
                }
            }
        } else {
            item(key = "dua") {
                DuaCard(item = item, transliterationHidden = isHidden(0), onReveal = { reveal(0) })
            }
        }
    }
}

@Composable
private fun VirtueCard(virtue: String, reference: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.AutoStories, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.reader_virtue), style = MaterialTheme.typography.labelLarge)
                if (virtue.isNotBlank()) Text(virtue, style = MaterialTheme.typography.bodyMedium)
                Text(reference, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** The practical action (sabab) or courtesy (adab) that goes with the words. */
@Composable
private fun SababCard(tip: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.reader_sabab_adab), style = MaterialTheme.typography.labelLarge)
                Text(tip, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DuaCard(item: ContentItem, transliterationHidden: Boolean, onReveal: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ArabicText(text = item.arabicText)
            HorizontalDivider()
            SectionLabel(stringResource(R.string.reader_transliteration))
            RevealableText(
                text = item.transliteration,
                hidden = transliterationHidden,
                onReveal = onReveal,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            )
            SectionLabel(stringResource(R.string.reader_translation))
            Text(item.translation, style = MaterialTheme.typography.bodyLarge)
            if (item.ayahs.isNotEmpty()) {
                val first = item.ayahs.first()
                val last = item.ayahs.last()
                Text(
                    text = stringResource(
                        R.string.reader_audio_quran_note,
                        first.surahNumber,
                        "${first.ayahNumber}–${last.ayahNumber}",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AyahCard(ayah: Ayah, highlighted: Boolean, transliterationHidden: Boolean, onReveal: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    text = "${ayah.surahNumber}:${ayah.ayahNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            ArabicText(text = ayah.arabicText)
            RevealableText(
                text = ayah.transliteration,
                hidden = transliterationHidden,
                onReveal = onReveal,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            )
            Text(ayah.translation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ReaderActionBar(
    state: ReaderUiState.Success,
    onCountOne: () -> Unit,
    onMarkAsRead: () -> Unit,
    onPlayPause: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.item.hasAudio) {
                FilledTonalIconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (state.isPlaying) R.string.reader_pause else R.string.reader_play),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reader_today_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (state.targetCount > 1) {
                        "${state.completedCount} / ${state.targetCount}"
                    } else {
                        stringResource(if (state.isCompleted) R.string.reader_status_read else R.string.reader_status_not_read)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            when {
                state.isCompleted -> OutlinedButton(onClick = onCountOne) {
                    Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp))
                    Text(stringResource(R.string.reader_read_again), Modifier.padding(start = 8.dp))
                }

                state.targetCount > 1 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onMarkAsRead) { Text(stringResource(R.string.reader_complete)) }
                    Button(onClick = onCountOne) { Text(stringResource(R.string.reader_count_one)) }
                }

                else -> Button(onClick = onMarkAsRead) {
                    Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp))
                    Text(stringResource(R.string.reader_mark_as_read), Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        if (checked) {
            FilledIconToggleButton(checked = true, onCheckedChange = onCheckedChange, enabled = enabled) {
                Icon(icon, contentDescription = label)
            }
        } else {
            IconToggleButton(checked = false, onCheckedChange = onCheckedChange, enabled = enabled) {
                Icon(icon, contentDescription = label)
            }
        }
    }
}
