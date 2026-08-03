package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.domain.model.Round
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RoundListRowUiModel(
  val id: Int,
  val storeName: String,
  val roundStarted: String,
  val roundFinished: String?,
)

data class RoundsListUiState(
  val isLoading: Boolean = true,
  val rounds: List<RoundListRowUiModel> = emptyList(),
)

private fun Round.toRowUiModel() =
  RoundListRowUiModel(
    id = id,
    storeName = store.name,
    roundStarted = roundStarted,
    roundFinished = roundFinished,
  )

@HiltViewModel
class RoundsListViewModel
  @Inject
  constructor(
    roundsRepository: RoundsRepository,
  ) : ViewModel() {
    val uiState: StateFlow<RoundsListUiState> =
      roundsRepository
        .getRounds()
        .map { resource ->
          RoundsListUiState(
            isLoading = resource.isLoading,
            rounds = resource.getOrNull()?.map { it.toRowUiModel() } ?: emptyList(),
          )
        }.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000),
          initialValue = RoundsListUiState(),
        )
  }
