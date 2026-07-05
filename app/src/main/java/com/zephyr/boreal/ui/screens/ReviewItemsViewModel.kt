package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.mapper.toDto
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ReceiptsRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.utils.AmountRounding
import com.zephyr.boreal.domain.utils.ReceiptDates
import com.zephyr.boreal.domain.utils.buildCreateReceiptRequest
import com.zephyr.boreal.domain.utils.calculateReceiptTotals
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.math.round

private const val ROUNDING_STEP = 5.0
private const val RECEIPT_DATE_FORMAT_PATTERN = "yyyy-MM-dd"
private val RECEIPT_DATE_FORMAT = DateTimeFormatter.ofPattern(RECEIPT_DATE_FORMAT_PATTERN)

data class ReviewItemsUiState(
  val items: List<DraftReceiptItem> = emptyList(),
  val otherItems: List<ReceiptOtherItem> = emptyList(),
  val grossTotal: Double = 0.0,
  val expandedItemKeys: Set<String> = emptySet(),
  val expandedOtherItemIds: Set<Int> = emptySet(),
  val showCancelConfirmation: Boolean = false,
  val showFinalizeConfirmation: Boolean = false,
  val isLoading: Boolean = false,
  val isInternetReachable: Boolean = true,
  val canFinalize: Boolean = false,
  val isSent: Boolean = false,
  val isSentSuccessfully: Boolean = false,
  val isSentWithErrors: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
)

@HiltViewModel
class ReviewItemsViewModel
  @Inject
  constructor(
    private val receiptsStore: ReceiptsStore,
    private val receiptsRepository: ReceiptsRepository,
    private val userRepository: UserRepository,
    private val storeSessionStore: StoreSessionStore,
    connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewItemsUiState())
    val uiState: StateFlow<ReviewItemsUiState> = _uiState.asStateFlow()

    private var isOrderSubmitted = false

    init {
      viewModelScope.launch {
        receiptsStore.currentReceipt.collect { draft ->
          val items = draft?.items ?: emptyList()
          val otherItems = draft?.otherItems ?: emptyList()
          _uiState.update {
            it.copy(
              items = items,
              otherItems = otherItems,
              grossTotal = items.sumOf { item -> item.grossAmount } + otherItems.sumOf { item -> item.grossAmount },
            )
          }
        }
      }

      viewModelScope.launch {
        combine(
          receiptsStore.currentReceipt,
          userRepository.getCurrentUser(),
          storeSessionStore.selectedStoreCurrentState,
          connectivityObserver.isInternetReachable,
        ) { draft, userResource, store, isOnline ->
          val user = userResource.getOrNull()
          val hasItems = !(draft?.items.isNullOrEmpty() && draft?.otherItems.isNullOrEmpty())
          isOnline to (isOnline && hasItems && user?.lastRound != null && store != null)
        }.collect { (isOnline, canFinalize) ->
          _uiState.update { it.copy(isInternetReachable = isOnline, canFinalize = canFinalize) }
        }
      }
    }

    fun onToggleExpanded(key: String) {
      _uiState.update { state ->
        val newKeys =
          if (state.expandedItemKeys.contains(key)) {
            state.expandedItemKeys - key
          } else {
            state.expandedItemKeys + key
          }
        state.copy(expandedItemKeys = newKeys)
      }
    }

    fun onToggleOtherItemExpanded(id: Int) {
      _uiState.update { state ->
        val newIds =
          if (state.expandedOtherItemIds.contains(id)) {
            state.expandedOtherItemIds - id
          } else {
            state.expandedOtherItemIds + id
          }
        state.copy(expandedOtherItemIds = newIds)
      }
    }

    fun removeItem(
      id: Int,
      expirationId: Int,
      onNavigateHome: () -> Unit,
    ) {
      receiptsStore.upsertSelectedItem(id, expirationId, null)
      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(
          items = draft.items.filter { !(it.id == id && it.expirationId == expirationId) },
        )
      }
      if (receiptsStore.currentReceipt.value
          ?.items
          .isNullOrEmpty()
      ) {
        receiptsStore.resetReceipts()
        onNavigateHome()
      }
    }

    fun removeOtherItem(id: Int) {
      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(otherItems = draft.otherItems.filter { it.id != id })
      }
    }

    fun showCancelDialog() {
      _uiState.update { it.copy(showCancelConfirmation = true) }
    }

    fun dismissCancelDialog() {
      _uiState.update { it.copy(showCancelConfirmation = false) }
    }

    fun cancelReceipt(onNavigateHome: () -> Unit) {
      receiptsStore.resetReceipts()
      onNavigateHome()
    }

    fun showFinalizeDialog() {
      _uiState.update { it.copy(showFinalizeConfirmation = true) }
    }

    fun dismissFinalizeDialog() {
      _uiState.update { it.copy(showFinalizeConfirmation = false) }
    }

    fun confirmFinalize() {
      _uiState.update { it.copy(showFinalizeConfirmation = false) }
      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        submitReceipt()
        _uiState.update { it.copy(isLoading = false) }
      }
    }

    fun retryReceipt() {
      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        submitReceipt()
        _uiState.update { it.copy(isLoading = false) }
      }
    }

    private suspend fun submitReceipt() {
      if (_uiState.value.isSentSuccessfully) {
        return
      }

      val draft = receiptsStore.currentReceipt.value
      val user = userRepository.getCurrentUser().first().getOrNull()
      val store = storeSessionStore.selectedStoreCurrentState.value

      val missingComponents =
        buildList {
          if (draft == null) add("aktuális számla")
          if (user?.lastRound == null) add("aktuális kör")
          if (store == null) add("kör raktár")
        }

      if (missingComponents.isNotEmpty()) {
        _uiState.update {
          it.copy(
            errorMessage = "Hiányzó adatok: ${missingComponents.joinToString(", ")}",
            isSentWithErrors = true,
            isSent = true,
          )
        }
        return
      }

      try {
        submitValidatedReceipt(draft = requireNotNull(draft), user = requireNotNull(user))
      } catch (e: IllegalArgumentException) {
        handleSubmitError(user = requireNotNull(user), message = e.message)
      }
    }

    private suspend fun submitValidatedReceipt(
      draft: DraftReceipt,
      user: User,
    ) {
      if (!submitOrderIfNeeded(user)) {
        return
      }

      val store = requireNotNull(storeSessionStore.selectedStoreCurrentState.value)
      val round = requireNotNull(user.lastRound)

      val totals = calculateReceiptTotals(draft.items, draft.otherItems)

      val invoiceDate = parseRoundStartedDate(round.roundStarted)
      val fulfillmentDate = invoiceDate.plusDays((draft.paymentDays ?: 0).toLong())

      val roundedAmount =
        if (draft.invoiceType == InvoiceType.PAPER) {
          round(totals.grossAmount / ROUNDING_STEP) * ROUNDING_STEP
        } else {
          totals.grossAmount
        }
      val roundAmount = roundedAmount - totals.grossAmount

      val serialNumber = nextAvailableSerialNumber(receiptsStore.receipts.value, store.firstAvailableSerialNumber ?: 0)

      val request =
        buildCreateReceiptRequest(
          draft = draft,
          company = user.company,
          store = store,
          serialNumber = serialNumber,
          totals = totals,
          dates =
            ReceiptDates(
              invoiceDate = invoiceDate.format(RECEIPT_DATE_FORMAT),
              fulfillmentDate = fulfillmentDate.format(RECEIPT_DATE_FORMAT),
              paidDate = fulfillmentDate.format(RECEIPT_DATE_FORMAT),
            ),
          rounding = AmountRounding(roundAmount = roundAmount, roundedAmount = roundedAmount),
        )

      when (val result = receiptsRepository.createReceipt(request)) {
        is ApiResource.Success -> {
          receiptsStore.addReceipt(result.data)
          storeSessionStore.sellItems(
            draft.items
              .groupBy { it.id }
              .mapValues { (_, items) -> items.associate { it.expirationId to it.quantity } },
          )
          _uiState.update {
            it.copy(
              errorMessage = null,
              isSentWithErrors = false,
              successMessage = "Számla sikeresen beküldve.",
              isSentSuccessfully = true,
              isSent = true,
            )
          }
        }
        is ApiResource.Error -> handleSubmitError(user, result.message)
        is ApiResource.Loading -> {}
      }
    }

    /**
     * Submits the order built when items were confirmed on the select-items screen, if any.
     * Guarded by [isOrderSubmitted] so a retry after a receipt-only failure doesn't resubmit an
     * order that already succeeded (mirrors the RN app's saveOrder/saveReceipt/saveStorage steps).
     * @return true if the order step succeeded (or there was nothing to submit), false on error.
     */
    private suspend fun submitOrderIfNeeded(user: User): Boolean {
      val order = receiptsStore.currentOrder.value

      return when {
        isOrderSubmitted -> true
        order == null || order.items.isEmpty() -> {
          isOrderSubmitted = true
          true
        }
        else ->
          when (val result = receiptsRepository.createOrders(listOf(order.toDto()))) {
            is ApiResource.Success -> {
              isOrderSubmitted = true
              true
            }
            is ApiResource.Error -> {
              handleSubmitError(user, result.message)
              false
            }
            is ApiResource.Loading -> false
          }
      }
    }

    private fun handleSubmitError(
      user: User,
      message: String?,
    ) {
      val errorMessage = if (user.isDev) message ?: "Számla beküldése sikertelen." else "Számla beküldése sikertelen."
      _uiState.update {
        it.copy(
          errorMessage = errorMessage,
          isSentWithErrors = true,
          isSent = true,
        )
      }
    }

    private fun nextAvailableSerialNumber(
      receipts: List<Receipt>,
      firstAvailableSerialNumber: Int,
    ): Int {
      if (receipts.isEmpty()) {
        return firstAvailableSerialNumber
      }
      return receipts.maxOf { maxOf(it.serialNumber, it.cancelSerialNumber ?: 0) } + 1
    }

    /**
     * The backend sends [Round.roundStarted] as a plain "yyyy-MM-dd" date (see RoundResource),
     * but some responses/tests use a full ISO-8601 instant. Try the instant format first,
     * falling back to a plain local date (same dual-format handling as [MainViewModel]).
     */
    private fun parseRoundStartedDate(roundStarted: String): LocalDate =
      try {
        Instant.parse(roundStarted).atZone(ZoneOffset.UTC).toLocalDate()
      } catch (e: DateTimeParseException) {
        LocalDate.parse(roundStarted)
      }
  }
