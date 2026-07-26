package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NO_ARG = -1

data class SummaryUiState(
  val receipt: Receipt? = null,
  val isInternetReachable: Boolean = true,
  val isLoading: Boolean = false,
)

@HiltViewModel
class SummaryViewModel
  @Inject
  constructor(
    savedStateHandle: SavedStateHandle,
    private val receiptsStore: ReceiptsStore,
    connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val serialNumber: Int = savedStateHandle.get<Int>("serialNumber") ?: NO_ARG

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private var hasLeft = false

    init {
      viewModelScope.launch {
        combine(
          receiptsStore.receiptBySerialNumber(serialNumber),
          connectivityObserver.isInternetReachable,
        ) { receipt, isOnline -> receipt to isOnline }
          .collect { (receipt, isOnline) ->
            _uiState.update { it.copy(receipt = receipt, isInternetReachable = isOnline) }
          }
      }
    }

    fun onLeave(onNavigateHome: () -> Unit) {
      if (hasLeft) {
        return
      }
      hasLeft = true

      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        receiptsStore.resetSellFlowDraft()
        onNavigateHome()
      }
    }
  }
