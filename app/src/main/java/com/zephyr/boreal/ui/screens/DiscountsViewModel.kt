package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.zephyr.boreal.domain.model.Discount
import com.zephyr.boreal.domain.model.DiscountType
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.SelectedDiscount
import com.zephyr.boreal.domain.utils.DiscountCalculator
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val NO_ARG = -1

data class DiscountsUiState(
  val isLoading: Boolean = true,
  val name: String = "",
  val expiresAt: String = "",
  val quantity: Double = 0.0,
  val unitName: String = "",
  val absoluteDiscount: Discount? = null,
  val percentageDiscount: Discount? = null,
  val freeFormDiscount: Discount? = null,
  val absoluteQuantityText: String = "",
  val percentageQuantityText: String = "",
  val freeFormQuantityText: String = "",
  val freeFormPriceText: String = "",
  val absoluteQuantityError: Boolean = false,
  val percentageQuantityError: Boolean = false,
  val freeFormQuantityError: Boolean = false,
  val freeFormPriceError: Boolean = false,
  val formErrorMessage: String? = null,
)

private data class ParsedDiscountInputs(
  val absoluteQuantity: Double,
  val percentageQuantity: Double,
  val freeFormQuantity: Double,
  val freeFormPrice: Double,
)

private data class DiscountValidationErrors(
  val message: String,
  val absoluteQuantityError: Boolean,
  val percentageQuantityError: Boolean,
  val freeFormQuantityError: Boolean,
  val freeFormPriceError: Boolean,
)

@HiltViewModel
class DiscountsViewModel
  @Inject
  constructor(
    savedStateHandle: SavedStateHandle,
    private val receiptsStore: ReceiptsStore,
  ) : ViewModel() {
    private val itemId: Int = savedStateHandle.get<Int>("itemId") ?: NO_ARG
    private val expirationId: Int = savedStateHandle.get<Int>("expirationId") ?: NO_ARG

    private val _uiState = MutableStateFlow(DiscountsUiState())
    val uiState: StateFlow<DiscountsUiState> = _uiState.asStateFlow()

    init {
      val item =
        receiptsStore.currentReceipt.value
          ?.items
          ?.find { it.id == itemId && it.expirationId == expirationId }

      if (item == null) {
        _uiState.update { it.copy(isLoading = false) }
      } else {
        _uiState.update { prefillFrom(item) }
      }
    }

    private fun prefillFrom(item: DraftReceiptItem): DiscountsUiState {
      val selectedAbsolute = item.selectedDiscounts.find { it.type == DiscountType.ABSOLUTE }
      val selectedPercentage = item.selectedDiscounts.find { it.type == DiscountType.PERCENTAGE }
      val selectedFreeForm = item.selectedDiscounts.find { it.type == DiscountType.FREE_FORM }

      return DiscountsUiState(
        isLoading = false,
        name = item.name,
        expiresAt = item.expiresAt,
        quantity = item.quantity,
        unitName = item.unitName,
        absoluteDiscount = item.availableDiscounts.find { it.type == DiscountType.ABSOLUTE },
        percentageDiscount = item.availableDiscounts.find { it.type == DiscountType.PERCENTAGE },
        freeFormDiscount = item.availableDiscounts.find { it.type == DiscountType.FREE_FORM },
        absoluteQuantityText = selectedAbsolute?.quantity?.let(::formatNumber) ?: "",
        percentageQuantityText = selectedPercentage?.quantity?.let(::formatNumber) ?: "",
        freeFormQuantityText = selectedFreeForm?.quantity?.let(::formatNumber) ?: "",
        freeFormPriceText = formatNumber(selectedFreeForm?.price ?: item.netPrice),
      )
    }

    fun onAbsoluteQuantityChanged(text: String) {
      _uiState.update { it.copy(absoluteQuantityText = text) }
    }

    fun onPercentageQuantityChanged(text: String) {
      _uiState.update { it.copy(percentageQuantityText = text) }
    }

    fun onFreeFormQuantityChanged(text: String) {
      _uiState.update { it.copy(freeFormQuantityText = text) }
    }

    fun onFreeFormPriceChanged(text: String) {
      _uiState.update { it.copy(freeFormPriceText = text) }
    }

    fun applyDiscounts(onSuccess: () -> Unit) {
      val state = _uiState.value
      val inputs = parseInputs(state)
      if (inputs == null) {
        _uiState.update { it.copy(formErrorMessage = NUMERIC_ERROR) }
        return
      }

      val validationErrors = validate(state, inputs)
      if (validationErrors != null) {
        _uiState.update {
          it.copy(
            formErrorMessage = validationErrors.message,
            absoluteQuantityError = validationErrors.absoluteQuantityError,
            percentageQuantityError = validationErrors.percentageQuantityError,
            freeFormQuantityError = validationErrors.freeFormQuantityError,
            freeFormPriceError = validationErrors.freeFormPriceError,
          )
        }
        return
      }

      _uiState.update {
        it.copy(
          formErrorMessage = null,
          absoluteQuantityError = false,
          percentageQuantityError = false,
          freeFormQuantityError = false,
          freeFormPriceError = false,
        )
      }

      writeSelectedDiscounts(buildSelectedDiscounts(state, inputs))
      onSuccess()
    }

    @Suppress("ReturnCount")
    private fun parseInputs(state: DiscountsUiState): ParsedDiscountInputs? {
      val absoluteQuantity = state.absoluteQuantityText.toAmountOrNull() ?: return null
      val percentageQuantity = state.percentageQuantityText.toAmountOrNull() ?: return null
      val freeFormQuantity = state.freeFormQuantityText.toAmountOrNull() ?: return null
      val freeFormPrice = state.freeFormPriceText.toAmountOrNull() ?: return null
      return ParsedDiscountInputs(absoluteQuantity, percentageQuantity, freeFormQuantity, freeFormPrice)
    }

    private fun String.toAmountOrNull(): Double? = ifBlank { "0" }.toDoubleOrNull()

    private fun validate(
      state: DiscountsUiState,
      inputs: ParsedDiscountInputs,
    ): DiscountValidationErrors? {
      val messages = mutableListOf<String>()
      var quantityError = false
      var priceError = false

      val totalDiscountedQuantity = inputs.absoluteQuantity + inputs.percentageQuantity + inputs.freeFormQuantity
      if (totalDiscountedQuantity > state.quantity) {
        messages += QUANTITY_TOO_LARGE_ERROR
        quantityError = true
      }

      if (inputs.freeFormQuantity > 0 && inputs.freeFormPrice < 0) {
        messages += FREE_FORM_PRICE_ERROR
        priceError = true
      }

      if (messages.isEmpty()) {
        return null
      }

      return DiscountValidationErrors(
        message = messages.joinToString(" "),
        absoluteQuantityError = quantityError,
        percentageQuantityError = quantityError,
        freeFormQuantityError = quantityError,
        freeFormPriceError = priceError,
      )
    }

    private fun buildSelectedDiscounts(
      state: DiscountsUiState,
      inputs: ParsedDiscountInputs,
    ): List<SelectedDiscount> =
      buildList {
        state.absoluteDiscount?.let { discount ->
          if (inputs.absoluteQuantity > 0) {
            add(
              SelectedDiscount(
                discount.id,
                discount.name,
                DiscountType.ABSOLUTE,
                inputs.absoluteQuantity,
                amount = discount.amount,
              ),
            )
          }
        }
        state.percentageDiscount?.let { discount ->
          if (inputs.percentageQuantity > 0) {
            add(
              SelectedDiscount(
                discount.id,
                discount.name,
                DiscountType.PERCENTAGE,
                inputs.percentageQuantity,
                amount = discount.amount,
              ),
            )
          }
        }
        state.freeFormDiscount?.let { discount ->
          if (inputs.freeFormQuantity > 0) {
            add(
              SelectedDiscount(
                discount.id,
                discount.name,
                DiscountType.FREE_FORM,
                inputs.freeFormQuantity,
                price = inputs.freeFormPrice,
              ),
            )
          }
        }
      }

    private fun writeSelectedDiscounts(selectedDiscounts: List<SelectedDiscount>) {
      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(items = draft.items.map { item -> applyDiscountsToItem(item, selectedDiscounts) })
      }
    }

    private fun applyDiscountsToItem(
      item: DraftReceiptItem,
      selectedDiscounts: List<SelectedDiscount>,
    ): DraftReceiptItem {
      if (item.id != itemId || item.expirationId != expirationId) {
        return item
      }
      val amounts =
        DiscountCalculator.calculateDiscountedLineAmounts(
          originalNetPrice = item.netPrice,
          quantity = item.quantity,
          vatRate = item.vatRate,
          selectedDiscounts = selectedDiscounts,
        )
      return item.copy(
        selectedDiscounts = selectedDiscounts,
        netAmount = amounts.netAmount,
        vatAmount = amounts.vatAmount,
        grossAmount = amounts.grossAmount,
      )
    }

    private companion object {
      const val NUMERIC_ERROR = "Csak számok adhatóak meg."
      const val QUANTITY_TOO_LARGE_ERROR = "Túl nagy megadott mennyiség."
      const val FREE_FORM_PRICE_ERROR = "Az új árat pozitív számként lehetséges megadni."

      fun formatNumber(value: Double): String = if (value % 1 == 0.0) value.toInt().toString() else value.toString()
    }
  }
