package com.sunnahcompanion.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.DeedPriority
import com.sunnahcompanion.app.core.database.model.HabitStatus
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.data.model.ContentItem
import com.sunnahcompanion.app.data.model.DayProgress
import com.sunnahcompanion.app.data.model.HabitItem
import com.sunnahcompanion.app.ui.components.ContentRow
import com.sunnahcompanion.app.ui.components.ProgressRing
import com.sunnahcompanion.app.ui.components.icon
import com.sunnahcompanion.app.ui.components.titleRes
import com.sunnahcompanion.app.ui.theme.SunnahTheme
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun HomeScreen(
    onOpenItem: (String) -> Unit,
    onOpenCategory: (LifeSituation) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onOpenItem = onOpenItem,
        onOpenCategory = onOpenCategory,
        onCategorySelected = viewModel::onCategorySelected,
        onChecklistToggled = viewModel::onChecklistToggled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onOpenItem: (String) -> Unit,
    onOpenCategory: (LifeSituation) -> Unit,
    onCategorySelected: (LifeSituation?) -> Unit,
    onChecklistToggled: (HabitItem) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state = uiState as? HomeUiState.Success
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(greetingFor(state?.timeOfDay)))
                        if (state != null) {
                            Text(
                                text = formatDates(state.today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when (uiState) {
            HomeUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }

            is HomeUiState.Success -> HomeContent(
                state = uiState,
                contentPadding = innerPadding,
                onOpenItem = onOpenItem,
                onOpenCategory = onOpenCategory,
                onCategorySelected = onCategorySelected,
                onChecklistToggled = onChecklistToggled,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    contentPadding: PaddingValues,
    onOpenItem: (String) -> Unit,
    onOpenCategory: (LifeSituation) -> Unit,
    onCategorySelected: (LifeSituation?) -> Unit,
    onChecklistToggled: (HabitItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "progress", contentType = "progress") {
            ProgressCard(
                completed = state.completedCount,
                total = state.checklist.size,
                progress = state.progress,
                week = state.week,
                today = state.today,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        state.suggestion?.let { suggestion ->
            item(key = "suggestion", contentType = "suggestion") {
                SuggestionCard(
                    category = suggestion,
                    onClick = { onOpenCategory(suggestion) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        item(key = "filters", contentType = "filters") {
            SituationFilters(
                selected = state.selectedCategory,
                onSelected = onCategorySelected,
            )
        }
        if (state.selectedCategory == null) {
            if (state.checklist.isEmpty()) {
                item(key = "empty", contentType = "empty") { EmptyChecklist() }
            } else {
                items(state.checklist, key = { "check_${it.item.id}" }, contentType = { "checklist" }) { entry ->
                    ChecklistRow(
                        entry = entry,
                        onToggle = { onChecklistToggled(entry) },
                        onClick = { onOpenItem(entry.item.id) },
                    )
                }
            }
        } else {
            items(state.categoryItems, key = { "item_${it.id}" }, contentType = { "content" }) { item ->
                ContentRow(item = item, onClick = { onOpenItem(item.id) })
            }
        }
    }
}

@Composable
private fun ProgressCard(
    completed: Int,
    total: Int,
    progress: Float,
    week: ImmutableList<DayProgress>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val summary = pluralStringResource(R.plurals.home_progress_summary, total, completed, total)
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                ProgressRing(progress = progress, size = 96.dp, strokeWidth = 10.dp) {
                    Text(
                        text = "$completed/$total",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = summary },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.home_progress_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(encouragementFor(completed, total)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            WeekStrip(week = week, today = today)
        }
    }
}

/** Seven-day consistency view. Partial days show partial rings: effort counts, no streak to break. */
@Composable
private fun WeekStrip(week: ImmutableList<DayProgress>, today: LocalDate) {
    val locale = LocalLocale.current.platformLocale
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { day ->
            val isToday = day.date == today
            val label = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
            val description = stringResource(
                R.string.home_week_day_description,
                day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                day.completed,
                day.total,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    ProgressRing(
                        progress = day.fraction,
                        size = 32.dp,
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(3.dp),
                    ) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(category: LifeSituation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(category.icon, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_suggestion_label), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(category.titleRes), style = MaterialTheme.typography.titleMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun SituationFilters(selected: LifeSituation?, onSelected: (LifeSituation?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "today") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text(stringResource(R.string.home_filter_today)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Checklist, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }
        items(LifeSituation.chips, key = { it.name }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(if (selected == category) null else category) },
                label = { Text(stringResource(category.titleRes)) },
                leadingIcon = {
                    Icon(category.icon, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }
    }
}

@Composable
private fun ChecklistRow(entry: HabitItem, onToggle: () -> Unit, onClick: () -> Unit) {
    val progressText = if (entry.targetCount > 1) {
        stringResource(R.string.home_checklist_count, entry.completedCount.coerceAtMost(entry.targetCount), entry.targetCount)
    } else {
        null
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Checkbox(
                checked = entry.isCompleted,
                onCheckedChange = { onToggle() },
            )
        },
        headlineContent = { Text(entry.item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = listOfNotNull(progressText, entry.item.transliteration).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun EmptyChecklist() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(0.dp))
    }
}

private fun greetingFor(timeOfDay: TimeOfDay?) = when (timeOfDay) {
    TimeOfDay.MORNING -> R.string.home_greeting_morning
    TimeOfDay.EVENING -> R.string.home_greeting_evening
    TimeOfDay.NIGHT -> R.string.home_greeting_night
    TimeOfDay.AFTERNOON, null -> R.string.home_greeting
}

private fun encouragementFor(completed: Int, total: Int) = when {
    total == 0 -> R.string.home_encourage_empty
    completed == 0 -> R.string.home_encourage_start
    completed >= total -> R.string.home_encourage_done
    else -> R.string.home_encourage_progress
}

@Composable
private fun formatDates(today: LocalDate): String {
    val locale = LocalLocale.current.platformLocale
    return remember(today, locale) { formatDates(today, locale) }
}

private fun formatDates(today: LocalDate, locale: Locale): String {
    val gregorian = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    val hijri = runCatching {
        HijrahDate.from(today).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withChronology(HijrahChronology.INSTANCE).withLocale(locale),
        )
    }.getOrNull()
    return listOfNotNull(gregorian, hijri).joinToString(" · ")
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val today = LocalDate.of(2026, 10, 14)
    val dua = ContentItem(
        id = "dua_leaving_home",
        kind = ContentKind.DUA,
        priority = DeedPriority.MUSTAHAB,
        title = "Leaving the Home",
        arabicText = "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ",
        transliteration = "Bismillah, tawakkaltu 'alallah",
        translation = "In the name of Allah, I place my trust in Allah.",
        audioUrl = null,
        recommendedTargetCount = 1,
        hadithVirtue = "It is said to him: \"You are guided, defended and protected.\"",
        virtueReference = "Abu Dawud 5095",
        sababAndAdabTip = "Check your keys and step out with your right foot.",
        anchorPrayer = null,
        isTiredModeEssential = false,
        surahNumber = null,
    )
    SunnahTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                today = today,
                timeOfDay = TimeOfDay.MORNING,
                checklist = persistentListOf(
                    HabitItem(dua, 1, 1, true, HabitStatus.ACTIVE, today.minusDays(5), null),
                    HabitItem(dua.copy(id = "quls", title = "The Three Quls"), 3, 1, false, HabitStatus.ACTIVE, today, null),
                ),
                week = (6 downTo 0).map { DayProgress(today.minusDays(it.toLong()), it % 3, 3) }.toImmutableList(),
                selectedCategory = null,
                categoryItems = persistentListOf(),
                suggestion = LifeSituation.MORNING,
            ),
            onOpenItem = {},
            onOpenCategory = {},
            onCategorySelected = {},
            onChecklistToggled = {},
        )
    }
}
