package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.utils.DevPrintFileNameBuilder
import com.zephyr.boreal.domain.utils.PrintReceiptTextBuilder
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrintSectionUiState(
  val isLoading: Boolean = true,
  val receiptFormat: String = "",
  val copyCount: Int = 0,
  val receiptToPrint: String? = null,
  val suggestedFileName: String = "",
)

/**
 * Backs the reusable [com.zephyr.boreal.ui.components.PrintSection]. Not nav-scoped (it's an
 * embedded slot on Summary, and later reused for reprints/storno), so it takes its
 * `isCancel`/`receiptSerialNumber` params via [initialize] rather than `SavedStateHandle`.
 */
@HiltViewModel
class PrintSectionViewModel
  @Inject
  constructor(
    private val receiptsStore: ReceiptsStore,
    private val partnersRepository: PartnersRepository,
    private val userRepository: UserRepository,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(PrintSectionUiState())
    val uiState: StateFlow<PrintSectionUiState> = _uiState.asStateFlow()

    private var initialized = false
    private var currentReceipt: Receipt? = null
    private var hasMarkedPrinted = false

    fun initialize(
      isCancel: Boolean,
      receiptSerialNumber: Int,
    ) {
      if (initialized) {
        return
      }
      initialized = true

      viewModelScope.launch {
        val receipt = receiptsStore.receipts.value.find { it.serialNumber == receiptSerialNumber }
        val user = userRepository.getCurrentUser().first().getOrNull()
        val partners =
          partnersRepository
            .getPartners()
            .first()
            .getOrNull()
            .orEmpty()
        val partner = partners.find { it.id == receipt?.buyer?.id }

        currentReceipt = receipt

        val canPrint = user != null && receipt != null && (partner != null || receipt.partnerCode.isEmpty())

        _uiState.update {
          it.copy(
            isLoading = false,
            receiptFormat = PrintReceiptTextBuilder.receiptFormatLabel(partner),
            copyCount = receipt?.let { r -> PrintReceiptTextBuilder.copyCount(partner, r) } ?: 0,
            receiptToPrint =
              if (canPrint) {
                PrintReceiptTextBuilder.build(isCancel, requireNotNull(user), requireNotNull(receipt), partner)
              } else {
                null
              },
            suggestedFileName = receipt?.let { r -> DevPrintFileNameBuilder.build(r) } ?: "",
          )
        }
      }
    }

    fun onPrintSuccess() {
      val receipt = currentReceipt ?: return
      if (receipt.isPrinted || hasMarkedPrinted) {
        return
      }
      hasMarkedPrinted = true
      viewModelScope.launch {
        receiptsStore.markReceiptPrinted(receipt.id)
      }
    }
  }
