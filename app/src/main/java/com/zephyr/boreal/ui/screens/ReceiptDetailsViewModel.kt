package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.api.dto.request.CreateCancelReceiptDataDto
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ReceiptsRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StoreSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NO_ARG = -1

data class ReceiptDetailsUiState(
  val isLoading: Boolean = true,
  val receipt: Receipt? = null,
  val isInternetReachable: Boolean = true,
  val hasStore: Boolean = true,
  val isCancelInProgress: Boolean = false,
  val hasCanceledNow: Boolean = false,
  val errorMessage: String? = null,
) {
  val canCancel: Boolean
    get() = receipt != null && !receipt.isCanceled && isInternetReachable && receipt.id > 0 && hasStore
}

@HiltViewModel
class ReceiptDetailsViewModel
  @Inject
  constructor(
    savedStateHandle: SavedStateHandle,
    private val receiptsStore: ReceiptsStore,
    private val receiptsRepository: ReceiptsRepository,
    private val storeSessionStore: StoreSessionStore,
    private val userRepository: UserRepository,
    connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val serialNumber: Int = savedStateHandle.get<Int>("serialNumber") ?: NO_ARG

    private val _uiState = MutableStateFlow(ReceiptDetailsUiState())
    val uiState: StateFlow<ReceiptDetailsUiState> = _uiState.asStateFlow()

    init {
      viewModelScope.launch {
        combine(
          receiptsStore.receiptBySerialNumber(serialNumber),
          connectivityObserver.isInternetReachable,
          storeSessionStore.selectedStoreCurrentState,
        ) { receipt, isOnline, store -> Triple(receipt, isOnline, store) }
          .collect { (receipt, isOnline, store) ->
            _uiState.update {
              it.copy(
                isLoading = false,
                receipt = receipt,
                isInternetReachable = isOnline,
                hasStore = store != null,
              )
            }
          }
      }
    }

    fun requestCancel() {
      val state = _uiState.value
      val receipt = state.receipt
      val store = storeSessionStore.selectedStoreCurrentState.value

      viewModelScope.launch {
        _uiState.update { it.copy(isCancelInProgress = true, errorMessage = null) }
        try {
          if (receipt == null) {
            _uiState.update { it.copy(errorMessage = "Számla nem található.") }
            return@launch
          }
          if (store == null) {
            _uiState.update { it.copy(errorMessage = "Kör raktár nem talákható.") }
            return@launch
          }

          val nextSerialNumber = receiptsStore.nextAvailableSerialNumber(store.firstAvailableSerialNumber ?: 0)
          val request =
            CreateCancelReceiptDataDto(
              id = receipt.id,
              cancelSerialNumber = nextSerialNumber,
              cancelYearCode = store.yearCode ?: 0,
            )

          when (val result = receiptsRepository.cancelReceipt(request)) {
            is ApiResource.Success -> {
              receiptsStore.addReceipt(result.data)
              restoreCanceledQuantities(receipt)
              _uiState.update { it.copy(hasCanceledNow = true) }
            }
            is ApiResource.Error -> {
              val user = userRepository.getCurrentUser().first().getOrNull()
              val message =
                if (user?.isDev == true) {
                  result.message
                } else {
                  "Váratlan hiba lépett fel a számla sztornózása során."
                }
              _uiState.update { it.copy(errorMessage = message) }
            }
            is ApiResource.Loading -> {}
          }
        } finally {
          _uiState.update { it.copy(isCancelInProgress = false) }
        }
      }
    }

    private fun restoreCanceledQuantities(receipt: Receipt) {
      val store = storeSessionStore.selectedStoreCurrentState.value ?: return

      val canceledQuantities =
        receipt.items
          .groupBy { it.articleNumber to it.expiresAt }
          .mapValues { (_, items) -> items.sumOf { it.quantity } }

      canceledQuantities.forEach { (key, quantity) ->
        val (articleNumber, expiresAt) = key
        val expiration =
          store.expirations.find { it.articleNumber == articleNumber && it.expiresAt == expiresAt }
        if (expiration != null) {
          storeSessionStore.updateExpirationQuantity(expiration.itemId, expiration.expirationId, quantity)
        }
      }
    }
  }
