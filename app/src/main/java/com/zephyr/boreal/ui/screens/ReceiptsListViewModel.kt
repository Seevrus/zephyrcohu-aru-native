package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReceiptRowUiModel(
  val serialNumber: Int,
  val yearCode: Int,
  val deliveryName: String,
  val grossAmount: Double,
  val cancelSerialNumber: Int?,
  val cancelYearCode: Int?,
) {
  val isCanceled: Boolean
    get() = cancelSerialNumber != null && cancelYearCode != null
}

data class ReceiptsListUiState(
  val receipts: List<ReceiptRowUiModel> = emptyList(),
)

private fun Receipt.toRowUiModel() =
  ReceiptRowUiModel(
    serialNumber = serialNumber,
    yearCode = yearCode,
    deliveryName = buyer.deliveryName,
    grossAmount = grossAmount,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
  )

@HiltViewModel
class ReceiptsListViewModel
  @Inject
  constructor(
    receiptsStore: ReceiptsStore,
    userRepository: UserRepository,
  ) : ViewModel() {
    val uiState: StateFlow<ReceiptsListUiState> =
      combine(
        receiptsStore.receipts,
        userRepository.getCurrentUser(),
      ) { receipts, userResource ->
        val isRoundInProgress = userResource.getOrNull()?.state == UserState.ON_ROUND
        val rows = if (isRoundInProgress) receipts.map { it.toRowUiModel() } else emptyList()
        ReceiptsListUiState(rows)
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReceiptsListUiState(),
      )
  }
