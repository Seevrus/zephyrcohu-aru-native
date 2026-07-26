package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.di.DefaultDispatcher
import com.zephyr.boreal.domain.utils.DevPrintFileNameBuilder
import com.zephyr.boreal.domain.utils.PrintReceiptTextBuilder
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PrintSectionUiState(
  val isLoading: Boolean = true,
  val receiptFormat: String = "",
  val copyCount: Int = 0,
  val receiptToPrint: String? = null,
  val suggestedFileName: String = "",
  val receiptId: Int? = null,
  val isPrinted: Boolean = false,
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
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(PrintSectionUiState())
    val uiState: StateFlow<PrintSectionUiState> = _uiState.asStateFlow()

    private var initializedFor: Pair<Boolean, Int>? = null

    fun initialize(
      isCancel: Boolean,
      receiptSerialNumber: Int,
    ) {
      val params = isCancel to receiptSerialNumber
      if (initializedFor == params) {
        return
      }
      initializedFor = params

      viewModelScope.launch {
        val receipt = receiptsStore.receiptBySerialNumber(receiptSerialNumber).first()
        val user = userRepository.getCurrentUser().first().getOrNull()
        val partners =
          partnersRepository
            .getPartners()
            .first()
            .getOrNull()
            .orEmpty()
        val partner = partners.find { it.id == receipt?.buyer?.id }

        val receiptToPrint =
          if (user == null || receipt == null) {
            null
          } else if (partner == null && receipt.partnerCode.isNotEmpty()) {
            null
          } else {
            withContext(defaultDispatcher) {
              PrintReceiptTextBuilder.build(isCancel, user, receipt, partner)
            }
          }

        _uiState.update {
          it.copy(
            isLoading = false,
            receiptFormat = PrintReceiptTextBuilder.receiptFormatLabel(partner),
            copyCount = receipt?.let { r -> PrintReceiptTextBuilder.copyCount(partner, r) } ?: 0,
            receiptToPrint = receiptToPrint,
            suggestedFileName = receipt?.let { r -> DevPrintFileNameBuilder.build(r) } ?: "",
            receiptId = receipt?.id,
            isPrinted = receipt?.isPrinted ?: false,
          )
        }
      }
    }

    fun onPrintSuccess() {
      val state = _uiState.value
      val receiptId = state.receiptId ?: return
      if (state.isPrinted) {
        return
      }
      _uiState.update { it.copy(isPrinted = true) }
      viewModelScope.launch {
        receiptsStore.markReceiptPrinted(receiptId)
      }
    }
  }
