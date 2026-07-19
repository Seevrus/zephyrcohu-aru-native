package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.api.dto.request.FinishRoundRequestDataDto
import com.zephyr.boreal.data.mapper.toFinishRoundReceiptDto
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EndErrandUiState(
  val isLoading: Boolean = false,
  val isEndErrandPending: Boolean = false,
  val errorMessage: String? = null,
  val isInternetReachable: Boolean = true,
  val isUserPending: Boolean = true,
  val canFinishRound: Boolean = false,
  val disableReasonResId: Int? = null,
)

@HiltViewModel
class EndErrandViewModel
  @Inject
  constructor(
    private val roundsRepository: RoundsRepository,
    private val userRepository: UserRepository,
    private val storesRepository: StoresRepository,
    private val receiptsStore: ReceiptsStore,
    connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(EndErrandUiState())
    val uiState: StateFlow<EndErrandUiState> =
      combine(
        _uiState,
        connectivityObserver.isInternetReachable,
      ) { state, isOnline ->
        state.copy(isInternetReachable = isOnline)
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EndErrandUiState(),
      )

    init {
      checkActiveRound()
    }

    private fun checkActiveRound() {
      viewModelScope.launch {
        combine(
          userRepository.getCurrentUser(),
          storesRepository.getStores(),
        ) { userResource, storesResource ->
          val user = userResource.getOrNull()
          val stores = storesResource.getOrNull()

          val hasActiveRound = user?.lastRound != null
          val storeInUseId = user?.storeInUseId
          val activeStore = stores?.find { it.id == storeInUseId }

          val disableReasonResId =
            when {
              !hasActiveRound -> null
              storeInUseId == null -> com.zephyr.boreal.R.string.end_errand_error_no_store
              activeStore == null -> com.zephyr.boreal.R.string.end_errand_error_store_not_found
              else -> null
            }

          _uiState.update {
            it.copy(
              isUserPending = false,
              canFinishRound = hasActiveRound && disableReasonResId == null,
              disableReasonResId = disableReasonResId,
            )
          }
        }.collect { }
      }
    }

    fun finishRound(onSuccess: () -> Unit) {
      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val userRes = userRepository.getCurrentUser().firstOrNull()
        val user = userRes?.getOrNull()
        val activeRound = user?.lastRound ?: return@launch

        val storeInUseId = user.storeInUseId
        val storeRes = storesRepository.getStores().firstOrNull()?.getOrNull()
        val activeStore = storeRes?.find { it.id == storeInUseId }

        val receipts = receiptsStore.receipts.value
        val lastSerialNumber =
          calculateLastSerialNumber(
            firstAvailableSerialNumber = activeStore?.firstAvailableSerialNumber ?: 1,
            receipts = receipts,
          )

        val request =
          FinishRoundRequestDataDto(
            roundId = activeRound.id,
            lastSerialNumber = lastSerialNumber,
            yearCode = activeStore?.yearCode,
            receipts = receipts.map { it.toFinishRoundReceiptDto() },
          )

        _uiState.update { it.copy(isEndErrandPending = true) }
        when (val result = roundsRepository.finishRound(request)) {
          is ApiResource.Success -> {
            // Refresh user state so the app knows the round is closed
            userRepository.refreshCurrentUser()

            _uiState.update { it.copy(isLoading = false, isEndErrandPending = false) }
            onSuccess()
          }
          is ApiResource.Error -> {
            val errorMsg = if (user.isDev) result.message else "Kör zárása sikertelen."
            handleError(errorMsg)
          }
          is ApiResource.Loading -> {}
        }
      }
    }

    private fun handleError(message: String?) {
      _uiState.update {
        it.copy(
          isLoading = false,
          isEndErrandPending = false,
          errorMessage = message,
        )
      }
    }

    private fun calculateLastSerialNumber(
      firstAvailableSerialNumber: Int,
      receipts: List<com.zephyr.boreal.domain.model.Receipt>,
    ): Int {
      if (receipts.isEmpty()) {
        return maxOf(0, firstAvailableSerialNumber - 1)
      }
      return receipts.maxOf { maxOf(it.serialNumber, it.cancelSerialNumber ?: 0) }
    }
  }
