package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.di.DefaultDispatcher
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.utils.DevPrintFileNameBuilder
import com.zephyr.boreal.domain.utils.PrintEndErrandTextBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val NO_ARG = -1

data class RoundDetailsUiState(
  val isLoading: Boolean = true,
  val round: Round? = null,
  val textToPrint: String? = null,
  val suggestedFileName: String = "",
)

@HiltViewModel
class RoundDetailsViewModel
  @Inject
  constructor(
    savedStateHandle: SavedStateHandle,
    private val roundsRepository: RoundsRepository,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
  ) : ViewModel() {
    private val roundId: Int = savedStateHandle.get<Int>("roundId") ?: NO_ARG

    private val _uiState = MutableStateFlow(RoundDetailsUiState())
    val uiState: StateFlow<RoundDetailsUiState> = _uiState.asStateFlow()

    init {
      viewModelScope.launch {
        roundsRepository.getRounds().collect { resource ->
          val round = resource.getOrNull()?.find { it.id == roundId }

          val textToPrint =
            if (round?.lastSerialNumber != null) {
              withContext(defaultDispatcher) { PrintEndErrandTextBuilder.build(round) }
            } else {
              null
            }
          val suggestedFileName = round?.let { DevPrintFileNameBuilder.buildForRound(it) } ?: ""

          _uiState.update {
            it.copy(
              isLoading = resource.isLoading,
              round = round,
              textToPrint = textToPrint,
              suggestedFileName = suggestedFileName,
            )
          }
        }
      }
    }
  }
