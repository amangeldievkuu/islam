package com.sunnahcompanion.app.feature.reader

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnahcompanion.app.audio.AudioPlayer
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import com.sunnahcompanion.app.core.time.TimeProvider
import com.sunnahcompanion.app.data.model.ContentItem
import com.sunnahcompanion.app.data.repository.ContentRepository
import com.sunnahcompanion.app.data.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data object NotFound : ReaderUiState

    @Immutable
    data class Success(
        val item: ContentItem,
        val completedCount: Int,
        val targetCount: Int,
        val isCompleted: Boolean,
        /** In the user's Active 3. */
        val isActive: Boolean,
        /** False when all Active 3 slots are taken by other habits. */
        val canActivate: Boolean,
        val learningMode: Boolean,
        val isPlaying: Boolean,
        /** Playlist index currently playing, for highlighting the ayah; null when idle. */
        val playingIndex: Int?,
    ) : ReaderUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = ReaderViewModel.Factory::class)
class ReaderViewModel @AssistedInject constructor(
    @Assisted private val itemId: String,
    contentRepository: ContentRepository,
    private val habitRepository: HabitRepository,
    private val preferences: AppPreferencesDataSource,
    private val audioPlayer: AudioPlayer,
    time: TimeProvider,
) : ViewModel() {

    private val progress = time.observeToday().flatMapLatest { habitRepository.observeProgress(itemId, it) }

    private val habitSlot = combine(
        habitRepository.observeIsActive(itemId),
        habitRepository.observeHasFreeSlot(),
    ) { active, hasFreeSlot -> HabitSlot(isActive = active, canActivate = active || hasFreeSlot) }

    val uiState: StateFlow<ReaderUiState> = combine(
        contentRepository.observeItem(itemId),
        progress,
        habitSlot,
        preferences.userPreferences.map { it.learningMode }.distinctUntilChanged(),
        audioPlayer.state,
    ) { item, progress, slot, learningMode, playback ->
        if (item == null) return@combine ReaderUiState.NotFound
        val playingThis = playback.itemId == item.id
        ReaderUiState.Success(
            item = item,
            completedCount = progress?.completedCount ?: 0,
            targetCount = progress?.targetCount ?: item.recommendedTargetCount,
            isCompleted = progress?.isCompleted == true,
            isActive = slot.isActive,
            canActivate = slot.canActivate,
            learningMode = learningMode,
            isPlaying = playingThis && playback.isPlaying,
            playingIndex = if (playingThis) playback.index else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState.Loading)

    init {
        // Reflect playback already running (e.g. started from a notification).
        viewModelScope.launch { audioPlayer.ensureConnected() }
    }

    fun onLearningModeChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setLearningMode(enabled) }
    }

    /** Adds to or removes from the Active 3; the toggle is disabled when the slots are full. */
    fun onActiveChange(active: Boolean) {
        viewModelScope.launch {
            if (active) habitRepository.activate(itemId) else habitRepository.deactivate(itemId)
        }
    }

    /** One recitation; completes the day automatically when the target is reached. */
    fun onCountOne() {
        viewModelScope.launch { habitRepository.increment(itemId, 1, TriggerSource.IN_APP) }
    }

    fun onMarkAsRead() {
        viewModelScope.launch { habitRepository.markCompleted(itemId, TriggerSource.IN_APP) }
    }

    fun onPlayPause() {
        val state = uiState.value as? ReaderUiState.Success ?: return
        viewModelScope.launch {
            when {
                state.isPlaying -> audioPlayer.pause()
                state.playingIndex != null -> audioPlayer.resume()
                else -> audioPlayer.play(state.item)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(itemId: String): ReaderViewModel
    }

    private data class HabitSlot(val isActive: Boolean, val canActivate: Boolean)
}
