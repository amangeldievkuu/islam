package com.sunnahcompanion.app.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.time.TimeProvider
import com.sunnahcompanion.app.data.model.ContentItem
import com.sunnahcompanion.app.data.model.DayProgress
import com.sunnahcompanion.app.data.model.HabitItem
import com.sunnahcompanion.app.data.repository.ContentRepository
import com.sunnahcompanion.app.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Success(
        val today: LocalDate,
        val timeOfDay: TimeOfDay,
        /** Today's Active 3 habits. */
        val checklist: ImmutableList<HabitItem>,
        val week: ImmutableList<DayProgress>,
        /** null = today's checklist; otherwise the situational filter in effect. */
        val selectedCategory: LifeSituation?,
        val categoryItems: ImmutableList<ContentItem>,
        /** The life situation that fits right now, offered as a shortcut. */
        val suggestion: LifeSituation?,
    ) : HomeUiState {
        val completedCount: Int get() = checklist.count { it.isCompleted }
        val progress: Float get() = if (checklist.isEmpty()) 0f else completedCount.toFloat() / checklist.size
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val contentRepository: ContentRepository,
    private val time: TimeProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectedCategory = savedStateHandle.getStateFlow<LifeSituation?>(KEY_CATEGORY, null)

    private val categoryItems = selectedCategory.flatMapLatest { category ->
        if (category == null) flowOf(emptyList()) else contentRepository.observeSituation(category)
    }

    val uiState: StateFlow<HomeUiState> = time.observeToday()
        .flatMapLatest { today ->
            combine(
                habitRepository.observeActiveHabits(today),
                habitRepository.observeWeek(today),
                selectedCategory,
                categoryItems,
            ) { checklist, week, category, items ->
                val now = time.now()
                HomeUiState.Success(
                    today = today,
                    timeOfDay = timeOfDay(now),
                    checklist = checklist.toImmutableList(),
                    week = week.toImmutableList(),
                    selectedCategory = category,
                    categoryItems = items.toImmutableList(),
                    suggestion = suggestionFor(now),
                )
            }
        }
        .map<HomeUiState.Success, HomeUiState> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun onCategorySelected(category: LifeSituation?) {
        savedStateHandle[KEY_CATEGORY] = category
    }

    /** Ticking marks the item read for today; un-ticking clears today's progress. */
    fun onChecklistToggled(item: HabitItem) {
        viewModelScope.launch {
            if (item.isCompleted) {
                habitRepository.clearToday(item.item.id)
            } else {
                habitRepository.markCompleted(item.item.id, TriggerSource.IN_APP)
            }
        }
    }

    private fun timeOfDay(now: ZonedDateTime) = when (now.hour) {
        in 4..11 -> TimeOfDay.MORNING
        in 12..16 -> TimeOfDay.AFTERNOON
        in 17..20 -> TimeOfDay.EVENING
        else -> TimeOfDay.NIGHT
    }

    private fun suggestionFor(now: ZonedDateTime): LifeSituation? = when {
        now.dayOfWeek == DayOfWeek.FRIDAY && now.hour in 6..17 -> LifeSituation.FRIDAY
        now.hour in 4..10 -> LifeSituation.MORNING
        now.hour in 15..19 -> LifeSituation.EVENING
        now.hour >= 21 || now.hour < 2 -> LifeSituation.BEFORE_SLEEP
        else -> null
    }

    private companion object {
        const val KEY_CATEGORY = "selected_category"
    }
}
