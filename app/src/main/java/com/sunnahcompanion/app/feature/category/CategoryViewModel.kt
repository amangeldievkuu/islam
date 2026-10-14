package com.sunnahcompanion.app.feature.category

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.data.model.ContentItem
import com.sunnahcompanion.app.data.repository.ContentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class CategoryUiState(
    val category: LifeSituation,
    val items: ImmutableList<ContentItem>? = null,
)

@HiltViewModel(assistedFactory = CategoryViewModel.Factory::class)
class CategoryViewModel @AssistedInject constructor(
    @Assisted category: LifeSituation,
    contentRepository: ContentRepository,
) : ViewModel() {

    val uiState: StateFlow<CategoryUiState> = contentRepository.observeSituation(category)
        .map { CategoryUiState(category, it.toImmutableList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryUiState(category))

    @AssistedFactory
    interface Factory {
        fun create(category: LifeSituation): CategoryViewModel
    }
}
