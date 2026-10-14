package com.sunnahcompanion.app.feature.tasbih

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import com.sunnahcompanion.app.core.datastore.TasbihPreferences
import com.sunnahcompanion.app.core.time.TimeProvider
import com.sunnahcompanion.app.data.model.ContentItem
import com.sunnahcompanion.app.data.repository.ContentRepository
import com.sunnahcompanion.app.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TasbihUiState {
    data object Loading : TasbihUiState

    @Immutable
    data class Ready(
        val options: ImmutableList<ContentItem>,
        val selected: ContentItem,
        val target: Int,
        val count: Int,
        val rounds: Int,
        /** Total logged for this dhikr today, across sessions and devices' notifications. */
        val todayTotal: Int,
    ) : TasbihUiState {
        val isRoundComplete: Boolean get() = count >= target
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TasbihViewModel @Inject constructor(
    contentRepository: ContentRepository,
    private val habitRepository: HabitRepository,
    private val preferences: AppPreferencesDataSource,
    private val haptics: TasbihHaptics,
    time: TimeProvider,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    /** In-memory counter for instant, lossless taps; persisted to DataStore with a debounce. */
    private val counter = MutableStateFlow<TasbihPreferences?>(null)

    private val options = contentRepository.observeKind(ContentKind.DHIKR)

    init {
        viewModelScope.launch { counter.value = preferences.tasbih.first() }
        viewModelScope.launch {
            counter.filterNotNull().drop(1).debounce(PERSIST_DEBOUNCE_MS).collect { preferences.setTasbih(it) }
        }
    }

    private val selected = combine(options, counter.filterNotNull().map { it.dhikrId }.distinctUntilChanged()) {
            options, dhikrId ->
        options.firstOrNull { it.id == dhikrId }
            ?: options.firstOrNull { it.id == DEFAULT_DHIKR }
            ?: options.firstOrNull()
    }.distinctUntilChanged()

    // Only re-queried when the selected dhikr or the day changes, never per tap.
    private val todayTotal = selected.flatMapLatest { item ->
        if (item == null) {
            flowOf(0)
        } else {
            time.observeToday()
                .flatMapLatest { habitRepository.observeProgress(item.id, it) }
                .map { it?.completedCount ?: 0 }
        }
    }

    val uiState: StateFlow<TasbihUiState> = combine(
        options,
        selected,
        counter.filterNotNull(),
        todayTotal,
    ) { options, selected, counter, total ->
        if (selected == null) return@combine TasbihUiState.Loading
        TasbihUiState.Ready(
            options = options.toImmutableList(),
            selected = selected,
            target = counter.target ?: selected.recommendedTargetCount,
            count = counter.count,
            rounds = counter.rounds,
            todayTotal = total,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasbihUiState.Loading)

    fun onTap() {
        val state = uiState.value as? TasbihUiState.Ready ?: return
        val current = counter.value ?: return
        val step = TasbihCounter.next(current.count, state.target)
        haptics.perform(step.feedback)
        counter.value = current.copy(
            dhikrId = state.selected.id,
            target = state.target,
            count = step.count,
            rounds = current.rounds + if (step.completedRound) 1 else 0,
        )
        if (step.completedRound) {
            // Outlives the screen so a round finished just before leaving is still logged.
            applicationScope.launch {
                habitRepository.increment(state.selected.id, state.target, TriggerSource.TASBIH)
            }
        }
    }

    fun onSelectDhikr(item: ContentItem) {
        counter.update {
            TasbihPreferences(dhikrId = item.id, target = item.recommendedTargetCount, count = 0, rounds = 0)
        }
    }

    fun onSelectTarget(target: Int) {
        counter.update { it?.copy(target = target, count = 0) }
    }

    fun onReset() {
        counter.update { it?.copy(count = 0, rounds = 0) }
    }

    override fun onCleared() {
        // Flush the latest count immediately rather than waiting for the debounce.
        counter.value?.let { last -> applicationScope.launch { preferences.setTasbih(last) } }
    }

    private companion object {
        const val DEFAULT_DHIKR = "dhikr_subhanallah"
        const val PERSIST_DEBOUNCE_MS = 400L
    }
}
